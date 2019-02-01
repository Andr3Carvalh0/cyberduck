package ch.cyberduck.core;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
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
 * feedback@cyberduck.ch
 */

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BookmarkNameProviderTest {

    @Test
    public void testToString() {
        assertEquals("h – FTP", BookmarkNameProvider.toString(new Host(new TestProtocol(Scheme.ftp), "h")));
        final TestProtocol dav = new TestProtocol(Scheme.http) {
            @Override
            public String getName() {
                return "WebDAV (HTTP)";
            }
        };
        assertEquals("h – WebDAV (HTTP)", BookmarkNameProvider.toString(new Host(dav, "h")));
        assertEquals("h – WebDAV (HTTP)", BookmarkNameProvider.toString(new Host(dav, "h", new Credentials("u", null))));
        assertEquals("u@h – WebDAV (HTTP)", BookmarkNameProvider.toString(new Host(dav, "h", new Credentials("u", null)), true));
        assertEquals("h – WebDAV (HTTP)", BookmarkNameProvider.toString(new Host(dav, "h", new Credentials(null, null)), true));
        assertEquals("h – WebDAV (HTTP)", BookmarkNameProvider.toString(new Host(dav, "h", new Credentials("", null)), true));
    }

    @Test
    public void testToStringNoDefaultHostname() {
        final TestProtocol protocol = new TestProtocol(Scheme.file) {
            @Override
            public String getName() {
                return "Disk";
            }

            @Override
            public String getDefaultHostname() {
                return "";
            }
        };
        assertEquals("Disk", BookmarkNameProvider.toString(new Host(protocol, StringUtils.EMPTY), true));
        assertEquals("Disk", BookmarkNameProvider.toString(new Host(protocol, StringUtils.EMPTY), false));
    }

    @Test
    public void testToStringDefaultHostname() {
        final TestProtocol protocol = new TestProtocol(Scheme.file) {
            @Override
            public String getName() {
                return "P";
            }

            @Override
            public String getDefaultHostname() {
                return "default";
            }
        };
        assertEquals("default \u2013 P", BookmarkNameProvider.toString(new Host(protocol, StringUtils.EMPTY), true));
        assertEquals("default \u2013 P", BookmarkNameProvider.toString(new Host(protocol, StringUtils.EMPTY), false));
    }
}
