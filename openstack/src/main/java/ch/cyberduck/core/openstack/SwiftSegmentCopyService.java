package ch.cyberduck.core.openstack;

/*
 * Copyright (c) 2002-2020 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.DefaultPathContainerService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Copy;
import ch.cyberduck.core.transfer.TransferStatus;

import java.util.List;

public class SwiftSegmentCopyService implements Copy {

    private final PathContainerService containerService
        = new DefaultPathContainerService();

    private final SwiftSession session;
    private final SwiftRegionService regionService;

    public SwiftSegmentCopyService(final SwiftSession session) {
        this(session, new SwiftRegionService(session));
    }

    public SwiftSegmentCopyService(final SwiftSession session, final SwiftRegionService regionService) {
        this.session = session;
        this.regionService = regionService;
    }

    @Override
    public Path copy(final Path source, final Path target, final TransferStatus status, final ConnectionCallback callback) throws BackgroundException {
        final SwiftSegmentService segmentService = new SwiftSegmentService(session);
        final List<Path> segments = segmentService.list(source);
        if(segments.isEmpty()) {
            return new SwiftDefaultCopyFeature(session, regionService).copy(source, target, status, callback);
        }
        else {
            return new SwiftLargeObjectCopyFeature(session, regionService, segmentService)
                .copy(source, segments, target, status, callback);
        }
    }

    @Override
    public boolean isSupported(final Path source, final Path target) {
        return !containerService.isContainer(source) && !containerService.isContainer(target);
    }
}
