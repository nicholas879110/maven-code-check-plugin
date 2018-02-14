package com.gome.maven.util;

import com.gome.maven.openapi.util.text.StringUtil;

public final class LocalFileUrl implements Url {
    private final String path;

    /**
     * Use {@link Urls#newLocalFileUrl(String)} instead
     */
    public LocalFileUrl( String path) {
        this.path = path;
    }

    
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isInLocalFileSystem() {
        return true;
    }

    @Override
    public String toDecodedForm() {
        return path;
    }

    
    @Override
    public String toExternalForm() {
        return path;
    }

    
    @Override
    public String getScheme() {
        return null;
    }

    
    @Override
    public String getAuthority() {
        return null;
    }

    
    @Override
    public String getParameters() {
        return null;
    }

    
    @Override
    public Url trimParameters() {
        return this;
    }

    @Override
    public String toString() {
        return toExternalForm();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || ((o instanceof LocalFileUrl) && path.equals(((LocalFileUrl)o).path));
    }

    @Override
    public boolean equalsIgnoreCase( Url o) {
        return this == o || ((o instanceof LocalFileUrl) && path.equalsIgnoreCase(((LocalFileUrl)o).path));
    }

    @Override
    public boolean equalsIgnoreParameters( Url url) {
        return equals(url);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int hashCodeCaseInsensitive() {
        return StringUtil.stringHashCodeInsensitive(path);
    }
}