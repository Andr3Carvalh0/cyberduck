package ch.cyberduck.core.onedrive.features;

/*
 * Copyright (c) 2002-2018 iterate GmbH. All rights reserved.
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
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.IdProvider;
import ch.cyberduck.core.onedrive.GraphSession;

import org.apache.commons.lang3.StringUtils;

public class GraphFileIdProvider implements IdProvider {

    private final GraphSession session;
    private Cache<Path> cache = PathCache.empty();

    public GraphFileIdProvider(final GraphSession session) {
        this.session = session;
    }

    @Override
    public String getFileid(final Path file, final ListProgressListener listener) throws BackgroundException {
        if(StringUtils.isNotBlank(file.attributes().getVersionId())) {
            return file.attributes().getVersionId();
        }
        if(cache.isCached(file.getParent())) {
            final AttributedList<Path> list = cache.get(file.getParent());
            final Path found = list.find(new SimplePathPredicate(file));
            if(null != found) {
                if(StringUtils.isNotBlank(found.attributes().getVersionId())) {
                    return found.attributes().getVersionId();
                }
            }
        }
        final AttributedList<Path> list = session._getFeature(ListService.class).list(file.getParent(), listener);
        final Path found = list.find(new SimplePathPredicate(file));
        if(null == found) {
            throw new NotfoundException(file.getAbsolute());
        }
        return found.attributes().getVersionId();
    }

    @Override
    public IdProvider withCache(final Cache<Path> cache) {
        this.cache = cache;
        return this;
    }
}
