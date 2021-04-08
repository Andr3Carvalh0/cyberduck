package ch.cyberduck.core;

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

import ch.cyberduck.core.pool.DefaultSessionPool;
import ch.cyberduck.core.pool.SessionPool;
import ch.cyberduck.core.pool.StatefulSessionPool;
import ch.cyberduck.core.pool.StatelessSessionPool;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.vault.VaultRegistry;
import ch.cyberduck.core.vault.VaultRegistryFactory;

import org.apache.log4j.Logger;

import java.util.Arrays;

public class SessionPoolFactory {
    private static final Logger log = Logger.getLogger(SessionPoolFactory.class);

    private SessionPoolFactory() {
        //
    }

    public static SessionPool create(final Controller controller, final Host bookmark, final Cache<Path> cache) {
        return create(controller, bookmark, cache, controller);
    }

    public static SessionPool create(final Controller controller, final Host bookmark, final Cache<Path> cache,
                                     final ProgressListener listener) {
        return create(controller, bookmark, cache, listener, Usage.transfer);
    }

    public static SessionPool create(final Controller controller, final Host bookmark, final Cache<Path> cache,
                                     final Usage... usage) {
        return create(controller, bookmark, cache, controller, usage);
    }

    public static SessionPool create(final Controller controller, final Host bookmark, final Cache<Path> cache,
                                     final ProgressListener listener, final TranscriptListener transcript, final Usage... usage) {
        return create(controller, bookmark, PasswordStoreFactory.get(), LoginCallbackFactory.get(controller), HostKeyCallbackFactory.get(controller,
            bookmark.getProtocol()), listener, transcript, cache, usage);
    }

    public static SessionPool create(final Controller controller, final Host bookmark, final Cache<Path> cache,
                                     final ProgressListener listener, final Usage... usage) {
        return create(controller, bookmark, PasswordStoreFactory.get(), LoginCallbackFactory.get(controller), HostKeyCallbackFactory.get(controller,
            bookmark.getProtocol()), listener, controller, cache, usage);
    }

    public static SessionPool create(final Controller controller, final Host bookmark,
                                     final HostPasswordStore keychain, final LoginCallback login, final HostKeyCallback key,
                                     final ProgressListener listener, final TranscriptListener transcript, final Cache<Path> cache,
                                     final Usage... usage) {
        final LoginConnectionService connect = new LoginConnectionService(login, key, keychain, listener);
        final CertificateStore certificates = CertificateStoreFactory.get();
        return create(connect, transcript, bookmark,
            new KeychainX509TrustManager(CertificateTrustCallbackFactory.get(controller), new DefaultTrustManagerHostnameCallback(bookmark), certificates),
            new KeychainX509KeyManager(CertificateIdentityCallbackFactory.get(controller), bookmark, certificates),
            VaultRegistryFactory.create(keychain, login), cache, usage);
    }

    public static SessionPool create(final ConnectionService connect, final TranscriptListener transcript,
                                     final Host bookmark,
                                     final X509TrustManager x509TrustManager, final X509KeyManager x509KeyManager,
                                     final VaultRegistry registry, final Cache<Path> cache,
                                     final Usage... usage) {
        switch(bookmark.getProtocol().getStatefulness()) {
            case stateful:
                if(Arrays.asList(usage).contains(Usage.browser)) {
                    return stateful(connect, transcript, bookmark, x509TrustManager, x509KeyManager, registry, cache);
                }
                // Break through to default pool
                if(log.isInfoEnabled()) {
                    log.info(String.format("Create new pooled connection pool for %s", bookmark));
                }
                return new DefaultSessionPool(connect, x509TrustManager, x509KeyManager, registry, transcript, bookmark, cache)
                    .withMinIdle(PreferencesFactory.get().getInteger("connection.pool.minidle"))
                    .withMaxIdle(PreferencesFactory.get().getInteger("connection.pool.maxidle"))
                    .withMaxTotal(PreferencesFactory.get().getInteger("connection.pool.maxtotal"));
            default:
                // Stateless protocol
                return stateless(connect, transcript, bookmark, x509TrustManager, x509KeyManager, registry, cache);
        }
    }

    /**
     * @return Single stateless session
     */
    protected static SessionPool stateless(final ConnectionService connect, final TranscriptListener transcript,
                                           final Host bookmark,
                                           final X509TrustManager trust, final X509KeyManager key,
                                           final VaultRegistry vault, final Cache<Path> cache) {
        if(log.isInfoEnabled()) {
            log.info(String.format("Create new stateless connection pool for %s", bookmark));
        }
        final Session<?> session = SessionFactory.create(bookmark, trust, key, cache);
        return new StatelessSessionPool(connect, session, transcript, vault);
    }

    /**
     * @return Single stateful session
     */
    protected static SessionPool stateful(final ConnectionService connect, final TranscriptListener transcript,
                                          final Host bookmark,
                                          final X509TrustManager trust, final X509KeyManager key,
                                          final VaultRegistry vault, final Cache<Path> cache) {
        if(log.isInfoEnabled()) {
            log.info(String.format("Create new stateful connection pool for %s", bookmark));
        }
        final Session<?> session = SessionFactory.create(bookmark, trust, key, cache);
        return new StatefulSessionPool(connect, session, transcript, vault);
    }

    public enum Usage {
        transfer,
        browser
    }
}
