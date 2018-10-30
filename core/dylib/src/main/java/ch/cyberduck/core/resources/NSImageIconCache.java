package ch.cyberduck.core.resources;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to feedback@cyberduck.ch
 */

import ch.cyberduck.binding.application.NSGraphics;
import ch.cyberduck.binding.application.NSImage;
import ch.cyberduck.binding.application.NSWorkspace;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.local.Application;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.cocoa.foundation.NSPoint;
import org.rococoa.cocoa.foundation.NSRect;
import org.rococoa.cocoa.foundation.NSSize;

import java.util.HashMap;
import java.util.Map;

public class NSImageIconCache extends AbstractIconCache<NSImage> {
    private static final Logger log = Logger.getLogger(NSImageIconCache.class);

    private final static NSRect NSZeroRect = new NSRect(0, 0);

    /**
     * Cache limited to n entries
     */
    private final Map<String, NSImage> cache;

    public NSImageIconCache() {
        if(0 == PreferencesFactory.get().getInteger("icon.cache.size")) {
            cache = new HashMap<String, NSImage>() {
                @Override
                public NSImage put(String key, NSImage value) {
                    return value;
                }
            };
        }
        else {
            cache = new LRUMap<String, NSImage>(PreferencesFactory.get().getInteger("icon.cache.size")) {
                @Override
                protected boolean removeLRU(LinkEntry entry) {
                    if(log.isDebugEnabled()) {
                        log.debug("Removing from cache:" + entry);
                    }
                    return true;
                }
            };
        }
    }

    private NSImage put(final String name, final NSImage image, final Integer size) {
        cache.put(String.format("%d-%s", size, name), image);
        return image;
    }

    private NSImage load(final String name, final Integer size) {
        if(!cache.containsKey(String.format("%d-%s", size, name))) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("No cached image for %s", name));
            }
            return null;
        }
        return cache.get(String.format("%d-%s", size, name));
    }

    /**
     * @param extension File type
     * @param size      Requested size
     * @return Cached icon
     */
    @Override
    public NSImage documentIcon(final String extension, final Integer size) {
        NSImage image = this.load(extension, size);
        if(null == image) {
            return this.put(extension,
                this.convert(extension, NSWorkspace.sharedWorkspace().iconForFileType(extension), size), size);
        }
        return image;
    }

    @Override
    public NSImage documentIcon(final String extension, final Integer size, final NSImage badge) {
        final String name = extension + badge.name();
        NSImage icon = this.iconNamed(name, size);
        if(null == icon) {
            icon = this.badge(badge, this.documentIcon(extension, size));
            this.put(name, icon, size);
        }
        return icon;
    }

    @Override
    public NSImage folderIcon(final Integer size) {
        NSImage folder = this.iconNamed("NSFolder", size);
        if(null == folder) {
            return this.iconNamed("NSFolder", size);
        }
        return folder;
    }

    @Override
    public NSImage folderIcon(final Integer size, final NSImage badge) {
        final String name = String.format("NSFolder-%s", badge.name());
        NSImage folder = this.load(name, size);
        if(null == folder) {
            folder = this.convert(name, this.iconNamed("NSFolder", size), size);
            folder = this.badge(badge, folder);
            this.put(name, folder, size);
        }
        return folder;
    }

    /**
     * Overlay badge image.
     *
     * @param badge Overlay
     * @param icon  Icon
     * @return Cached icon
     */
    @Override
    protected NSImage badge(final NSImage badge, final NSImage icon) {
        NSImage f = NSImage.imageWithSize(icon.size());
        f.lockFocus();
        icon.drawInRect(new NSRect(new NSPoint(0, 0), icon.size()),
            NSZeroRect, NSGraphics.NSCompositeSourceOver, 1.0f);
        badge.drawInRect(new NSRect(new NSPoint(0, 0), badge.size()),
            NSZeroRect, NSGraphics.NSCompositeSourceOver, 1.0f);
        f.unlockFocus();
        return f;
    }

    public NSImage iconNamed(final String image, final Integer size, final NSImage badge) {
        final String name = String.format("%s-%s", image, badge.name());
        NSImage icon = this.load(name, size);
        if(null == icon) {
            icon = this.convert(name, this.iconNamed(image, size), size);
            icon = this.badge(badge, icon);
            this.put(name, icon, size);
        }
        return icon;
    }

    /**
     * @param name   When looking for files in the application bundle, it is better (but not required)
     *               to include the filename extension in the name parameter
     * @param width  Requested size
     * @param height Requested size
     * @return Cached icon
     * @see NSImage#imageNamed(String)
     * @see #convert(String, ch.cyberduck.binding.application.NSImage, Integer, Integer)
     */
    @Override
    public NSImage iconNamed(final String name, final Integer width, final Integer height) {
        NSImage image = this.load(name, width);
        if(null == image) {
            if(null == name) {
                return iconNamed("notfound.tiff", width, height);
            }
            else if(name.startsWith("/")) {
                image = NSImage.imageWithContentsOfFile(name);
            }
            else {
                image = NSImage.imageNamed(String.format("%d-%s", width, name));
                if(null == image) {
                    image = NSImage.imageNamed(name);
                }
                else {
                    return image;
                }
            }
            if(null == image) {
                log.warn(String.format("No icon named %s", name));
                this.put(name, null, width);
            }
            else {
                return this.put(name, this.convert(name, image, width, height), width);
            }
        }
        return image;
    }

    /**
     * @param file File
     * @param size Requested size
     * @return Cached icon
     */
    @Override
    public NSImage fileIcon(final Local file, final Integer size) {
        NSImage icon = null;
        if(file.exists()) {
            icon = this.load(file.getAbsolute(), size);
            if(null == icon) {
                return this.put(file.getAbsolute(),
                    this.convert(file.getName(), NSWorkspace.sharedWorkspace().iconForFile(file.getAbsolute()), size), size);
            }
        }
        if(null == icon) {
            return this.iconNamed("notfound.tiff", size);
        }
        return icon;
    }

    /**
     * @param app  Application
     * @param size Requested size
     * @return Cached icon
     */
    @Override
    public NSImage applicationIcon(final Application app, final Integer size) {
        NSImage icon = this.load(app.getIdentifier(), size);
        if(null == icon) {
            final String path = NSWorkspace.sharedWorkspace().absolutePathForAppBundleWithIdentifier(app.getIdentifier());
            // Null if the bundle cannot be found
            if(StringUtils.isNotBlank(path)) {
                return this.put(app.getIdentifier(),
                    this.convert(app.getIdentifier(), NSWorkspace.sharedWorkspace().iconForFile(path), size), size);
            }
        }
        if(null == icon) {
            return this.iconNamed("notfound.tiff", size);
        }
        return icon;
    }

    /**
     * @param file File
     * @param size Requested size
     * @return Cached icon
     */
    @Override
    public NSImage fileIcon(final Path file, final Integer size) {
        if(file.getType().contains(Path.Type.decrypted)) {
            final NSImage badge = this.iconNamed("unlockedbadge.tiff", size);
            badge.setName("unlockedbadge");
            if(file.isDirectory()) {
                return this.folderIcon(size, badge);
            }
            return this.documentIcon(StringUtils.lowerCase(file.getExtension()), size, badge);
        }
        if(file.isSymbolicLink()) {
            final NSImage badge = this.iconNamed("aliasbadge.tiff", size);
            badge.setName("aliasbadge");
            if(file.isDirectory()) {
                return this.folderIcon(size, badge);
            }
            return this.documentIcon(StringUtils.lowerCase(file.getExtension()), size, badge);
        }
        if(file.isFile()) {
            if(StringUtils.isEmpty(file.getExtension())) {
                if(file.attributes().getPermission().isExecutable()) {
                    return this.iconNamed("executable.tiff", size);
                }
            }
            return this.documentIcon(StringUtils.lowerCase(file.getExtension()), size);
        }
        if(file.isDirectory()) {
            if(Permission.EMPTY != file.attributes().getPermission()) {
                if(!file.attributes().getPermission().isExecutable()) {
                    final NSImage badge = this.iconNamed("privatefolderbadge.tiff", size);
                    badge.setName("privatefolderbadge");
                    return this.folderIcon(size, badge);
                }
                if(!file.attributes().getPermission().isReadable()) {
                    if(file.attributes().getPermission().isWritable()) {
                        final NSImage badge = this.iconNamed("dropfolderbadge.tiff", size);
                        badge.setName("dropfolderbadge");
                        return this.folderIcon(size, badge);
                    }
                }
                if(!file.attributes().getPermission().isWritable()) {
                    final NSImage badge = this.iconNamed("readonlyfolderbadge.tiff", size);
                    badge.setName("readonlyfolderbadge");
                    return this.folderIcon(size, badge);
                }
            }
            return this.folderIcon(size);
        }
        return this.iconNamed("notfound.tiff", size);
    }

    private NSImage convert(final String name, final NSImage icon, final Integer size) {
        return this.convert(name, icon, size, size);
    }

    private NSImage convert(final String name, final NSImage icon, final Integer width, final Integer height) {
        if(StringUtils.endsWith(name, "pdf")) {
            icon.setTemplate(true);
        }
        if(null == width || null == height) {
            log.debug(String.format("Return default size for %s", icon.name()));
            return icon;
        }
        // Cache sized image
        icon.setName(String.format("%d-%s", width, name));
        icon.setSize(new NSSize(width, height));
        return icon;
    }
}
