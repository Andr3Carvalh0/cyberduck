package ch.cyberduck.core.googledrive;

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

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.test.IntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.EnumSet;

import com.google.api.services.drive.model.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(IntegrationTest.class)
public class DriveFileidProviderTest extends AbstractDriveTest {

    @Test
    public void testGetFileidRoot() throws Exception {
        assertEquals("root", new DriveFileidProvider(new DriveSession(new Host(new DriveProtocol(), ""), new DisabledX509TrustManager(), new DefaultX509KeyManager()))
            .getFileid(new Path("/", EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener()));
    }

    @Test
    public void testGetFileid() throws Exception {
        final Path test = new Path(DriveHomeFinderService.MYDRIVE_FOLDER, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session, new DriveFileidProvider(session).withCache(cache)).touch(test, new TransferStatus());
        assertNotNull(new DriveFileidProvider(session).withCache(cache).getFileid(test, new DisabledListProgressListener()));
        new DriveDeleteFeature(session, new DriveFileidProvider(session).withCache(cache)).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testGetFileidAccentCharacter() throws Exception {
        final Path test = new Path(DriveHomeFinderService.MYDRIVE_FOLDER, String.format("%sà", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session, new DriveFileidProvider(session).withCache(cache)).touch(test, new TransferStatus());
        assertNotNull(new DriveFileidProvider(session).withCache(cache).getFileid(test, new DisabledListProgressListener()));
        new DriveDeleteFeature(session, new DriveFileidProvider(session).withCache(cache)).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testGetFileidSingleQuoteCharacter() throws Exception {
        final Path test = new Path(DriveHomeFinderService.MYDRIVE_FOLDER, String.format("%s'", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session, new DriveFileidProvider(session).withCache(cache)).touch(test, new TransferStatus());
        assertNotNull(new DriveFileidProvider(session).withCache(cache).getFileid(test, new DisabledListProgressListener()));
        new DriveDeleteFeature(session, new DriveFileidProvider(session).withCache(cache)).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testGetFileidBackslashCharacter() throws Exception {
        final Path test = new Path(DriveHomeFinderService.MYDRIVE_FOLDER, String.format("%s\\", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session, new DriveFileidProvider(session).withCache(cache)).touch(test, new TransferStatus());
        assertNotNull(new DriveFileidProvider(session).withCache(cache).getFileid(test, new DisabledListProgressListener()));
        new DriveDeleteFeature(session, new DriveFileidProvider(session).withCache(cache)).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testGetFileidDoubleBackslashCharacter() throws Exception {
        final Path test = new Path(DriveHomeFinderService.MYDRIVE_FOLDER, String.format("%s\\\\", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session, new DriveFileidProvider(session).withCache(cache)).touch(test, new TransferStatus());
        assertNotNull(new DriveFileidProvider(session).withCache(cache).getFileid(test, new DisabledListProgressListener()));
        new DriveDeleteFeature(session, new DriveFileidProvider(session).withCache(cache)).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testGetFileidSameName() throws Exception {
        final String filename = new AlphanumericRandomStringService().random();
        final Path test = new Path(DriveHomeFinderService.MYDRIVE_FOLDER, filename, EnumSet.of(Path.Type.file));
        final DriveFileidProvider fileid = new DriveFileidProvider(session).withCache(cache);
        final Path p1 = new DriveTouchFeature(session, fileid).touch(test, new TransferStatus());
        assertEquals(p1.attributes().getVersionId(), fileid.getFileid(test, new DisabledListProgressListener()));
        final File body = new File();
        body.set("trashed", true);
        session.getClient().files().update(p1.attributes().getVersionId(), body).execute();
        cache.remove(p1);
        final Path p2 = new DriveTouchFeature(session, fileid).touch(test, new TransferStatus());
        assertEquals(p2.attributes().getVersionId(), fileid.getFileid(new Path(DriveHomeFinderService.MYDRIVE_FOLDER, filename, EnumSet.of(Path.Type.file)), new DisabledListProgressListener()));
        session.getClient().files().delete(p1.attributes().getVersionId());
        session.getClient().files().delete(p2.attributes().getVersionId());
    }
}
