/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.StandardFileSystems;
import com.gome.maven.util.io.URLUtil;

public final class UrlImpl implements Url {
    private final String scheme;
    private final String authority;

    private final String path;
    private String decodedPath;

    private final String parameters;

    private String externalForm;
    private UrlImpl withoutParameters;

    public UrlImpl( String path) {
        this(null, null, path, null);
    }

    UrlImpl( String scheme,  String authority,  String path) {
        this(scheme, authority, path, null);
    }

    public UrlImpl( String scheme,  String authority,  String path,  String parameters) {
        this.scheme = scheme;
        this.authority = StringUtil.nullize(authority);
        this.path = StringUtil.isEmpty(path) ? "/" : path;
        this.parameters = StringUtil.nullize(parameters);
    }

    
    @Override
    public String getPath() {
        if (decodedPath == null) {
            decodedPath = URLUtil.unescapePercentSequences(path);
        }
        return decodedPath;
    }

    
    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    
    public String getAuthority() {
        return authority;
    }

    @Override
    public boolean isInLocalFileSystem() {
        return StandardFileSystems.FILE_PROTOCOL.equals(scheme);
    }

    
    @Override
    public String getParameters() {
        return parameters;
    }

    @Override
    public String toDecodedForm() {
        StringBuilder builder = new StringBuilder();
        if (scheme != null) {
            builder.append(scheme);
            if (authority != null || isInLocalFileSystem()) {
                builder.append(URLUtil.SCHEME_SEPARATOR);
            }
            else {
                builder.append(':');
            }

            if (authority != null) {
                builder.append(authority);
            }
        }
        builder.append(getPath());
        if (parameters != null) {
            builder.append(parameters);
        }
        return builder.toString();
    }

    @Override
    
    public String toExternalForm() {
        if (externalForm != null) {
            return externalForm;
        }

        // relative path - special url, encoding is not required
        // authority is null in case of URI or file URL
        if ((path.charAt(0) != '/' || authority == null) && !isInLocalFileSystem()) {
            return toDecodedForm();
        }

        String result = Urls.toUriWithoutParameters(this).toASCIIString();
        if (parameters != null) {
            result += parameters;
        }
        externalForm = result;
        return result;
    }

    @Override
    
    public Url trimParameters() {
        if (parameters == null) {
            return this;
        }
        else if (withoutParameters == null) {
            withoutParameters = new UrlImpl(scheme, authority, path, null);
        }
        return withoutParameters;
    }

    @Override
    public String toString() {
        return toExternalForm();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UrlImpl)) {
            return false;
        }

        UrlImpl url = (UrlImpl)o;
        return StringUtil.equals(scheme, url.scheme) && StringUtil.equals(authority, url.authority) && getPath().equals(url.getPath()) && StringUtil.equals(parameters, url.parameters);
    }

    @Override
    public boolean equalsIgnoreCase( Url o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UrlImpl)) {
            return false;
        }

        UrlImpl url = (UrlImpl)o;
        return StringUtil.equalsIgnoreCase(scheme, url.scheme) &&
                StringUtil.equalsIgnoreCase(authority, url.authority) &&
                getPath().equalsIgnoreCase(url.getPath()) &&
                StringUtil.equalsIgnoreCase(parameters, url.parameters);
    }

    @Override
    public boolean equalsIgnoreParameters( Url url) {
        return url != null && equals(url.trimParameters());
    }

    private int computeHashCode(boolean caseSensitive) {
        int result = stringHashCode(scheme, caseSensitive);
        result = 31 * result + stringHashCode(authority, caseSensitive);
        result = 31 * result + stringHashCode(getPath(), caseSensitive);
        result = 31 * result + stringHashCode(parameters, caseSensitive);
        return result;
    }

    private static int stringHashCode( CharSequence string, boolean caseSensitive) {
        return string == null ? 0 : (caseSensitive ? string.hashCode() : StringUtil.stringHashCodeInsensitive(string));
    }

    @Override
    public int hashCode() {
        return computeHashCode(true);
    }

    @Override
    public int hashCodeCaseInsensitive() {
        return computeHashCode(false);
    }
}