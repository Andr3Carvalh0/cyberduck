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

import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class GoogleStorageDeleteFeature implements Delete {
    private static final Logger log = Logger.getLogger(GoogleStorageDeleteFeature.class);

    private final PathContainerService containerService;
    private final GoogleStorageSession session;

    public GoogleStorageDeleteFeature(final GoogleStorageSession session) {
        this.session = session;
        this.containerService = session.getFeature(PathContainerService.class);
    }

    @Override
    public void delete(final Map<Path, TransferStatus> files, final PasswordCallback prompt, final Callback callback) throws BackgroundException {
        try {
            for(Path file : files.keySet()) {
                callback.delete(file);
                if(containerService.isContainer(file)) {
                    session.getClient().buckets().delete(file.getName()).execute();
                }
                else if(file.isPlaceholder()) {
                    log.warn(String.format("Do not attempt to delete placeholder %s", file));
                }
                else {
                    session.getClient().objects().delete(containerService.getContainer(file).getName(), containerService.getKey(file)).execute();
                }
            }
        }
        catch(IOException e) {
            throw new GoogleStorageExceptionMappingService().map(e);
        }
    }

    @Override
    public boolean isSupported(final Path file) {
        return true;
    }
}
