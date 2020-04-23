package ch.cyberduck.core.onedrive;

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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;

import org.apache.log4j.Logger;
import org.nuxeo.onedrive.client.GroupsIterator;
import org.nuxeo.onedrive.client.resources.GroupItem;

import java.util.EnumSet;
import java.util.Iterator;

public class SharepointGroupListService extends AbstractListService<GroupItem.Metadata> {
    private static final Logger log = Logger.getLogger(SharepointGroupListService.class);

    private final SharepointSession session;

    public SharepointGroupListService(final SharepointSession session) {
        this.session = session;
    }

    @Override
    protected Iterator<GroupItem.Metadata> getIterator(final Path directory) {
        return new GroupsIterator(session.getClient());
    }

    @Override
    protected Path toPath(final GroupItem.Metadata metadata, final Path directory) {
        final PathAttributes attributes = new PathAttributes();
        attributes.setVersionId(metadata.getId());
        return new Path(directory, metadata.getDisplayName(), EnumSet.of(Path.Type.directory, Path.Type.volume), attributes);
    }

    @Override
    public ListService withCache(final Cache<Path> cache) {
        return this;
    }
}
