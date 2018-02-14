/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.util;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.SystemInfoRt;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.StandardFileSystems;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.io.URLUtil;
import gnu.trove.TObjectHashingStrategy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Urls {
    private static final Logger LOG = Logger.getInstance(Urls.class);

    // about ";" see WEB-100359
    private static final Pattern URI_PATTERN = Pattern.compile("^([^:/?#]+):(//)?([^/?#]*)([^?#;]*)(.*)");

    
    public static Url newUri( String scheme,  String path) {
        return new UrlImpl(scheme, null, path);
    }

    
    public static Url newLocalFileUrl( String path) {
        return new LocalFileUrl(path);
    }

    
    public static Url newLocalFileUrl( VirtualFile file) {
        return newLocalFileUrl(file.getPath());
    }

    
    public static Url newFromEncoded( String url) {
        Url result = parseEncoded(url);
        LOG.assertTrue(result != null, url);
        return result;
    }

    
    public static Url parseEncoded( String url) {
        return parse(url, false);
    }

    
    public static Url newHttpUrl( String authority,  String path) {
        return newUrl("http", authority, path);
    }

    
    public static Url newUrl( String scheme,  String authority,  String path) {
        return new UrlImpl(scheme, authority, path);
    }

    
    /**
     * Url will not be normalized (see {@link VfsUtilCore#toIdeaUrl(String)}), parsed as is
     */
    public static Url newFromIdea( String url) {
        Url result = parseFromIdea(url);
        LOG.assertTrue(result != null, url);
        return result;
    }

    // java.net.URI.create cannot parse "file:///Test Stuff" - but you don't need to worry about it - this method is aware
    
    public static Url parseFromIdea( String url) {
        return URLUtil.containsScheme(url) ? parseUrl(url) : newLocalFileUrl(url);
    }

    
    public static Url parse( String url, boolean asLocalIfNoScheme) {
        if (url.isEmpty()) {
            return null;
        }

        if (asLocalIfNoScheme && !URLUtil.containsScheme(url)) {
            // nodejs debug - files only in local filesystem
            return newLocalFileUrl(url);
        }
        return parseUrl(VfsUtilCore.toIdeaUrl(url));
    }

    
    public static URI parseAsJavaUriWithoutParameters( String url) {
        Url asUrl = parseUrl(url);
        if (asUrl == null) {
            return null;
        }

        try {
            return toUriWithoutParameters(asUrl);
        }
        catch (Exception e) {
            LOG.info("Cannot parse url " + url, e);
            return null;
        }
    }

    
    private static Url parseUrl( String url) {
        String urlToParse;
        if (url.startsWith("jar:file://")) {
            urlToParse = url.substring("jar:".length());
        }
        else {
            urlToParse = url;
        }

        Matcher matcher = URI_PATTERN.matcher(urlToParse);
        if (!matcher.matches()) {
            return null;
        }
        String scheme = matcher.group(1);
        if (urlToParse != url) {
            scheme = "jar:" + scheme;
        }

        String authority = StringUtil.nullize(matcher.group(3));

        String path = StringUtil.nullize(matcher.group(4));
        if (path != null) {
            path = FileUtil.toCanonicalUriPath(path);
        }

        if (authority != null && (StandardFileSystems.FILE_PROTOCOL.equals(scheme) || StringUtil.isEmpty(matcher.group(2)))) {
            path = path == null ? authority : (authority + path);
            authority = null;
        }
        return new UrlImpl(scheme, authority, path, matcher.group(5));
    }

    
    public static Url newFromVirtualFile( VirtualFile file) {
        if (file.isInLocalFileSystem()) {
            return newUri(file.getFileSystem().getProtocol(), file.getPath());
        }
        else {
            Url url = parseUrl(file.getUrl());
            return url == null ? new UrlImpl(file.getPath()) : url;
        }
    }

    public static boolean equalsIgnoreParameters( Url url,  Collection<Url> urls) {
        return equalsIgnoreParameters(url, urls, true);
    }

    public static boolean equalsIgnoreParameters( Url url,  Collection<Url> urls, boolean caseSensitive) {
        for (Url otherUrl : urls) {
            if (equals(url, otherUrl, caseSensitive, true)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalsIgnoreParameters( Url url,  VirtualFile file) {
        if (file.isInLocalFileSystem()) {
            return url.isInLocalFileSystem() && (SystemInfoRt.isFileSystemCaseSensitive
                    ? url.getPath().equals(file.getPath()) :
                    url.getPath().equalsIgnoreCase(file.getPath()));
        }
        else if (url.isInLocalFileSystem()) {
            return false;
        }

        Url fileUrl = parseUrl(file.getUrl());
        return fileUrl != null && fileUrl.equalsIgnoreParameters(url);
    }

    public static boolean equals( Url url1,  Url url2, boolean caseSensitive, boolean ignoreParameters) {
        if (url1 == null || url2 == null){
            return url1 == url2;
        }

        Url o1 = ignoreParameters ? url1.trimParameters() : url1;
        Url o2 = ignoreParameters ? url2.trimParameters() : url2;
        return caseSensitive ? o1.equals(o2) : o1.equalsIgnoreCase(o2);
    }

    
    public static URI toUriWithoutParameters( Url url) {
        try {
            String externalPath = url.getPath();
            boolean inLocalFileSystem = url.isInLocalFileSystem();
            if (inLocalFileSystem && SystemInfoRt.isWindows && externalPath.charAt(0) != '/') {
                externalPath = '/' + externalPath;
            }
            return new URI(inLocalFileSystem ? "file" : url.getScheme(), inLocalFileSystem ? "" : url.getAuthority(), externalPath, null, null);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static TObjectHashingStrategy<Url> getCaseInsensitiveUrlHashingStrategy() {
        return CaseInsensitiveUrlHashingStrategy.INSTANCE;
    }

    private static final class CaseInsensitiveUrlHashingStrategy implements TObjectHashingStrategy<Url> {
        private static final TObjectHashingStrategy<Url> INSTANCE = new CaseInsensitiveUrlHashingStrategy();

        @Override
        public int computeHashCode(Url url) {
            return url == null ? 0 : url.hashCodeCaseInsensitive();
        }

        @Override
        public boolean equals(Url url1, Url url2) {
            return Urls.equals(url1, url2, false, false);
        }
    }
}