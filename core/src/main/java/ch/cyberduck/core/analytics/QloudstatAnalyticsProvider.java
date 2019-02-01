package ch.cyberduck.core.analytics;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
 * http://cyberduck.ch/
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
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class QloudstatAnalyticsProvider implements AnalyticsProvider {
    private static final Logger log = Logger.getLogger(QloudstatAnalyticsProvider.class);

    private final String target;

    private final PathContainerService containerService
        = new PathContainerService();

    public QloudstatAnalyticsProvider() {
        this(PreferencesFactory.get().getProperty("analytics.provider.qloudstat.setup"));
    }

    public QloudstatAnalyticsProvider(final String target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return URI.create(target).getHost();
    }

    @Override
    public DescriptiveUrl getSetup(final String hostname, final Scheme method, final Path container, final Credentials credentials) {
        final String setup = String.format("provider=%s,protocol=%s,endpoint=%s,key=%s,secret=%s",
            hostname,
            method.name(),
            containerService.getContainer(container).getName(),
            credentials.getUsername(),
            credentials.getPassword());
        final String encoded;
        encoded = this.encode(new String(Base64.encodeBase64(setup.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        final String formatted = String.format("%s?setup=%s", target, encoded);
        if(log.isInfoEnabled()) {
            log.info(String.format("Setup URL %s", formatted));
        }
        return new DescriptiveUrl(URI.create(formatted), DescriptiveUrl.Type.analytics);
    }

    private String encode(final String p) {
        try {
            StringBuilder b = new StringBuilder();
            b.append(URLEncoder.encode(p, "UTF-8"));
            // Becuase URLEncoder uses <code>application/x-www-form-urlencoded</code> we have to replace these
            // for proper URI percented encoding.
            return StringUtils.replaceEach(b.toString(), new String[]{"+", "*", "%7E"}, new String[]{"%20", "%2A", "~"});
        }
        catch(UnsupportedEncodingException e) {
            return null;
        }
    }
}
