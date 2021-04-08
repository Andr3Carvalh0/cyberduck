package ch.cyberduck.core.gmxcloud;/*
 * Copyright (c) 2002-2021 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.dav.DAVSession;
import ch.cyberduck.core.features.Quota;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;

import org.apache.log4j.Logger;

public class GmxcloudSession extends DAVSession {
    private static final Logger log = Logger.getLogger(GmxcloudSession.class);

    public GmxcloudSession(final Host host, final X509TrustManager trust,
                           final X509KeyManager key, final Cache<Path> cache) {
        super(host, trust, key, cache);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getFeature(final Class<T> type) {
        if(type == Quota.class) {
            return (T) new GmxcloudQuotaFeature(this);
        }
        return super.getFeature(type);
    }
}
