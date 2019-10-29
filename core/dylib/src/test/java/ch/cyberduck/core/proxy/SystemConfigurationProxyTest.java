package ch.cyberduck.core.proxy;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.TestProtocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemConfigurationProxyTest {

    @Test
    public void testFind() {
        final SystemConfigurationProxy proxy = new SystemConfigurationProxy();
        assertEquals(Proxy.Type.DIRECT, proxy.find(new Host(new TestProtocol(), "cyberduck.io")).getType());
    }

    @Test
    public void testExcludedLocalHost() {
        final SystemConfigurationProxy proxy = new SystemConfigurationProxy();
        assertEquals(Proxy.Type.DIRECT, proxy.find(new Host(new TestProtocol(), "cyberduck.local")).getType());
    }

    @Test
    public void testSimpleExcluded() {
        final SystemConfigurationProxy proxy = new SystemConfigurationProxy();
        assertEquals(Proxy.Type.DIRECT, proxy.find(new Host(new TestProtocol(), "simple")).getType());
    }
}
