package ch.cyberduck.core.googlestorage;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.UrlProvider;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.*;
import ch.cyberduck.core.identity.DefaultCredentialsIdentityConfiguration;
import ch.cyberduck.core.identity.IdentityConfiguration;
import ch.cyberduck.core.oauth.OAuth2ErrorResponseInterceptor;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.s3.RequestEntityRestStorageService;
import ch.cyberduck.core.s3.S3CopyFeature;
import ch.cyberduck.core.s3.S3DefaultDeleteFeature;
import ch.cyberduck.core.s3.S3DisabledMultipartService;
import ch.cyberduck.core.s3.S3MetadataFeature;
import ch.cyberduck.core.s3.S3MoveFeature;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.s3.S3SingleUploadService;
import ch.cyberduck.core.s3.S3WriteFeature;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.AccessControlListHandler;
import org.jets3t.service.impl.rest.GSAccessControlListHandler;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.WebsiteConfig;
import org.jets3t.service.security.ProviderCredentials;

import java.io.InputStream;
import java.util.Collections;

public class GoogleStorageSession extends S3Session {

    private OAuth2RequestInterceptor authorizationService;

    public GoogleStorageSession(final Host h) {
        super(h);
    }

    public GoogleStorageSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    @Override
    protected Jets3tProperties configure() {
        final Jets3tProperties configuration = super.configure();
        configuration.setProperty("s3service.enable-storage-classes", String.valueOf(false));
        configuration.setProperty("s3service.disable-dns-buckets", String.valueOf(true));
        return configuration;
    }

    @Override
    public RequestEntityRestStorageService connect(final Proxy proxy, final HostKeyCallback key, final LoginCallback prompt) {
        authorizationService = new OAuth2RequestInterceptor(builder.build(proxy, this, prompt).build(), host.getProtocol())
            .withRedirectUri(host.getProtocol().getOAuthRedirectUrl());
        final HttpClientBuilder configuration = builder.build(proxy, this, prompt);
        configuration.addInterceptorLast(authorizationService);
        configuration.setServiceUnavailableRetryStrategy(new OAuth2ErrorResponseInterceptor(host, authorizationService, prompt));
        return new OAuth2RequestEntityRestStorageService(this, this.configure(), configuration);
    }

    @Override
    protected boolean authorize(final HttpUriRequest request, final ProviderCredentials credentials) {
        request.setHeader("x-goog-api-version", "2");
        return true;
    }

    @Override
    public void login(final Proxy proxy, final LoginCallback prompt,
                      final CancelCallback cancel) throws BackgroundException {
        authorizationService.setTokens(authorizationService.authorize(host, prompt, cancel));
    }

    @Override
    protected XmlResponsesSaxParser getXmlResponseSaxParser() throws ServiceException {
        return new XmlResponsesSaxParser(this.configure(), false) {
            @Override
            public AccessControlListHandler parseAccessControlListResponse(InputStream inputStream) throws ServiceException {
                return this.parseAccessControlListResponse(inputStream, new GSAccessControlListHandler());
            }

            @Override
            public BucketLoggingStatusHandler parseLoggingStatusResponse(InputStream inputStream) throws ServiceException {
                return super.parseLoggingStatusResponse(inputStream, new GSBucketLoggingStatusHandler());
            }

            @Override
            public WebsiteConfig parseWebsiteConfigurationResponse(InputStream inputStream) throws ServiceException {
                return super.parseWebsiteConfigurationResponse(inputStream, new GSWebsiteConfigurationHandler());
            }
        };
    }

    /**
     * @return the identifier for the signature algorithm.
     */
    @Override
    protected String getSignatureIdentifier() {
        return "GOOG1";
    }

    /**
     * @return header prefix for general Google Storage headers: x-goog-.
     */
    @Override
    protected String getRestHeaderPrefix() {
        return "x-goog-";
    }

    /**
     * @return header prefix for Google Storage metadata headers: x-goog-meta-.
     */
    @Override
    protected String getRestMetadataPrefix() {
        return "x-goog-meta-";
    }

    private final class OAuth2RequestEntityRestStorageService extends RequestEntityRestStorageService {
        public OAuth2RequestEntityRestStorageService(final GoogleStorageSession session,
                                                     final Jets3tProperties properties,
                                                     final HttpClientBuilder configuration) {
            super(session, properties, configuration);
        }

        @Override
        protected StorageBucket createBucketImpl(String bucketName, String location,
                                                 AccessControlList acl) throws ServiceException {
            return super.createBucketImpl(bucketName, location, acl,
                Collections.singletonMap("x-goog-project-id", host.getCredentials().getUsername()));
        }

        @Override
        protected StorageBucket[] listAllBucketsImpl() throws ServiceException {
            return super.listAllBucketsImpl(
                Collections.singletonMap("x-goog-project-id", host.getCredentials().getUsername()));
        }

        @Override
        public boolean isAuthenticatedConnection() {
            return true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(final Class<T> type) {
        if(type == ListService.class) {
            return (T) new GoogleStorageListService(this);
        }
        if(type == Upload.class) {
            return (T) new S3SingleUploadService(this, new S3WriteFeature(this, new S3DisabledMultipartService()));
        }
        if(type == MultipartWrite.class) {
            return null;
        }
        if(type == Write.class) {
            return (T) new S3WriteFeature(this, new S3DisabledMultipartService());
        }
        if(type == Delete.class) {
            return (T) new S3DefaultDeleteFeature(this);
        }
        if(type == Directory.class) {
            return (T) new GoogleStorageDirectoryFeature(this, new S3WriteFeature(this, new S3DisabledMultipartService()));
        }
        if(type == Move.class) {
            return (T) new S3MoveFeature(this, new GoogleStorageAccessControlListFeature(this));
        }
        if(type == Headers.class) {
            return (T) new S3MetadataFeature(this, new GoogleStorageAccessControlListFeature(this));
        }
        if(type == Metadata.class) {
            return (T) new S3MetadataFeature(this, new GoogleStorageAccessControlListFeature(this));
        }
        if(type == Copy.class) {
            return (T) new S3CopyFeature(this, new GoogleStorageAccessControlListFeature(this));
        }
        if(type == AclPermission.class) {
            return (T) new GoogleStorageAccessControlListFeature(this);
        }
        if(type == DistributionConfiguration.class) {
            return (T) new GoogleStorageWebsiteDistributionConfiguration(this);
        }
        if(type == IdentityConfiguration.class) {
            return (T) new DefaultCredentialsIdentityConfiguration(host);
        }
        if(type == Logging.class) {
            return (T) new GoogleStorageLoggingFeature(this);
        }
        if(type == Lifecycle.class) {
            return null;
        }
        if(type == Versioning.class) {
            return null;
        }
        if(type == Encryption.class) {
            return null;
        }
        if(type == Redundancy.class) {
            return null;
        }
        if(type == UrlProvider.class) {
            return (T) new GoogleStorageUrlProvider(this);
        }
        return super._getFeature(type);
    }
}
