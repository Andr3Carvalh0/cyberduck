package ch.cyberduck.core.cryptomator;

/*
 * Copyright (c) 2002-2017 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.cryptomator.features.CryptoAttributesFeature;
import ch.cyberduck.core.cryptomator.features.CryptoDeleteFeature;
import ch.cyberduck.core.cryptomator.features.CryptoFindFeature;
import ch.cyberduck.core.cryptomator.features.CryptoTouchFeature;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.s3.S3AttributesFinderFeature;
import ch.cyberduck.core.s3.S3DefaultDeleteFeature;
import ch.cyberduck.core.s3.S3FindFeature;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.s3.S3SingleUploadService;
import ch.cyberduck.core.s3.S3TouchFeature;
import ch.cyberduck.core.s3.S3WriteFeature;
import ch.cyberduck.core.shared.DefaultFindFeature;
import ch.cyberduck.core.shared.DefaultTouchFeature;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.DefaultVaultRegistry;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.test.IntegrationTest;

import org.apache.commons.text.RandomStringGenerator;
import org.jets3t.service.model.StorageObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class S3TouchFeatureTest {

    @Test
    public void testTouchEncrypted() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("s3.key"), System.getProperties().getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
        session.login(Proxy.DIRECT, new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path home = new Path("test-us-east-1-cyberduck", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path vault = new Path(home, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        final CryptoVault cryptomator = new CryptoVault(vault);
        cryptomator.create(session, null, new VaultCredentials("test"), new DisabledPasswordStore());
        session.withRegistry(new DefaultVaultRegistry(new DisabledPasswordStore(), new DisabledPasswordCallback(), cryptomator));
        final Path test = new CryptoTouchFeature<StorageObject>(session, new S3TouchFeature(session), new S3WriteFeature(session), cryptomator).touch(
            new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file)), new TransferStatus());
        assertTrue(new CryptoFindFeature(session, new DefaultFindFeature(session), cryptomator).find(test));
        assertEquals(test.attributes(), new CryptoAttributesFeature(session, new S3AttributesFinderFeature(session), cryptomator).find(test));
        new CryptoDeleteFeature(session, new S3DefaultDeleteFeature(session), cryptomator).delete(Arrays.asList(test, vault), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }

    @Test
    public void testTouchLongFilenameEncrypted() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("s3.key"), System.getProperties().getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
        session.login(Proxy.DIRECT, new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path home = new Path("test-us-east-1-cyberduck", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path vault = new Path(home, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        final Path test = new Path(vault, new RandomStringGenerator.Builder().build().generate(130), EnumSet.of(Path.Type.file));
        final CryptoVault cryptomator = new CryptoVault(vault);
        cryptomator.create(session, null, new VaultCredentials("test"), new DisabledPasswordStore());
        session.withRegistry(new DefaultVaultRegistry(new DisabledPasswordStore(), new DisabledPasswordCallback(), cryptomator));
        new CryptoTouchFeature<StorageObject>(session, new S3TouchFeature(session), new S3WriteFeature(session), cryptomator).touch(test, new TransferStatus());
        assertTrue(new CryptoFindFeature(session, new S3FindFeature(session), cryptomator).find(test));
        new CryptoDeleteFeature(session, new S3DefaultDeleteFeature(session), cryptomator).delete(Arrays.asList(test, vault), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }

    @Test
    public void testTouchEncryptedDefaultFeature() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("s3.key"), System.getProperties().getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
        session.login(Proxy.DIRECT, new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path home = new Path("test-us-east-1-cyberduck", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path vault = new Path(home, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        final Path test = new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        final CryptoVault cryptomator = new CryptoVault(vault);
        cryptomator.create(session, null, new VaultCredentials("test"), new DisabledPasswordStore());
        session.withRegistry(new DefaultVaultRegistry(new DisabledPasswordStore(), new DisabledPasswordCallback(), cryptomator));
        new CryptoTouchFeature<StorageObject>(session, new DefaultTouchFeature<StorageObject>(new S3SingleUploadService(session, new S3WriteFeature(session)),
            new S3AttributesFinderFeature(session)), new S3WriteFeature(session), cryptomator).touch(test, new TransferStatus());
        assertTrue(new CryptoFindFeature(session, new DefaultFindFeature(session), cryptomator).find(test));
        new CryptoDeleteFeature(session, new S3DefaultDeleteFeature(session), cryptomator).delete(Arrays.asList(test, vault), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }
}
