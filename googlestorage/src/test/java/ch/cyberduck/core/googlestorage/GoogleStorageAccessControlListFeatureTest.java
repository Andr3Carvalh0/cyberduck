package ch.cyberduck.core.googlestorage;

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

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.s3.S3DefaultDeleteFeature;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.test.IntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Category(IntegrationTest.class)
public class GoogleStorageAccessControlListFeatureTest extends AbstractGoogleStorageTest {

    @Test
    public void testWrite() throws Exception {
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory));
        final Path test = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        session.getFeature(Touch.class).touch(test, new TransferStatus());
        final GoogleStorageAccessControlListFeature f = new GoogleStorageAccessControlListFeature(session);
        final Acl acl = new Acl();
        acl.addAll(new Acl.GroupUser(Acl.GroupUser.EVERYONE), new Acl.Role(Acl.Role.READ));
        acl.addAll(new Acl.GroupUser(Acl.GroupUser.AUTHENTICATED), new Acl.Role(Acl.Role.READ));
        f.setPermission(test, acl);
        acl.addAll(new Acl.CanonicalUser("dkocher@sudo.ch", "dkocher"), new Acl.Role(Acl.Role.FULL));
        assertEquals(acl, f.getPermission(test));
        new S3DefaultDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testReadBucket() throws Exception {
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory));
        final GoogleStorageAccessControlListFeature f = new GoogleStorageAccessControlListFeature(session);
        final Acl acl = f.getPermission(container);
        assertTrue(acl.containsKey(new Acl.GroupUser("cloud-storage-analytics@google.com")));
        assertTrue(acl.containsKey(new Acl.GroupUser(acl.getOwner().getIdentifier())));
        assertTrue(acl.containsKey(new Acl.GroupUser(Acl.GroupUser.EVERYONE)));
    }

    @Test
    public void testRoles() {
        final GoogleStorageSession session = new GoogleStorageSession(new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("google.projectid"), null
        )));
        final GoogleStorageAccessControlListFeature f = new GoogleStorageAccessControlListFeature(session);
        assertTrue(f.getAvailableAclUsers().contains(new Acl.CanonicalUser()));
        assertTrue(f.getAvailableAclUsers().contains(new Acl.EmailUser()));
        assertTrue(f.getAvailableAclUsers().contains(new Acl.EmailGroupUser("")));
        assertTrue(f.getAvailableAclUsers().contains(new Acl.DomainUser("")));
    }
}
