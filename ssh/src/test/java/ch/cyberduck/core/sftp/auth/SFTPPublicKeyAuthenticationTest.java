package ch.cyberduck.core.sftp.auth;

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

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.local.DefaultLocalTouchFeature;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.sftp.AbstractSFTPTest;
import ch.cyberduck.core.sftp.SFTPProtocol;
import ch.cyberduck.core.sftp.SFTPSession;
import ch.cyberduck.test.IntegrationTest;

import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public class SFTPPublicKeyAuthenticationTest extends AbstractSFTPTest {

    @Test
    public void testAuthenticateKeyNoPassword() throws Exception {
        Assume.assumeNotNull(System.getProperties().getProperty("sftp.key"));
        final Local key = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        try {
            session.getHost().getCredentials().setIdentity(key);
            new DefaultLocalTouchFeature().touch(key);
            IOUtils.copy(new StringReader(System.getProperties().getProperty("sftp.key")), key.getOutputStream(false), Charset.forName("UTF-8"));
            session.close();
            assertTrue(new SFTPPublicKeyAuthentication(session).authenticate(session.getHost(), new DisabledLoginCallback() {
                @Override
                public Credentials prompt(final Host bookmark, String username, String title, String reason, LoginOptions options) throws LoginCanceledException {
                    fail();
                    throw new LoginCanceledException();
                }
            }, new DisabledCancelCallback()));

        }
        finally {
            key.delete();
        }
    }

    @Test(expected = LoginFailureException.class)
    public void testAuthenticatePuTTYKeyWithWrongPassword() throws Exception {
        Assume.assumeNotNull(System.getProperties().getProperty("sftp.key.putty"));
        final Credentials credentials = new Credentials(
            System.getProperties().getProperty("sftp.user"), ""
        );
        final Local key = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        try {
            credentials.setIdentity(key);
            new DefaultLocalTouchFeature().touch(key);
            IOUtils.copy(new StringReader(System.getProperties().getProperty("sftp.key.putty")), key.getOutputStream(false), Charset.forName("UTF-8"));
            final Host host = new Host(new SFTPProtocol(), "test.cyberduck.ch", credentials);
            final SFTPSession session = new SFTPSession(host);
            session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
            final AtomicBoolean p = new AtomicBoolean();
            assertFalse(new SFTPPublicKeyAuthentication(session).authenticate(host, new DisabledLoginCallback() {
                @Override
                public Credentials prompt(final Host bookmark, String username, String title, String reason, LoginOptions options) throws LoginCanceledException {
                    p.set(true);
                    throw new LoginCanceledException();
                }
            }, new DisabledCancelCallback()));
            assertTrue(p.get());

        }
        finally {
            key.delete();
        }
    }

    @Test(expected = LoginFailureException.class)
    public void testAuthenticateOpenSSHKeyWithPassword() throws Exception {
        Assume.assumeNotNull(System.getProperties().getProperty("sftp.key.openssh.rsa"));
        final Credentials credentials = new Credentials(
            System.getProperties().getProperty("sftp.user"), ""
        );
        final Local key = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        try {
            credentials.setIdentity(key);
            new DefaultLocalTouchFeature().touch(key);
            IOUtils.copy(new StringReader(System.getProperties().getProperty("sftp.key.openssh.rsa")), key.getOutputStream(false), Charset.forName("UTF-8"));
            final Host host = new Host(new SFTPProtocol(), "test.cyberduck.ch", credentials);
            final SFTPSession session = new SFTPSession(host);
            session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
            final AtomicBoolean b = new AtomicBoolean();
            assertTrue(new SFTPPublicKeyAuthentication(session).authenticate(host, new DisabledLoginCallback() {
                @Override
                public Credentials prompt(final Host bookmark, String username, String title, String reason, LoginOptions options) throws LoginCanceledException {
                    b.set(true);
                    throw new LoginCanceledException();
                }
            }, new DisabledCancelCallback()));
            assertTrue(b.get());

        }
        finally {
            key.delete();
        }
    }

    @Test(expected = InteroperabilityException.class)
    public void testUnknownFormat() throws Exception {
        final Credentials credentials = new Credentials(
            System.getProperties().getProperty("sftp.user"), ""
        );
        final Local key = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        try {
            credentials.setIdentity(key);
            new DefaultLocalTouchFeature().touch(key);
            IOUtils.copy(new StringReader("--unknown format"), key.getOutputStream(false), Charset.forName("UTF-8"));
            final Host host = new Host(new SFTPProtocol(), "test.cyberduck.ch", credentials);
            final SFTPSession session = new SFTPSession(host);
            session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
            assertTrue(new SFTPPublicKeyAuthentication(session).authenticate(host, new DisabledLoginCallback() {
                @Override
                public Credentials prompt(final Host bookmark, String username, String title, String reason, LoginOptions options) throws LoginCanceledException {
                    fail();
                    throw new LoginCanceledException();
                }
            }, new DisabledCancelCallback()));

        }
        finally {
            key.delete();
        }
    }
}
