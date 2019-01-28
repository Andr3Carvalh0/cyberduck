package ch.cyberduck.ui.cocoa.callback;

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

import ch.cyberduck.binding.AlertController;
import ch.cyberduck.binding.WindowController;
import ch.cyberduck.binding.application.SheetCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.diagnostics.ReachabilityFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.notification.NotificationAlertCallback;
import ch.cyberduck.core.threading.AlertCallback;
import ch.cyberduck.core.threading.DefaultFailureDiagnostics;
import ch.cyberduck.ui.cocoa.controller.BackgroundExceptionAlertController;

public class PromptAlertCallback implements AlertCallback {

    private final WindowController parent;
    private final NotificationAlertCallback notification = new NotificationAlertCallback();

    public PromptAlertCallback(final WindowController parent) {
        this.parent = parent;
    }

    @Override
    public boolean alert(final Host host, final BackgroundException failure, final StringBuilder transcript) {
        switch(new DefaultFailureDiagnostics().determine(failure)) {
            case cancel:
                return false;
            default:
                // Send notification
                notification.alert(host, failure, transcript);
                // Run prompt
                final AlertController alert = new BackgroundExceptionAlertController(failure, host);
                switch(alert.beginSheet(parent)) {
                    case SheetCallback.ALTERNATE_OPTION:
                        ReachabilityFactory.get().diagnose(host);
                        break;
                    case SheetCallback.DEFAULT_OPTION:
                        return true;
                }
                return false;
        }
    }
}
