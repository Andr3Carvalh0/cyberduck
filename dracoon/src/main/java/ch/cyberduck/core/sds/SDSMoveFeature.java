package ch.cyberduck.core.sds;

/*
 * Copyright (c) 2002-2017 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
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
 */

import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.sds.io.swagger.client.ApiException;
import ch.cyberduck.core.sds.io.swagger.client.api.NodesApi;
import ch.cyberduck.core.sds.io.swagger.client.model.MoveNodesRequest;
import ch.cyberduck.core.sds.io.swagger.client.model.UpdateFileRequest;
import ch.cyberduck.core.sds.io.swagger.client.model.UpdateFolderRequest;
import ch.cyberduck.core.sds.io.swagger.client.model.UpdateRoomRequest;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

public class SDSMoveFeature implements Move {

    private final SDSSession session;
    private final SDSNodeIdProvider nodeid;

    private final PathContainerService containerService
        = new SDSPathContainerService();

    public SDSMoveFeature(final SDSSession session, final SDSNodeIdProvider nodeid) {
        this.session = session;
        this.nodeid = nodeid;
    }

    @Override
    public Path move(final Path file, final Path renamed, final TransferStatus status, final Delete.Callback callback, final ConnectionCallback connectionCallback) throws BackgroundException {
        try {
            if(status.isExists()) {
                new SDSDeleteFeature(session, nodeid).delete(Collections.singletonList(renamed), connectionCallback, callback);
            }
            final long nodeId = Long.parseLong(nodeid.getFileid(file, new DisabledListProgressListener()));
            if(!new SimplePathPredicate(file.getParent()).test(renamed.getParent())) {
                // Change parent node
                new NodesApi(session.getClient()).moveNodes(
                    Long.parseLong(nodeid.getFileid(renamed.getParent(), new DisabledListProgressListener())),
                    new MoveNodesRequest().resolutionStrategy(MoveNodesRequest.ResolutionStrategyEnum.AUTORENAME).addNodeIdsItem(
                        nodeId), StringUtils.EMPTY, null);
            }
            if(!StringUtils.equals(file.getName(), renamed.getName())) {
                if(containerService.isContainer(file)) {
                    return new Path(renamed.getParent(), renamed.getName(), renamed.getType(), new SDSAttributesFinderFeature(session, nodeid).toAttributes(
                        new NodesApi(session.getClient()).updateRoom(nodeId,
                            new UpdateRoomRequest().name(renamed.getName()), StringUtils.EMPTY, null)
                    ));
                }
                // Rename
                else if(file.isDirectory()) {
                    return new Path(renamed.getParent(), renamed.getName(), renamed.getType(), new SDSAttributesFinderFeature(session, nodeid).toAttributes(
                        new NodesApi(session.getClient()).updateFolder(nodeId,
                            new UpdateFolderRequest().name(renamed.getName()), StringUtils.EMPTY, null)
                    ));
                }
                else {
                    return new Path(renamed.getParent(), renamed.getName(), renamed.getType(), new SDSAttributesFinderFeature(session, nodeid).toAttributes(
                        new NodesApi(session.getClient()).updateFile(nodeId,
                            new UpdateFileRequest().name(renamed.getName()), StringUtils.EMPTY, null)
                    ));
                }
            }
            // Copy original file attributes
            return new Path(renamed.getParent(), renamed.getName(), renamed.getType(),
                new PathAttributes(renamed.attributes()).withVersionId(file.attributes().getVersionId()));
        }
        catch(ApiException e) {
            throw new SDSExceptionMappingService().map("Cannot rename {0}", e, file);
        }
    }

    @Override
    public boolean isRecursive(final Path source, final Path target) {
        return true;
    }

    @Override
    public boolean isSupported(final Path source, final Path target) {
        if(containerService.isContainer(source)) {
            if(!new SimplePathPredicate(source.getParent()).test(target.getParent())) {
                // Cannot move data room but only rename
                return false;
            }
        }
        if(target.getParent().isRoot() && !source.getParent().isRoot()) {
            // Cannot move file or directory to root but only rename data rooms
            return false;
        }
        if(!new SDSTouchFeature(session, nodeid).validate(target.getName())) {
            return false;
        }
        final SDSPermissionsFeature acl = new SDSPermissionsFeature(session, nodeid);
        return acl.containsRole(source, SDSPermissionsFeature.CHANGE_ROLE) &&
            acl.containsRole(source, SDSPermissionsFeature.DELETE_ROLE) &&
            acl.containsRole(target, SDSPermissionsFeature.CREATE_ROLE);
    }

    @Override
    public Move withDelete(final Delete delete) {
        return this;
    }
}
