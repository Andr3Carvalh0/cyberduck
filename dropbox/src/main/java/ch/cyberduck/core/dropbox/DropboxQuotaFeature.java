package ch.cyberduck.core.dropbox;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Quota;

import java.util.EnumSet;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.users.DbxUserUsersRequests;
import com.dropbox.core.v2.users.SpaceAllocation;
import com.dropbox.core.v2.users.SpaceUsage;

public class DropboxQuotaFeature implements Quota {

    private final DropboxSession session;

    public DropboxQuotaFeature(final DropboxSession session) {
        this.session = session;
    }

    @Override
    public Space get() throws BackgroundException {
        try {
            final SpaceUsage usage = new DbxUserUsersRequests(session.getClient()).getSpaceUsage();
            final SpaceAllocation allocation = usage.getAllocation();
            if(allocation.isIndividual()) {
                long remaining = allocation.getIndividualValue().getAllocated() - usage.getUsed();
                return new Space(usage.getUsed(), remaining);
            }
            else if(allocation.isTeam()) {
                long remaining = allocation.getTeamValue().getAllocated() - usage.getUsed();
                return new Space(usage.getUsed(), remaining);
            }
            return unknown;
        }
        catch(DbxException e) {
            throw new DropboxExceptionMappingService().map("Failure to read attributes of {0}", e,
                    new Path(String.valueOf(Path.DELIMITER), EnumSet.of(Path.Type.volume, Path.Type.directory)));
        }
    }
}
