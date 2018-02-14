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
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @author cdr
 */
public abstract class EncodingManager extends EncodingRegistry {
     public static final String PROP_NATIVE2ASCII_SWITCH = "native2ascii";
     public static final String PROP_PROPERTIES_FILES_ENCODING = "propertiesFilesEncoding";

    
    public static EncodingManager getInstance() {
        return ServiceManager.getService(EncodingManager.class);
    }

    
    public abstract Collection<Charset> getFavorites();

    @Override
    public abstract boolean isNative2AsciiForPropertiesFiles();

    public abstract void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii);

    
    // returns empty for system default
    public abstract String getDefaultCharsetName();

    public void setDefaultCharsetName( String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * @return null for system-default
     */
    @Override
    
    public abstract Charset getDefaultCharsetForPropertiesFiles( VirtualFile virtualFile);
    public abstract void setDefaultCharsetForPropertiesFiles( VirtualFile virtualFile,  Charset charset);

    public abstract void addPropertyChangeListener( PropertyChangeListener listener,  Disposable parentDisposable);

    
    public abstract Charset getCachedCharsetFromContent( Document document);
}
