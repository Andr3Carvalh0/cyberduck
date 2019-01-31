package ch.cyberduck.core;

/*
 * Copyright (c) 2002-2018 iterate GmbH. All rights reserved.
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

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class HostParserTest {

    @Test
    public void parseDefaultHostname() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestProtocol(Scheme.https) {
            @Override
            public Type getType() {
                return Type.onedrive;
            }

            @Override
            public String getDefaultHostname() {
                return "defaultHostname";
            }

            @Override
            public boolean isHostnameConfigurable() {
                return false;
            }
        }))).get("https://folder/file");
        assertEquals("defaultHostname", host.getHostname());
        assertEquals("/folder/file", host.getDefaultPath());
    }
}
