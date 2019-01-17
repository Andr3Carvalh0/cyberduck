package ch.cyberduck.cli;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
 * http://cyberduck.io/
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
 * feedback@cyberduck.io
 */

import ch.cyberduck.core.BundledProtocolPredicate;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.aquaticprime.DisabledLicenseVerifierCallback;
import ch.cyberduck.core.aquaticprime.License;
import ch.cyberduck.core.aquaticprime.LicenseFactory;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.preferences.SupportDirectoryFinderFactory;
import ch.cyberduck.core.sds.io.swagger.client.StringUtil;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferAction;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;

public final class TerminalHelpPrinter {

    private TerminalHelpPrinter() {
        //
    }

    public static void print(final Options options) {
        print(options, new TerminalHelpFormatter());
    }

    public static void print(final Options options, final HelpFormatter formatter) {
        formatter.setSyntaxPrefix("Usage:");
        final StringBuilder protocols = new StringBuilder(StringUtils.LF);
        protocols.append("Supported protocols").append(StringUtils.LF);
        for(Protocol p : ProtocolFactory.get().find()) {
            protocols.append(String.format("  %s\n", p.getDescription()));
            switch(p.getType()) {
                case b2:
                case s3:
                case googlestorage:
                case swift:
                case azure:
                case onedrive:
                    protocols.append(String.format("    %s://<container>/<key>", getScheme(p)));
                    break;
                default:
                    if(p.isHostnameConfigurable()) {
                        protocols.append(String.format("    %s://<hostname>/<folder>/<file>", getScheme(p)));
                    }
                    else {
                        // case file:
                        // case googledrive:
                        // case dropbox:
                        // case onedrive:
                        protocols.append(String.format("    %s://<folder>/<file>", getScheme(p)));
                    }
                    break;
            }
            protocols.append(StringUtils.LF);
        }

        final StringBuilder transferActions = new StringBuilder("Transfer actions for existing files:").append(StringUtils.LF);
        transferActions.append("  Options for downloads and uploads:").append(StringUtils.LF);
        for(TransferAction a : TransferAction.forTransfer(Transfer.Type.download)) {
            appendTransferAction(a, transferActions);
        }
        for(TransferAction a : Collections.singletonList(TransferAction.cancel)) {
            appendTransferAction(a, transferActions);
        }
        transferActions.append("  Options for synchronize:").append(StringUtils.LF);
        for(TransferAction a : TransferAction.forTransfer(Transfer.Type.sync)) {
            appendTransferAction(a, transferActions);
        }
        for(TransferAction a : Collections.singletonList(TransferAction.cancel)) {
            appendTransferAction(a, transferActions);
        }

        final StringBuilder header = new StringBuilder(StringUtils.LF);
        header.append("\t");
        header.append("URLs must be fully qualified. Paths can either denote "
            + "a remote file (ftps://user@example.net/resource) or folder (ftps://user@example.net/directory/) "
            + "with a trailing slash. You can reference files relative to your home directory with /~ (ftps://user@example.net/~/).");
        header.append(protocols).append(StringUtils.LF);
        header.append(transferActions);
        final Preferences preferences = PreferencesFactory.get();
        final Local profiles = LocalFactory.get(SupportDirectoryFinderFactory.get().find(),
            PreferencesFactory.get().getProperty("profiles.folder.name"));
        header.append(StringUtils.LF);
        header.append(String.format("You can install additional connection profiles in %s",
            profiles.getAbbreviatedPath()));
        header.append(StringUtils.LF);
        final StringBuilder footer = new StringBuilder(StringUtils.LF);
        footer.append(String.format("Cyberduck is libre software licenced under the GPL. For general help about using Cyberduck, please refer to %s and the wiki at %s. For bug reports or feature requests open a ticket at %s.",
            preferences.getProperty("website.cli"), preferences.getProperty("website.help"), MessageFormat.format(preferences.getProperty("website.bug"), preferences.getProperty("application.version"))));
        final License l = LicenseFactory.find();
        footer.append(StringUtils.LF);
        if(l.verify(new DisabledLicenseVerifierCallback())) {
            footer.append(l.toString());
        }
        else {
            footer.append("Not registered. Purchase a donation key to support the development of this software.");
        }
        formatter.printHelp("duck [options...]", header.toString(), options, footer.toString());
    }

    private static void appendTransferAction(final TransferAction action, final StringBuilder builder) {
        builder.append(String.format("  %s  %s (%s)\n",
            StringUtils.leftPad(action.getTitle(), 16), action.getDescription(), action.name()));
    }

    protected static String getScheme(final Protocol protocol) {
        if(new BundledProtocolPredicate().test(protocol)) {
            for(String scheme :
                protocol.getSchemes()) {
                // Return first custom scheme registered
                return scheme;
            }
            // Return default name
            return protocol.getIdentifier();
        }
        // Find parent protocol definition for profile
        final Protocol standard = ProtocolFactory.get().forName(protocol.getIdentifier());
        if(Arrays.equals(protocol.getSchemes(), standard.getSchemes())) {
            // No custom scheme set in profile
            return protocol.getProvider();
        }
        for(String scheme : protocol.getSchemes()) {
            // First custom scheme in profile
            return scheme;
        }
        // Default vendor string of third party profile
        return protocol.getProvider();
    }
}
