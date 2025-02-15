package ch.cyberduck.core.googlestorage;

/*
 * Copyright (c) 2002-2019 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.PathNormalizer;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.EnumSet;

import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;

public class GoogleStorageObjectListService implements ListService {
    private static final Logger log = Logger.getLogger(GoogleStorageObjectListService.class);

    private final Preferences preferences = PreferencesFactory.get();
    private final GoogleStorageSession session;
    private final GoogleStorageAttributesFinderFeature attributes;
    private final PathContainerService containerService;

    public GoogleStorageObjectListService(final GoogleStorageSession session) {
        this.session = session;
        this.attributes = new GoogleStorageAttributesFinderFeature(session);
        this.containerService = session.getFeature(PathContainerService.class);
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        return this.list(directory, listener, String.valueOf(Path.DELIMITER), preferences.getInteger("googlestorage.listing.chunksize"));
    }

    public AttributedList<Path> list(final Path directory, final ListProgressListener listener, final String delimiter, final int chunksize) throws BackgroundException {
        try {
            final Path bucket = containerService.getContainer(directory);
            final AttributedList<Path> objects = new AttributedList<Path>();
            Objects response;
            long revision = 0L;
            String lastKey = null;
            String page = null;
            boolean hasDirectoryPlaceholder = containerService.isContainer(directory);
            do {
                response = session.getClient().objects().list(bucket.getName())
                    .setPageToken(page)
                    // lists all versions of an object as distinct results. The default is false
                    .setVersions(false)
                    .setMaxResults((long) chunksize)
                    .setDelimiter(delimiter)
                    .setPrefix(this.createPrefix(directory))
                    .execute();
                if(response.getItems() != null) {
                    for(StorageObject object : response.getItems()) {
                        final String key = PathNormalizer.normalize(object.getName());
                        if(String.valueOf(Path.DELIMITER).equals(key)) {
                            log.warn(String.format("Skipping prefix %s", key));
                            continue;
                        }
                        if(new Path(bucket, key, EnumSet.of(Path.Type.directory)).equals(directory)) {
                            // Placeholder object, skip
                            hasDirectoryPlaceholder = true;
                            continue;
                        }
                        if(!StringUtils.equals(lastKey, key)) {
                            // Reset revision for next file
                            revision = 0L;
                        }
                        final EnumSet<Path.Type> types = object.getName().endsWith(String.valueOf(Path.DELIMITER))
                            ? EnumSet.of(Path.Type.directory) : EnumSet.of(Path.Type.file);
                        final Path file;
                        final PathAttributes attr = attributes.toAttributes(object);
                        attr.setRevision(++revision);
                        // Copy bucket location
                        attr.setRegion(bucket.attributes().getRegion());
                        if(null == delimiter) {
                            file = new Path(String.format("%s%s", bucket.getAbsolute(), key), types, attr);
                        }
                        else {
                            file = new Path(directory, PathNormalizer.name(key), types, attr);
                        }
                        objects.add(file);
                        lastKey = key;
                    }
                }
                if(response.getPrefixes() != null) {
                    for(String prefix : response.getPrefixes()) {
                        if(String.valueOf(Path.DELIMITER).equals(prefix)) {
                            log.warn(String.format("Skipping prefix %s", prefix));
                            continue;
                        }
                        final String key = PathNormalizer.normalize(prefix);
                        if(new Path(bucket, key, EnumSet.of(Path.Type.directory)).equals(directory)) {
                            continue;
                        }
                        final Path file;
                        final PathAttributes attributes = new PathAttributes();
                        if(null == delimiter) {
                            file = new Path(String.format("%s%s", bucket.getAbsolute(), key), EnumSet.of(Path.Type.directory, Path.Type.placeholder), attributes);
                        }
                        else {
                            file = new Path(directory, PathNormalizer.name(key), EnumSet.of(Path.Type.directory, Path.Type.placeholder), attributes);
                        }
                        attributes.setRegion(bucket.attributes().getRegion());
                        objects.add(file);
                    }
                }
                page = response.getNextPageToken();
                listener.chunk(directory, objects);
            }
            while(page != null);
            if(!hasDirectoryPlaceholder && objects.isEmpty()) {
                throw new NotfoundException(directory.getAbsolute());
            }
            return objects;
        }
        catch(IOException e) {
            throw new GoogleStorageExceptionMappingService().map("Listing directory {0} failed", e, directory);
        }
    }

    protected String createPrefix(final Path directory) {
        // Keys can be listed by prefix. By choosing a common prefix
        // for the names of related keys and marking these keys with
        // a special character that delimits hierarchy, you can use the list
        // operation to select and browse keys hierarchically
        String prefix = StringUtils.EMPTY;
        if(!containerService.isContainer(directory)) {
            // Restricts the response to only contain results that begin with the
            // specified prefix. If you omit this optional argument, the value
            // of Prefix for your query will be the empty string.
            // In other words, the results will be not be restricted by prefix.
            prefix = containerService.getKey(directory);
            if(!prefix.endsWith(String.valueOf(Path.DELIMITER))) {
                prefix += Path.DELIMITER;
            }
        }
        return prefix;
    }

    @Override
    public ListService withCache(final Cache<Path> cache) {
        attributes.withCache(cache);
        return this;
    }
}
