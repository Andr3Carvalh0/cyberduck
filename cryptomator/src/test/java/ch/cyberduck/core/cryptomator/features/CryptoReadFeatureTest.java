package ch.cyberduck.core.cryptomator.features;

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

import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.TestProtocol;
import ch.cyberduck.core.cryptomator.CryptoVault;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

public class CryptoReadFeatureTest {

    @Test
    public void testCalculations() throws Exception {
        final NullSession session = new NullSession(new Host(new TestProtocol())) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T _getFeature(final Class<T> type) {
                if(type == Read.class) {
                    return (T) new Read() {
                        @Override
                        public InputStream read(final Path file, final TransferStatus status, final ConnectionCallback callback) {
                            final String masterKey = "{\n" +
                                    "  \"scryptSalt\": \"NrC7QGG/ouc=\",\n" +
                                    "  \"scryptCostParam\": 16384,\n" +
                                    "  \"scryptBlockSize\": 8,\n" +
                                    "  \"primaryMasterKey\": \"Q7pGo1l0jmZssoQh9rXFPKJE9NIXvPbL+HcnVSR9CHdkeR8AwgFtcw==\",\n" +
                                    "  \"hmacMasterKey\": \"xzBqT4/7uEcQbhHFLC0YmMy4ykVKbuvJEA46p1Xm25mJNuTc20nCbw==\",\n" +
                                    "  \"versionMac\": \"hlNr3dz/CmuVajhaiGyCem9lcVIUjDfSMLhjppcXOrM=\",\n" +
                                    "  \"version\": 5\n" +
                                    "}";
                            return IOUtils.toInputStream(masterKey, Charset.defaultCharset());
                        }

                        @Override
                        public boolean offset(final Path file) {
                            return false;
                        }
                    };
                }
                return super._getFeature(type);
            }
        };
        final Path home = new Path("/", EnumSet.of((Path.Type.directory)));
        final CryptoVault vault = new CryptoVault(home);
        assertEquals(home, vault.load(session, new DisabledPasswordCallback() {
            @Override
            public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) {
                return new VaultCredentials("vault");
            }
        }, new DisabledPasswordStore()).getHome());
        CryptoReadFeature read = new CryptoReadFeature(null, null, vault);
        {
            assertEquals(0, read.chunk(0));
            assertEquals(0, read.chunk(1));
            assertEquals(1, read.chunk(32768));
            assertEquals(1, read.chunk(32769));
        }
        {
            assertEquals(88, read.align(1));
            assertEquals(88, read.align(88));
            assertEquals(88, read.align(89));
            assertEquals(88, read.align(15342));
            assertEquals(88, read.align(32767));
            assertEquals(88 + 48 + 32768, read.align(32768));
            assertEquals(88 + 48 + 32768, read.align(32769));
        }
        {
            assertEquals(24, read.position(24));
            assertEquals(0, read.position(0));
            assertEquals(0, read.position(32768));
            assertEquals(1, read.position(32769));
        }
        vault.close();
    }
}
