package ch.cyberduck.core.dav;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.io.StreamCopier;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.shared.DefaultDownloadFeature;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.test.IntegrationTest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public class DAVReadFeatureTest extends AbstractDAVTest {

    @Test(expected = NotfoundException.class)
    public void testReadNotFound() throws Exception {
        final TransferStatus status = new TransferStatus();
        try {
            new DAVReadFeature(session).read(new Path(new DefaultHomeFinderService(session).find(), "nosuchname", EnumSet.of(Path.Type.file)), status, new DisabledConnectionCallback());
        }
        catch(NotfoundException e) {
            assertEquals("Unexpected response (404 OK). Please contact your web hosting service provider for assistance.", e.getDetail());
            throw e;
        }
    }

    @Test
    public void testReadChunkedTransfer() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.ch", new Credentials(
            PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        final LoginConnectionService service = new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
            new DisabledPasswordStore(), new DisabledProgressListener());
        service.connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path test = new Path("/trunk/LICENSE.txt", EnumSet.of(Path.Type.file));
        // Unknown length in status
        final TransferStatus status = new TransferStatus() {
            @Override
            public void setLength(long length) {
                assertEquals(923L, length);
                // Ignore update. As with unknown length for chunked transfer
            }
        };
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        assertEquals(-1L, local.attributes().getSize());
        new DefaultDownloadFeature(session.getFeature(Read.class)).download(test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED),
            new DisabledStreamListener(), status, new DisabledLoginCallback(), new DisabledPasswordCallback());
        assertEquals(35147L, local.attributes().getSize());
    }

    @Test
    public void testReadInterrupt() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.ch", new Credentials(
            PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        final LoginConnectionService service = new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
            new DisabledPasswordStore(), new DisabledProgressListener());
        service.connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path test = new Path("/trunk/LICENSE.txt", EnumSet.of(Path.Type.file));
        // Unknown length in status
        final TransferStatus status = new TransferStatus();
        // Read a single byte
        {
            final InputStream in = new DAVReadFeature(session).read(test, status, new DisabledConnectionCallback());
            assertNotNull(in.read());
            in.close();
        }
        {
            final InputStream in = new DAVReadFeature(session).read(test, status, new DisabledConnectionCallback());
            assertNotNull(in);
            in.close();
        }
        session.close();
    }

    @Test
    public void testReadRange() throws Exception {
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        session.getFeature(Touch.class).touch(test, new TransferStatus());
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = new RandomStringGenerator.Builder().build().generate(1000).getBytes();
        final OutputStream out = local.getOutputStream(false);
        assertNotNull(out);
        IOUtils.write(content, out);
        out.close();
        new DAVUploadFeature(new DAVWriteFeature(session)).upload(
            test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(),
            new TransferStatus().length(content.length),
            new DisabledConnectionCallback());
        final TransferStatus status = new TransferStatus();
        status.setLength(content.length);
        status.setAppend(true);
        status.setOffset(100L);
        final InputStream in = new DAVReadFeature(session).read(test, status.length(content.length - 100), new DisabledConnectionCallback());
        assertNotNull(in);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(content.length - 100);
        new StreamCopier(status, status).transfer(in, buffer);
        final byte[] reference = new byte[content.length - 100];
        System.arraycopy(content, 100, reference, 0, content.length - 100);
        assertArrayEquals(reference, buffer.toByteArray());
        in.close();
        new DAVDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }

    @Test
    public void testReadRangeUnknownLength() throws Exception {
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        session.getFeature(Touch.class).touch(test, new TransferStatus());
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = new RandomStringGenerator.Builder().build().generate(1000).getBytes();
        final OutputStream out = local.getOutputStream(false);
        assertNotNull(out);
        IOUtils.write(content, out);
        out.close();
        new DAVUploadFeature(new DAVWriteFeature(session)).upload(
            test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(),
            new TransferStatus().length(content.length),
            new DisabledConnectionCallback());
        final TransferStatus status = new TransferStatus();
        status.setLength(-1L);
        status.setAppend(true);
        status.setOffset(100L);
        final InputStream in = new DAVReadFeature(session).read(test, status, new DisabledConnectionCallback());
        assertNotNull(in);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(content.length - 100);
        new StreamCopier(status, status).transfer(in, buffer);
        final byte[] reference = new byte[content.length - 100];
        System.arraycopy(content, 100, reference, 0, content.length - 100);
        assertArrayEquals(reference, buffer.toByteArray());
        in.close();
        new DAVDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testReadCloseReleaseEntity() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.ch", new Credentials(
            PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
        session.login(Proxy.DIRECT, new DisabledLoginCallback(), new DisabledCancelCallback());
        final TransferStatus status = new TransferStatus();
        final Path test = new Path("/trunk/LICENSE.txt", EnumSet.of(Path.Type.file));
        final CountingInputStream in = new CountingInputStream(new DAVReadFeature(session).read(test, status, new DisabledConnectionCallback()));
        in.close();
        assertEquals(0L, in.getByteCount(), 0L);
        session.close();
    }

    @Test
    public void testReadPRMarketingWebDAV() throws Exception {
        final Host host = new Host(new DAVProtocol(), "prmarketing.ddns.net", 8080, new Credentials(
            System.getProperties().getProperty("connection.webdav.prmarketing.user"), System.getProperties().getProperty("connection.webdav.prmarketing.password")
        ));
        final DAVSession session = new DAVSession(host);
        try {
            session.open(Proxy.DIRECT, new DisabledHostKeyCallback(), new DisabledLoginCallback());
            session.login(Proxy.DIRECT, new DisabledLoginCallback(), new DisabledCancelCallback());
            final TransferStatus status = new TransferStatus();
            final Path test = new Path("/webdav/j/2017 GENERIC Heater Deck 3.21.17.pptx", EnumSet.of(Path.Type.file));
            InputStream remoteStream = null;
            try {
                remoteStream = new DAVReadFeature(session).read(test, status, new DisabledConnectionCallback());
                while(remoteStream.available() > 0) {
                    remoteStream.read();
                }
            }
            finally {
                if(remoteStream != null) {
                    remoteStream.close();
                }
            }
        }
        finally {
            session.close();
        }
    }
}
