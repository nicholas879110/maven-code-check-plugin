package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.components.RoamingType;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public abstract class StreamProvider {
    public static final StreamProvider[] EMPTY_ARRAY = new StreamProvider[0];

    public boolean isEnabled() {
        return true;
    }

    /**
     * fileSpec Only main fileSpec, not version
     */
    public boolean isApplicable( String fileSpec,  RoamingType roamingType) {
        return true;
    }

    /**
     * @param fileSpec
     * @param content bytes of content, size of array is not actual size of data, you must use {@code size}
     * @param size actual size of data
     * @param roamingType
     * @param async
     */
    public abstract void saveContent( String fileSpec,  byte[] content, int size,  RoamingType roamingType, boolean async) throws IOException;

    
    public abstract InputStream loadContent( String fileSpec,  RoamingType roamingType) throws IOException;

    
    public Collection<String> listSubFiles( String fileSpec,  RoamingType roamingType) {
        return Collections.emptyList();
    }

    /**
     * Delete file or directory
     */
    public abstract void delete( String fileSpec,  RoamingType roamingType);
}