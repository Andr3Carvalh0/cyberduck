/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

#import "SystemConfigurationProxy.h"
#import <JavaNativeFoundation/JNFString.h>

JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_proxy_SystemConfigurationProxy_usePassiveFTPNative(JNIEnv *env, jobject this)
{
JNF_COCOA_ENTER(env);
	return [Proxy usePassiveFTP];
JNF_COCOA_EXIT(env);
}

JNIEXPORT jstring JNICALL Java_ch_cyberduck_core_proxy_SystemConfigurationProxy_findNative(JNIEnv *env, jobject this, jstring target)
{
JNF_COCOA_ENTER(env);
    NSString *uri = [Proxy find:JNFJavaToNSString(env, target)];
    if(nil == uri) {
        return NULL;
    }
	return (*env)->NewStringUTF(env, [uri UTF8String]);
JNF_COCOA_EXIT(env);
}

@implementation Proxy

+ (NSString*)find:(NSString*)targetURL
{
    NSDictionary *defaultConfiguration = (NSDictionary *)CFNetworkCopySystemProxySettings();
    if(!defaultConfiguration) {
        // No proxy settings have been defined
        return nil;
    }
	NSArray *proxyConfigurations = (NSArray *)CFNetworkCopyProxiesForURL((CFURLRef)[NSURL URLWithString:targetURL], (CFDictionaryRef) defaultConfiguration);
    CFRelease(defaultConfiguration);
    if(!proxyConfigurations) {
        // No proxy settings have been defined
        return nil;
    }
    NSEnumerator *enumerator = [proxyConfigurations objectEnumerator];
    NSDictionary *proxyConfiguration;
    while((proxyConfiguration = [enumerator nextObject]) != nil) {
        if(![proxyConfiguration respondsToSelector:@selector(objectForKey:)]) {
            NSLog(@"Invalid proxy configuration");
            continue;
        }
        // Every proxy dictionary has an entry for kCFProxyTypeKey
        if([[proxyConfiguration objectForKey:(NSString *)kCFProxyTypeKey] isEqualToString:(NSString *)kCFProxyTypeNone]) {
            CFRelease(proxyConfigurations);
            return nil;
        }
        // Look for PAC configuration
        if([[proxyConfiguration objectForKey:(NSString *)kCFProxyTypeKey] isEqualToString:(NSString *)kCFProxyTypeAutoConfigurationURL]) {
            // If the type is kCFProxyTypeAutoConfigurationURL, it has an entry for kCFProxyAutoConfigurationURLKey
            NSURL *pacLocation = [proxyConfiguration objectForKey:(NSURL *)kCFProxyAutoConfigurationURLKey];
            if(!pacLocation) {
        		NSLog(@"Failure retrieving auto configuration script location from configuration");
                continue;
            }
            NSError* error;
            // Obtain from URL for automatic proxy configuration
            NSString *pacScript = [NSString stringWithContentsOfURL:pacLocation encoding:NSUTF8StringEncoding error:&error];
            if(!pacScript) {
        		NSLog(@"Failure retrieving auto configuration script from %@: %@", pacLocation, error);
                continue;
            }
            CFErrorRef err = NULL;
            // Executes a proxy auto configuration script to determine the best proxy to use to retrieve a specified URL
            NSArray *pacProxies = (NSArray*)CFNetworkCopyProxiesForAutoConfigurationScript((CFStringRef)pacScript, (CFURLRef)[NSURL URLWithString:targetURL], &err);
            if(err) {
        		NSLog(@"Failure retrieving proxies from auto configuration script: %@", err);
                continue;
            }
            NSEnumerator *enumerator = [pacProxies objectEnumerator];
            NSDictionary *dict;
            NSString *proxyUrl = nil;
            while (nil != (dict = [enumerator nextObject])) {
                proxyUrl = [Proxy evaluate:dict];
                if(nil != proxyUrl) {
                    // Break on first match. The array is ordered optimally for requesting the URL specified.
                    break;
                }
            }
            CFRelease(pacProxies);
            CFRelease(proxyConfigurations);
            return proxyUrl;
        }
        else {
            NSString *proxyUrl = [Proxy evaluate:proxyConfiguration];
            CFRelease(proxyConfigurations);
            return proxyUrl;
        }
    }
    CFRelease(proxyConfigurations);
    // Empty list
    return nil;
}

+ (NSString*)evaluate:(NSDictionary *) dict
{
    if(nil == dict) {
        return nil;
    }
    if(![dict respondsToSelector:@selector(objectForKey:)]) {
        return nil;
    }
    if(nil == [dict objectForKey:(NSString *)kCFProxyTypeKey]) {
        NSLog(@"Missing kCFProxyTypeKey in proxy configuration");
        return nil;
    }
    if(nil == [dict objectForKey:(NSString *)kCFProxyHostNameKey]) {
        NSLog(@"Missing kCFProxyHostNameKey in proxy configuration");
        return nil;
    }
    if(nil == [dict objectForKey:(NSString *)kCFProxyPortNumberKey]) {
        NSLog(@"Missing kCFProxyPortNumberKey in proxy configuration");
        return nil;
    }
    NSString *type = [dict objectForKey:(NSString *)kCFProxyTypeKey];
    if([[type componentsSeparatedByString:@"kCFProxyType"] count] == 2) {
        return [NSString stringWithFormat:@"%@://%@:%@",
               [[type componentsSeparatedByString:@"kCFProxyType"] objectAtIndex:1],
               [dict objectForKey:(NSString *)kCFProxyHostNameKey],
               [dict objectForKey:(NSString *)kCFProxyPortNumberKey]];
    }
    return nil;
}

+ (BOOL)usePassiveFTP
{
	NSDictionary *proxies = (NSDictionary *)SCDynamicStoreCopyProxies(NULL);
    if(!proxies) return NO;
	BOOL enabled = [[proxies objectForKey:(NSString *)kSCPropNetProxiesFTPPassive] boolValue];
	if (proxies != NULL) {
        CFRelease(proxies);
    }
    return enabled;
}

@end
