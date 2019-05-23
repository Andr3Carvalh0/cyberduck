package ch.cyberduck.core.worker;

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

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.ftp.FTPDeleteFeature;
import ch.cyberduck.core.ftp.FTPSession;
import ch.cyberduck.core.ftp.FTPTLSProtocol;
import ch.cyberduck.core.ftp.FTPWriteFeature;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.io.StatusOutputStream;
import ch.cyberduck.core.notification.DisabledNotificationService;
import ch.cyberduck.core.pool.DefaultSessionPool;
import ch.cyberduck.core.pool.PooledSessionFactory;
import ch.cyberduck.core.pool.SessionPool;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.shared.DefaultAttributesFinderFeature;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.transfer.DisabledTransferErrorCallback;
import ch.cyberduck.core.transfer.DisabledTransferPrompt;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferAction;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.TransferOptions;
import ch.cyberduck.core.transfer.TransferSpeedometer;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.UploadTransfer;
import ch.cyberduck.core.vault.DefaultVaultRegistry;
import ch.cyberduck.test.IntegrationTest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class SingleTransferWorkerTest {

    @Test
    public void testTransferredSizeRepeat() throws Exception {
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = new byte[98305];
        new Random().nextBytes(content);
        final OutputStream out = local.getOutputStream(false);
        IOUtils.write(content, out);
        out.close();
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                System.getProperties().getProperty("ftp.user"), System.getProperties().getProperty("ftp.password")
        ));
        final AtomicBoolean failed = new AtomicBoolean();
        final FTPSession session = new FTPSession(host);
        session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
        session.login(Proxy.DIRECT, new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final Transfer t = new UploadTransfer(new Host(new TestProtocol()), test, local);
        final BytecountStreamListener counter = new BytecountStreamListener(new DisabledStreamListener());
        final LoginConnectionService connect = new LoginConnectionService(new DisabledLoginCallback() {
            @Override
            public Credentials prompt(final Host bookmark, final String username, final String title, final String reason, final LoginOptions options) {
                return new Credentials(
                    System.getProperties().getProperty("ftp.user"), System.getProperties().getProperty("ftp.password")
                );
            }
        }, new DisabledHostKeyCallback(), new DisabledPasswordStore(), new DisabledProgressListener());
        final DefaultSessionPool pool = new DefaultSessionPool(connect,
            new DefaultVaultRegistry(new DisabledPasswordCallback()), PathCache.empty(), new DisabledTranscriptListener(), host,
            new GenericObjectPool<Session>(new PooledSessionFactory(connect, new DisabledX509TrustManager(), new DefaultX509KeyManager(),
                PathCache.empty(), host, new DefaultVaultRegistry(new DisabledPasswordCallback())) {
                @Override
                public Session create() {
                    return new FTPSession(host) {
                        final FTPWriteFeature write = new FTPWriteFeature(this) {
                            @Override
                            public StatusOutputStream<Integer> write(final Path file, final TransferStatus status, final ConnectionCallback callback) throws BackgroundException {
                                final StatusOutputStream<Integer> proxy = super.write(file, status, callback);
                                if(failed.get()) {
                                    // Second attempt successful
                                    return proxy;
                                }
                                return new StatusOutputStream<Integer>(new CountingOutputStream(proxy) {
                                    @Override
                                    protected void afterWrite(final int n) throws IOException {
                                        super.afterWrite(n);
                                        if(this.getByteCount() >= 42768L) {
                                            // Buffer size
                                            assertEquals(32768L, status.getOffset());
                                            failed.set(true);
                                            throw new SocketTimeoutException();
                                        }
                                    }
                                }) {
                                    @Override
                                    public Integer getStatus() throws BackgroundException {
                                        return proxy.getStatus();
                                    }
                                };
                            }
                        };

                        @Override
                        @SuppressWarnings("unchecked")
                        public <T> T _getFeature(final Class<T> type) {
                            if(type == Write.class) {
                                return (T) write;
                            }
                            return super._getFeature(type);
                        }
                    };
                }
            }));
        final ConcurrentTransferWorker worker = new ConcurrentTransferWorker(
            pool, SessionPool.DISCONNECTED, t, new TransferOptions(),
            new TransferSpeedometer(t), new DisabledTransferPrompt() {
            @Override
            public TransferAction prompt(final TransferItem file) {
                return TransferAction.overwrite;
            }
        }, new DisabledTransferErrorCallback(),
            new DisabledConnectionCallback(), new DisabledPasswordCallback(), new DisabledProgressListener(), counter, new DisabledNotificationService()
        );
        assertTrue(worker.run(session));
        local.delete();
        assertEquals(98305L, counter.getSent(), 0L);
        assertEquals(98305L, new DefaultAttributesFinderFeature(session).find(test).getSize());
        assertTrue(failed.get());
        new FTPDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }
}
