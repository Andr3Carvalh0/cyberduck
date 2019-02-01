package ch.cyberduck.core;

import ch.cyberduck.core.local.LocalTouchFactory;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.junit.Test;

import static org.junit.Assert.*;

public class CredentialsTest {

    @Test
    public void testEquals() {
        assertEquals(new Credentials("a", "b"), new Credentials("a", "b"));
        assertNotSame(new Credentials("a", "b"), new Credentials("a", "c"));
        assertNotEquals(new Credentials("a", "b"), new Credentials("a", "c"));
    }

    @Test
    public void testSetIdentity() throws Exception {
        Credentials c = new Credentials();
        c.setIdentity(new Local("~/.ssh/unknown.rsa"));
        assertFalse(c.isPublicKeyAuthentication());
        final Local t = new Local(PreferencesFactory.get().getProperty("tmp.dir"), "id_rsa");
        LocalTouchFactory.get().touch(t);
        c.setIdentity(t);
        assertTrue(c.isPublicKeyAuthentication());
        t.delete();
    }

    @Test
    public void testAnonymous() {
        Credentials c = new Credentials("anonymous", "");
        assertEquals("cyberduck@example.net", c.getPassword());
    }

    @Test
    public void testDefault() {
        Credentials c = new Credentials();
        assertEquals("", c.getUsername());
        assertEquals("", c.getPassword());
    }

    @Test
    public void testNullifyPassword() {
        Credentials c = new Credentials();
        assertEquals("", c.getPassword());
        c.setPassword(null);
        assertNull(c.getPassword());
        c.setPassword("n");
        assertEquals("n", c.getPassword());
        c.setPassword(null);
        assertNull(c.getPassword());
    }

    @Test
    public void testLoginReasonable() {
        Credentials credentials = new Credentials("guest", "changeme");
        assertTrue(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
    }

    @Test
    public void testLoginWithoutUsername() {
        Credentials credentials = new Credentials(null,
                PreferencesFactory.get().getProperty("connection.login.anon.pass"));
        assertFalse(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
    }

    @Test
    public void testLoginWithoutPass() {
        Credentials credentials = new Credentials("guest", null);
        assertFalse(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
    }

    @Test
    public void testLoginWithoutEmptyPass() {
        Credentials credentials = new Credentials("guest", "");
        assertTrue(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
    }

    @Test
    public void testLoginAnonymous1() {
        Credentials credentials = new Credentials(PreferencesFactory.get().getProperty("connection.login.anon.name"),
                PreferencesFactory.get().getProperty("connection.login.anon.pass"));
        assertTrue(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
    }

    @Test
    public void testLoginAnonymous2() {
        Credentials credentials = new Credentials(PreferencesFactory.get().getProperty("connection.login.anon.name"),
                null);
        assertTrue(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
    }

    /**
     * http://trac.cyberduck.ch/ticket/1204
     */
    @Test
    public void testLogin1204() {
        Credentials credentials = new Credentials("cyberduck.login",
                "1seCret");
        assertTrue(credentials.validate(new TestProtocol(Scheme.ftp), new LoginOptions()));
        assertEquals("cyberduck.login", credentials.getUsername());
        assertEquals("1seCret", credentials.getPassword());
    }
}
