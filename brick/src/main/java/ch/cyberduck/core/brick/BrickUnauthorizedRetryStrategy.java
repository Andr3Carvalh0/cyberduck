package ch.cyberduck.core.brick;

/*
 * Copyright (c) 2002-2020 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.http.DisabledServiceUnavailableRetryStrategy;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.proxy.ProxyHostUrlProvider;
import ch.cyberduck.core.threading.BackgroundAction;
import ch.cyberduck.core.threading.BackgroundActionRegistry;
import ch.cyberduck.core.threading.BackgroundActionStateCancelCallback;
import ch.cyberduck.core.threading.CancelCallback;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class BrickUnauthorizedRetryStrategy extends DisabledServiceUnavailableRetryStrategy {
    private static final Logger log = Logger.getLogger(BrickUnauthorizedRetryStrategy.class);

    private static final int MAX_RETRIES = 1;

    private final HostPasswordStore store = PasswordStoreFactory.get();
    private final BrickSession session;
    private final LoginCallback prompt;
    private final CancelCallback cancel;

    public BrickUnauthorizedRetryStrategy(final BrickSession session, final LoginCallback prompt, final CancelCallback cancel) {
        this.session = session;
        this.prompt = prompt;
        this.cancel = cancel;
    }

    @Override
    public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
        switch(response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_UNAUTHORIZED:
                if(executionCount <= MAX_RETRIES) {
                    // Pairing token no longer valid
                    try {
                        // Reset credentials to force repairing
                        final Host bookmark = session.getHost();
                        final Credentials credentials = bookmark.getCredentials();
                        credentials.reset();
                        session.login(ProxyFactory.get().find(new ProxyHostUrlProvider().get(bookmark)), prompt, new CancelCallback() {
                            @Override
                            public void verify() throws ConnectionCanceledException {
                                cancel.verify();
                                for(BackgroundAction action : BackgroundActionRegistry.global()) {
                                    // Fail if any current background action is canceled
                                    new BackgroundActionStateCancelCallback(action).verify();
                                }
                            }
                        });
                        if(credentials.isSaved()) {
                            store.save(bookmark);
                        }
                        // Notify changed bookmark
                        final AbstractHostCollection bookmarks = BookmarkCollection.defaultCollection();
                        if(bookmarks.isLoaded()) {
                            if(bookmarks.contains(bookmark)) {
                                bookmarks.collectionItemChanged(bookmark);
                            }
                        }
                        credentials.reset();
                        return true;
                    }
                    catch(BackgroundException e) {
                        log.warn(String.format("Failure %s trying to refresh pairing after error response %s", e, response));
                    }
                }
        }
        return false;
    }
}
