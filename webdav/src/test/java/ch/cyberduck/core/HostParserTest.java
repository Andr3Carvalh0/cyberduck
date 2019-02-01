package ch.cyberduck.core;

import ch.cyberduck.core.dav.DAVProtocol;
import ch.cyberduck.core.dav.DAVSSLProtocol;
import ch.cyberduck.core.exception.InvalidHostException;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

public class HostParserTest {

    @Test(expected = InvalidHostException.class)
    public void testParseURLEmpty() {
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get("");
        assertEquals(h.getHostname(), PreferencesFactory.get().getProperty("connection.hostname.default"));
    }

    @Test
    public void testParseHostnameOnly() {
        final HostParser parser = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol());
        assertEquals("hostname", parser.get("hostname").getHostname());
        assertEquals("hostname", parser.get("hostname ").getHostname());
        assertEquals("hostname", parser.get(" hostname").getHostname());
    }

    @Test
    public void testParseHostnameOnlyRemoveTrailingSlash() {
        final HostParser parser = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol());
        assertEquals("hostname", parser.get("hostname/").getHostname());
        assertEquals("hostname", parser.get("hostname//").getHostname());
        assertEquals("", parser.get("/hostname").getHostname());
    }

    @Test
    public void testHostnameAppendPort() {
        final HostParser parser = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol());
        assertEquals("s3.amazonaws.com", parser.get("s3.amazonaws.com:443").getHostname());
        assertEquals("s3.amazonaws.com", parser.get("s3.amazonaws.com:443/").getHostname());
        assertEquals(443, parser.get("s3.amazonaws.com:443").getPort());
        assertEquals(443, parser.get("s3.amazonaws.com:443/").getPort());
    }

    @Test
    public void testParseHttp() {
        String url = "http://www.testrumpus.com/";
        Host h = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol()))).get(url);
        assertEquals("www.testrumpus.com", h.getHostname());
        assertEquals(new TestDAVProtocol(), h.getProtocol());
        assertNotNull(h.getCredentials().getUsername());
        assertEquals("/", h.getDefaultPath());
    }

    @Test
    public void testParseNoProtocolAndCustomPath() {
        String url = "user@hostname/path/to/file";
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get(url);
        assertEquals("hostname", h.getHostname());
        assertNotNull(h.getCredentials().getUsername());
        assertEquals("user", h.getCredentials().getUsername());
        assertNull(h.getCredentials().getPassword());
        assertEquals("/path/to/file", h.getDefaultPath());
    }

    @Test
    public void testParseNoProtocol() {
        String url = "user@hostname";
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get(url);
        assertEquals("hostname", h.getHostname());
        assertNotNull(h.getCredentials().getUsername());
        assertEquals("user", h.getCredentials().getUsername());
        assertNull(h.getCredentials().getPassword());
    }

    @Test
    public void testParseWithTwoAtSymbol() {
        String url = "user@name@hostname";
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get(url);
        assertEquals("hostname", h.getHostname());
        assertNotNull(h.getCredentials().getUsername());
        assertEquals("user@name", h.getCredentials().getUsername());
        assertNull(h.getCredentials().getPassword());
    }

    @Test
    public void testParseWithTwoAtSymbolAndPassword() {
        String url = "user@name:password@hostname";
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get(url);
        assertEquals("hostname", h.getHostname());
        assertNotNull(h.getCredentials().getUsername());
        assertEquals("user@name", h.getCredentials().getUsername());
        assertEquals("password", h.getCredentials().getPassword());
    }

    @Test
    public void testParseWithDefaultPath() {
        String url = "user@hostname/path/to/file";
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get(url);
        assertEquals("/path/to/file", h.getDefaultPath());
    }

    @Test
    public void testParseWithDefaultPathAndCustomPort() {
        String url = "user@hostname:999/path/to/file";
        Host h = new HostParser(new ProtocolFactory(new HashSet<Protocol>()), new TestDAVProtocol()).get(url);
        assertEquals("/path/to/file", h.getDefaultPath());
    }

    @Test
    public void testParseDefaultPathWithAtSign() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVSSLProtocol()))).get("https://mail.sgbio.com/dav/YourEmail@sgbio.com/Briefcase");
        assertEquals("mail.sgbio.com", host.getHostname());
        assertEquals(new DAVSSLProtocol(), host.getProtocol());
        assertEquals(Scheme.https, host.getProtocol().getScheme());
        assertEquals("/dav/YourEmail@sgbio.com/Briefcase", host.getDefaultPath());
        assertEquals("anonymous", host.getCredentials().getUsername());
    }

    @Test
    public void testParseUsernameDefaultPathWithAtSign() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVSSLProtocol()))).get("https://u@mail.sgbio.com/dav/YourEmail@sgbio.com/Briefcase");
        assertEquals("mail.sgbio.com", host.getHostname());
        assertEquals(new TestDAVSSLProtocol(), host.getProtocol());
        assertEquals(Scheme.https, host.getProtocol().getScheme());
        assertEquals("/dav/YourEmail@sgbio.com/Briefcase", host.getDefaultPath());
        assertEquals("u", host.getCredentials().getUsername());
    }

    @Test
    public void testParseUsernamePasswordDefaultPathWithAtSign() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVSSLProtocol()))).get("https://u:p@mail.sgbio.com/dav/YourEmail@sgbio.com/Briefcase");
        assertEquals("mail.sgbio.com", host.getHostname());
        assertEquals(new TestDAVSSLProtocol(), host.getProtocol());
        assertEquals(Scheme.https, host.getProtocol().getScheme());
        assertEquals("/dav/YourEmail@sgbio.com/Briefcase", host.getDefaultPath());
        assertEquals("u", host.getCredentials().getUsername());
        assertEquals("p", host.getCredentials().getPassword());
    }

    @Test
    public void testParseColonInPath() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol()))).get("http://cdn.duck.sh/duck-4.6.2.16174:16179M.pkg");
        assertEquals(80, host.getPort());
        assertEquals("/duck-4.6.2.16174:16179M.pkg", host.getDefaultPath());
    }

    @Test
    public void testParseColonInPathPlusPort() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol()))).get("http://cdn.duck.sh:444/duck-4.6.2.16174:16179M.pkg");
        assertEquals(444, host.getPort());
        assertEquals("/duck-4.6.2.16174:16179M.pkg", host.getDefaultPath());
    }

    @Test
    public void testInvalidPortnumber() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol()))).get("http://hostname:21a");
        assertEquals("hostname", host.getHostname());
        assertEquals(80, host.getPort());
    }

    @Test
    public void testMissingPortNumber() {
        final Host host = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol()))).get("http://hostname:~/sandbox");
        assertEquals("hostname", host.getHostname());
        assertEquals(80, host.getPort());
        assertEquals("~/sandbox", host.getDefaultPath());
    }

    @Test
    public void testParseIpv6() throws Exception {
        final HostParser parser = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol())));
        assertEquals("fc01:2:3:4:5::1", parser.get("http://[fc01:2:3:4:5::1]:2121").getHostname());
        assertEquals(2121, parser.get("http://[fc01:2:3:4:5::1]:2121").getPort());
        assertEquals("user", parser.get("http://user@[fc01:2:3:4:5::1]:2121").getCredentials().getUsername());
        assertEquals("/~/sandbox", parser.get("http://[fc01:2:3:4:5::1]:2121/~/sandbox").getDefaultPath());
        assertEquals("/sandbox", parser.get("http://[fc01:2:3:4:5::1]:2121/sandbox").getDefaultPath());
        assertEquals("/sandbox@a", parser.get("http://[fc01:2:3:4:5::1]:2121/sandbox@a").getDefaultPath());
    }

    @Test
    public void testParseIpv6LinkLocalZoneIndex() throws Exception {
        final HostParser parser = new HostParser(new ProtocolFactory(Collections.singleton(new TestDAVProtocol())));
        assertEquals("fe80::c62c:3ff:fe0b:8670%en0", parser.get("http://[fe80::c62c:3ff:fe0b:8670%en0]/~/sandbox").getHostname());
    }

    private static class TestDAVProtocol extends DAVProtocol {
        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    private static class TestDAVSSLProtocol extends DAVSSLProtocol {
        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
