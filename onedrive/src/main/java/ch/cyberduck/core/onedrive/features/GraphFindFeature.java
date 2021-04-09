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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.onedrive.GraphSession;

public class GraphFindFeature implements Find {

    private final GraphSession session;
    private final GraphFileIdProvider idProvider;

    public GraphFindFeature(final GraphSession session, final GraphFileIdProvider idProvider) {
        this.session = session;
        this.idProvider = idProvider;
    }

    @Override
    public boolean find(final Path file) throws BackgroundException {
        try {
            new GraphAttributesFinderFeature(session, idProvider).find(file);
            return true;
        }
        catch(NotfoundException | InteroperabilityException e) {
            return false;
        }
    }

    @Override
    public Find withCache(final Cache<Path> cache) {
        idProvider.withCache(cache);
        return this;
    }
}
