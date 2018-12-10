package ch.cyberduck.core.vault.registry;

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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Bulk;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultRegistry;

import java.util.Map;

public class VaultRegistryBulkFeature<R> implements Bulk<R> {

    private final Session<?> session;
    private final Bulk<R> proxy;
    private final VaultRegistry registry;

    public VaultRegistryBulkFeature(final Session<?> session, final Bulk<R> proxy, final VaultRegistry registry) {
        this.session = session;
        this.proxy = proxy;
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R pre(final Transfer.Type type, final Map<TransferItem, TransferStatus> files, final ConnectionCallback callback) throws BackgroundException {
        for(TransferItem file : files.keySet()) {
            return (R) registry.find(session, file.remote).getFeature(session, Bulk.class, proxy).pre(type, files, callback);
        }
        return proxy.pre(type, files, callback);
    }

    @Override
    public Bulk<R> withDelete(final Delete delete) {
        proxy.withDelete(delete);
        return this;
    }

    @Override
    public Bulk<R> withCache(final Cache<Path> cache) {
        proxy.withCache(cache);
        return this;
    }

    @Override
    public void post(final Transfer.Type type, final Map<TransferItem, TransferStatus> files, final ConnectionCallback callback) throws BackgroundException {
        for(TransferItem file : files.keySet()) {
            registry.find(session, file.remote).getFeature(session, Bulk.class, proxy).post(type, files, callback);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VaultRegistryBulkFeature{");
        sb.append("proxy=").append(proxy);
        sb.append('}');
        return sb.toString();
    }
}
