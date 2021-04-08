package ch.cyberduck.core.s3;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
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

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.UrlProvider;
import ch.cyberduck.core.auth.AWSSessionCredentialsRetriever;
import ch.cyberduck.core.cdn.Distribution;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.cloudfront.CloudFrontDistributionConfigurationPreloader;
import ch.cyberduck.core.cloudfront.WebsiteCloudFrontDistributionConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionRefusedException;
import ch.cyberduck.core.exception.ConnectionTimeoutException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.ListCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.exception.ResolveFailedException;
import ch.cyberduck.core.features.*;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.kms.KMSEncryptionFeature;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.restore.Glacier;
import ch.cyberduck.core.shared.DelegatingSchedulerFeature;
import ch.cyberduck.core.shared.DisabledBulkFeature;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.ssl.ThreadLocalHostnameDelegatingTrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSCredentialsConfigurator;
import ch.cyberduck.core.threading.CancelCallback;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.AWSSessionCredentials;

import java.util.Collections;
import java.util.Map;

public class S3Session extends HttpSession<RequestEntityRestStorageService> {
    private static final Logger log = Logger.getLogger(S3Session.class);

    private final Preferences preferences
        = PreferencesFactory.get();

    private final Versioning versioning
        = preferences.getBoolean("s3.versioning.enable") ? new S3VersioningFeature(this, new S3AccessControlListFeature(this)) : null;

    private Map<Path, Distribution> distributions = Collections.emptyMap();

    private S3Protocol.AuthenticationHeaderSignatureVersion authenticationHeaderSignatureVersion
        = S3Protocol.AuthenticationHeaderSignatureVersion.getDefault(host.getProtocol());

    public S3Session(final Host host) {
        super(host, new S3BucketHostnameTrustManager(new DisabledX509TrustManager(), host.getHostname()), new DefaultX509KeyManager(), PathCache.empty());
    }

    public S3Session(final Host host, final X509TrustManager trust, final X509KeyManager key, final Cache<Path> cache) {
        super(host, new S3BucketHostnameTrustManager(trust, host.getHostname()), key, cache);
    }

    @Override
    protected void logout() throws BackgroundException {
        try {
            client.shutdown();
        }
        catch(ServiceException e) {
            throw new S3ExceptionMappingService().map(e);
        }
    }

    protected XmlResponsesSaxParser getXmlResponseSaxParser() throws ServiceException {
        return new XmlResponsesSaxParser(client.getConfiguration(), false);
    }

    /**
     * @return the identifier for the signature algorithm.
     */
    protected String getSignatureIdentifier() {
        return "AWS";
    }

    public S3Protocol.AuthenticationHeaderSignatureVersion getSignatureVersion() {
        return authenticationHeaderSignatureVersion;
    }

    public void setSignatureVersion(final S3Protocol.AuthenticationHeaderSignatureVersion authenticationHeaderSignatureVersion) {
        this.authenticationHeaderSignatureVersion = authenticationHeaderSignatureVersion;
    }

    /**
     * @return header prefix for general Google Storage headers: x-goog-.
     */
    protected String getRestHeaderPrefix() {
        return "x-amz-";
    }

    /**
     * @return header prefix for Google Storage metadata headers: x-goog-meta-.
     */
    protected String getRestMetadataPrefix() {
        return "x-amz-meta-";
    }

    @Override
    public RequestEntityRestStorageService connect(final Proxy proxy, final HostKeyCallback hostkey, final LoginCallback prompt, final CancelCallback cancel) {
        final HttpClientBuilder configuration = builder.build(proxy, this, prompt);
        // Only for AWS
        if(S3Session.isAwsHostname(host.getHostname())) {
            configuration.setServiceUnavailableRetryStrategy(new S3TokenExpiredResponseInterceptor(this,
                new ThreadLocalHostnameDelegatingTrustManager(trust, host.getHostname()), key, prompt));
        }
        return new RequestEntityRestStorageService(this, configuration);
    }

    @Override
    public void login(final Proxy proxy, final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        if(Scheme.isURL(host.getProtocol().getContext())) {
            try {
                final Credentials temporary = new AWSSessionCredentialsRetriever(trust, key, this, host.getProtocol().getContext()).get();
                client.setProviderCredentials(new AWSSessionCredentials(temporary.getUsername(), temporary.getPassword(),
                    temporary.getToken()));
            }
            catch(ConnectionTimeoutException | ConnectionRefusedException | ResolveFailedException | NotfoundException | InteroperabilityException e) {
                log.warn(String.format("Failure to retrieve session credentials from . %s", e.getMessage()));
                throw new LoginFailureException(e.getDetail(false), e);
            }
        }
        else {
            final Credentials credentials;
            // Only for AWS
            if(isAwsHostname(host.getHostname())) {
                // Try auto-configure
                credentials = new STSCredentialsConfigurator(
                    new ThreadLocalHostnameDelegatingTrustManager(trust, host.getHostname()), key, prompt).configure(host);
            }
            else {
                credentials = host.getCredentials();
            }
            if(StringUtils.isNotBlank(credentials.getToken())) {
                client.setProviderCredentials(credentials.isAnonymousLogin() ? null :
                    new AWSSessionCredentials(credentials.getUsername(), credentials.getPassword(),
                        credentials.getToken()));
            }
            else {
                client.setProviderCredentials(credentials.isAnonymousLogin() ? null :
                    new AWSCredentials(credentials.getUsername(), credentials.getPassword()));
            }
        }
        if(host.getCredentials().isPassed()) {
            log.warn(String.format("Skip verifying credentials with previous successful authentication event for %s", this));
            return;
        }
        try {
            this.getFeature(ListService.class).list(new S3HomeFinderService(this).find(), new DisabledListProgressListener() {
                @Override
                public void chunk(final Path parent, final AttributedList<Path> list) throws ListCanceledException {
                    throw new ListCanceledException(list);
                }
            });
        }
        catch(ListCanceledException e) {
            // Success
        }
    }

    public static boolean isAwsHostname(final String hostname) {
        return isAwsHostname(hostname, true);
    }

    public static boolean isAwsHostname(final String hostname, boolean cn) {
        if(cn) {
            // Matches s3.amazonaws.com
            // Matches s3.cn-north-1.amazonaws.com.cn
            // Matches s3.cn-northwest-1.amazonaws.com.cn
            // Matches s3-us-gov-west-1.amazonaws.com
            return hostname.matches("([a-z0-9\\-]+\\.)?s3(\\.dualstack)?(\\.[a-z0-9\\-]+)?\\.amazonaws\\.com(\\.cn)?");
        }
        return hostname.matches("([a-z0-9\\-]+\\.)?s3(\\.dualstack)?(\\.[a-z0-9\\-]+)?\\.amazonaws\\.com");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(final Class<T> type) {
        if(type == ListService.class) {
            return (T) new S3ListService(this);
        }
        if(type == Read.class) {
            return (T) new S3ReadFeature(this);
        }
        if(type == MultipartWrite.class) {
            if(S3Session.isAwsHostname(host.getHostname())) {
                return (T) new S3MultipartWriteFeature(this);
            }
            return (T) new S3MultipartWriteFeature(this);
        }
        if(type == Write.class) {
            return (T) new S3WriteFeature(this);
        }
        if(type == Upload.class) {
            return (T) new S3ThresholdUploadService(this);
        }
        if(type == Directory.class) {
            return (T) new S3DirectoryFeature(this, new S3WriteFeature(this, new S3DisabledMultipartService()));
        }
        if(type == Move.class) {
            return (T) new S3MoveFeature(this);
        }
        if(type == Copy.class) {
            if(S3Session.isAwsHostname(host.getHostname())) {
                return (T) new S3ThresholdCopyFeature(this);
            }
            return (T) new S3CopyFeature(this);
        }
        if(type == Delete.class) {
            if(S3Session.isAwsHostname(host.getHostname())) {
                return (T) new S3MultipleDeleteFeature(this);
            }
            return (T) new S3DefaultDeleteFeature(this);
        }
        if(type == AclPermission.class) {
            return (T) new S3AccessControlListFeature(this);
        }
        if(type == Headers.class) {
            return (T) new S3MetadataFeature(this, new S3AccessControlListFeature(this));
        }
        if(type == Metadata.class) {
            return (T) new S3MetadataFeature(this, new S3AccessControlListFeature(this));
        }
        if(type == Touch.class) {
            return (T) new S3TouchFeature(this);
        }
        if(type == Location.class) {
            if(this.isConnected()) {
                return (T) new S3LocationFeature(this, client.getRegionEndpointCache());
            }
            return (T) new S3LocationFeature(this);
        }
        if(type == Versioning.class) {
            return (T) versioning;
        }
        if(type == Logging.class) {
            return (T) new S3LoggingFeature(this);
        }
        if(type == Lifecycle.class) {
            return (T) new S3LifecycleConfiguration(this);
        }
        if(type == Encryption.class) {
            // Only for AWS
            if(S3Session.isAwsHostname(host.getHostname())) {
                return (T) new KMSEncryptionFeature(this, trust, key);
            }
            return null;
        }
        if(type == Redundancy.class) {
            return (T) new S3StorageClassFeature(this);
        }
        if(type == DistributionConfiguration.class) {
            return (T) new WebsiteCloudFrontDistributionConfiguration(this, trust, key, distributions);
        }
        if(type == UrlProvider.class) {
            return (T) new S3UrlProvider(this);
        }
        if(type == Find.class) {
            return (T) new S3FindFeature(this);
        }
        if(type == AttributesFinder.class) {
            return (T) new S3AttributesFinderFeature(this);
        }
        if(type == Home.class) {
            return (T) new S3HomeFinderService(this);
        }
        if(type == TransferAcceleration.class) {
            // Only for AWS. Disable transfer acceleration for AWS GovCloud
            if(host.getHostname().endsWith(preferences.getProperty("s3.hostname.default"))) {
                return (T) new S3TransferAccelerationService(this);
            }
            return null;
        }
        if(type == Bulk.class) {
            // Only for AWS. Disable transfer acceleration for AWS GovCloud
            if(host.getHostname().endsWith(preferences.getProperty("s3.hostname.default"))) {
                return (T) new S3BulkTransferAccelerationFeature(this, new S3TransferAccelerationService(this));
            }
            return (T) new DisabledBulkFeature();
        }
        if(type == Search.class) {
            return (T) new S3SearchFeature(this);
        }
        if(type == IdProvider.class) {
            return (T) new S3VersionIdProvider(this);
        }
        if(type == Scheduler.class) {
            return (T) new DelegatingSchedulerFeature(
                new CloudFrontDistributionConfigurationPreloader(this) {
                    @Override
                    public Map<Path, Distribution> operate(final PasswordCallback callback, final Path container) throws BackgroundException {
                        return distributions = super.operate(callback, container);
                    }
                }
            );
        }
        if(type == Restore.class) {
            return (T) new Glacier(this, trust, key);
        }
        return super._getFeature(type);
    }
}
