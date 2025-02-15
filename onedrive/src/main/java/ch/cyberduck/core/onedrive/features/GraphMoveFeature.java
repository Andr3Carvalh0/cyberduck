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

import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.onedrive.GraphExceptionMappingService;
import ch.cyberduck.core.onedrive.GraphSession;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.codec.binary.StringUtils;
import org.nuxeo.onedrive.client.Files;
import org.nuxeo.onedrive.client.OneDriveAPIException;
import org.nuxeo.onedrive.client.PatchOperation;
import org.nuxeo.onedrive.client.types.DriveItem;
import org.nuxeo.onedrive.client.types.FileSystemInfo;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

public class GraphMoveFeature implements Move {

    private final GraphSession session;
    private final Delete delete;
    private final GraphFileIdProvider idProvider;

    public GraphMoveFeature(final GraphSession session, final GraphFileIdProvider idProvider) {
        this.session = session;
        this.delete = new GraphDeleteFeature(session);
        this.idProvider = idProvider;
    }

    @Override
    public Path move(final Path file, final Path renamed, final TransferStatus status, final Delete.Callback callback, final ConnectionCallback connectionCallback) throws BackgroundException {
        if(status.isExists()) {
            delete.delete(Collections.singletonMap(renamed, status), connectionCallback, callback);
            // Reset file ID for non existing file
            renamed.attributes().setFileId(null);
        }
        final PatchOperation patchOperation = new PatchOperation();
        if(!StringUtils.equals(file.getName(), renamed.getName())) {
            patchOperation.rename(renamed.getName());
        }
        if(!file.getParent().equals(renamed.getParent())) {
            final DriveItem moveTarget = session.toFolder(renamed.getParent());
            patchOperation.move(moveTarget);
        }
        // Keep current timestamp set
        final FileSystemInfo info = new FileSystemInfo();
        info.setLastModifiedDateTime(Instant.ofEpochMilli(file.attributes().getModificationDate()).atOffset(ZoneOffset.UTC));
        patchOperation.facet("fileSystemInfo", info);
        final DriveItem item = session.toItem(file);
        try {
            Files.patch(item, patchOperation);
        }
        catch(OneDriveAPIException e) {
            throw new GraphExceptionMappingService().map("Cannot rename {0}", e, file);
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map("Cannot rename {0}", e, file);
        }
        return renamed.withAttributes(new GraphAttributesFinderFeature(session, idProvider).find(renamed));
    }

    @Override
    public boolean isRecursive(final Path source, final Path target) {
        return true;
    }

    @Override
    public boolean isSupported(final Path source, final Path target) {
        if(!session.isAccessible(source, false)) {
            return false;
        }
        if(!session.getContainer(source).equals(session.getContainer(target))) {
            return false;
        }
        return !source.getType().contains(Path.Type.shared);
    }
}
