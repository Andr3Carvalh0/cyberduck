package ch.cyberduck.ui.cocoa.delegate;

/*
 *  Copyright (c) 2006 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.binding.Action;
import ch.cyberduck.binding.BundleController;
import ch.cyberduck.binding.Delegate;
import ch.cyberduck.binding.application.NSImage;
import ch.cyberduck.binding.application.NSMenu;
import ch.cyberduck.binding.application.NSMenuItem;
import ch.cyberduck.binding.foundation.NSAttributedString;
import ch.cyberduck.binding.foundation.NSMutableAttributedString;
import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.BookmarkNameProvider;
import ch.cyberduck.core.FolderBookmarkCollection;
import ch.cyberduck.core.HistoryCollection;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.local.ApplicationLauncherFactory;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.resources.IconCacheFactory;
import ch.cyberduck.ui.cocoa.controller.MainController;
import ch.cyberduck.ui.cocoa.view.BookmarkCell;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.Selector;
import org.rococoa.cocoa.foundation.NSInteger;

public class BookmarkMenuDelegate extends CollectionMenuDelegate<Host> {
    private static final Logger log = Logger.getLogger(BookmarkMenuDelegate.class);

    private static final int BOOKMARKS_INDEX = 8;

    private final Preferences preferences
        = PreferencesFactory.get();

    private final AbstractHostCollection collection;

    private final int index;

    private final MenuCallback callback;

    private final NSMenu historyMenu = NSMenu.menu();
    @Delegate
    private final HistoryMenuDelegate historyMenuDelegate;

    private final NSMenu rendezvousMenu = NSMenu.menu();
    @Delegate
    private final RendezvousMenuDelegate rendezvousMenuDelegate;

    public BookmarkMenuDelegate() {
        this(new HistoryMenuDelegate(), new RendezvousMenuDelegate());
    }

    public BookmarkMenuDelegate(final HistoryMenuDelegate history, final RendezvousMenuDelegate rendezvous) {
        this(new MenuCallback() {
            @Override
            public void selected(final NSMenuItem sender) {
                MainController.newDocument().mount(FolderBookmarkCollection.favoritesCollection().lookup(sender.representedObject()));
            }
        }, history, rendezvous);
    }

    public BookmarkMenuDelegate(final MenuCallback callback) {
        this(callback, new HistoryMenuDelegate(), new RendezvousMenuDelegate());
    }

    public BookmarkMenuDelegate(final MenuCallback callback, final HistoryMenuDelegate history, final RendezvousMenuDelegate rendezvous) {
        this(FolderBookmarkCollection.favoritesCollection(), BOOKMARKS_INDEX, callback, history, rendezvous);
    }

    public BookmarkMenuDelegate(final AbstractHostCollection collection, final int index,
                                final MenuCallback callback, final HistoryMenuDelegate history, final RendezvousMenuDelegate rendezvous) {
        super(collection);
        this.collection = collection;
        this.index = index;
        this.historyMenuDelegate = history;
        this.rendezvousMenuDelegate = rendezvous;
        this.historyMenu.setDelegate(historyMenuDelegate.id());
        this.rendezvousMenu.setDelegate(rendezvousMenuDelegate.id());
        this.callback = callback;
    }

    @Override
    public NSInteger numberOfItemsInMenu(NSMenu menu) {
        if(this.isPopulated()) {
            // If you return a negative value, the number of items is left unchanged
            // and menu:updateItem:atIndex:shouldCancel: is not called.
            return new NSInteger(-1);
        }
        /**
         * Toogle Bookmarks
         * Sort By
         * ----------------
         * New Bookmark
         * Edit Bookmark
         * Delete Bookmark
         * Duplicate Bookmark
         * ----------------
         * History
         * Bonjour
         * ----------------
         * ...
         */
        return new NSInteger(collection.size() + index + 3);
    }

    @Override
    public Host itemForIndex(final NSInteger row) {
        return collection.get(row.intValue() - (index + 3));
    }

    @Override
    public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, NSInteger row, boolean cancel) {
        if(row.intValue() == index) {
            item.setEnabled(true);
            item.setTitle(LocaleFactory.get().localize("History", "Localizable"));
            item.setImage(IconCacheFactory.<NSImage>get().iconNamed("history.tiff", 16));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("historyMenuClicked:"));
            item.setSubmenu(historyMenu);
        }
        if(row.intValue() == index + 1) {
            item.setEnabled(true);
            item.setTitle(LocaleFactory.get().localize("Bonjour", "Main"));
            item.setImage(IconCacheFactory.<NSImage>get().iconNamed("rendezvous.tiff", 16));
            item.setSubmenu(rendezvousMenu);
        }
        if(row.intValue() == index + 2) {
            menu.removeItemAtIndex(row);
            menu.insertItem_atIndex(this.seperator(), row);
        }
        if(row.intValue() > index + 2) {
            Host h = this.itemForIndex(row);
            this.build(item, h);
        }
        return super.menuUpdateItemAtIndex(menu, item, row, cancel);
    }

    private void build(final NSMenuItem item, final Host h) {
        final NSMutableAttributedString title = NSMutableAttributedString.create(BookmarkNameProvider.toString(h));
        if(preferences.getInteger("bookmark.menu.icon.size") >= BookmarkCell.MEDIUM_BOOKMARK_SIZE) {
            title.appendAttributedString(NSAttributedString.attributedStringWithAttributes(
                String.format("\n%s", h.getHostname()), BundleController.MENU_HELP_FONT_ATTRIBUTES));
        }
        if(preferences.getInteger("bookmark.menu.icon.size") >= BookmarkCell.LARGE_BOOKMARK_SIZE) {
            title.appendAttributedString(NSAttributedString.attributedStringWithAttributes(
                String.format("\n%s", StringUtils.isNotBlank(h.getCredentials().getUsername()) ? h.getCredentials().getUsername() : StringUtils.EMPTY), BundleController.MENU_HELP_FONT_ATTRIBUTES));
        }
        item.setAttributedTitle(title);
        item.setTitle(BookmarkNameProvider.toString(h));
        switch(preferences.getInteger("bookmark.menu.icon.size")) {
            default:
                item.setImage(IconCacheFactory.<NSImage>get().iconNamed(h.getProtocol().icon(), CollectionMenuDelegate.SMALL_ICON_SIZE));
                break;
            case BookmarkCell.MEDIUM_BOOKMARK_SIZE:
                item.setImage(IconCacheFactory.<NSImage>get().iconNamed(h.getProtocol().icon(), CollectionMenuDelegate.MEDIUM_ICON_SIZE));
                break;
            case BookmarkCell.LARGE_BOOKMARK_SIZE:
                item.setImage(IconCacheFactory.<NSImage>get().iconNamed(h.getProtocol().icon(), CollectionMenuDelegate.LARGE_ICON_SIZE));
                break;
        }
        item.setTarget(this.id());
        item.setAction(this.getDefaultAction());
        item.setRepresentedObject(h.getUuid());
    }

    @Action
    public void bookmarkMenuItemClicked(final NSMenuItem sender) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Menu item clicked %s", sender));
        }
        callback.selected(sender);
    }

    @Action
    public void historyMenuClicked(NSMenuItem sender) {
        ApplicationLauncherFactory.get().open(HistoryCollection.defaultCollection().getFolder());
    }

    @Override
    public Selector getDefaultAction() {
        return Foundation.selector("bookmarkMenuItemClicked:");
    }
}
