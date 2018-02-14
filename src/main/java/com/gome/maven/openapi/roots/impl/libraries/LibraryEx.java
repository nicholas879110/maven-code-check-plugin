/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.impl.libraries;

import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.LibraryProperties;
import com.gome.maven.openapi.roots.libraries.PersistentLibraryKind;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.List;

/**
 *  @author dsl
 */
public interface LibraryEx extends Library {
    List<String> getInvalidRootUrls(OrderRootType type);

    boolean isDisposed();

    
    @Override
    ModifiableModelEx getModifiableModel();

    
    PersistentLibraryKind<?> getKind();

    LibraryProperties getProperties();

    
    String[] getExcludedRootUrls();

    
    VirtualFile[] getExcludedRoots();

    interface ModifiableModelEx extends ModifiableModel {
        void setProperties(LibraryProperties properties);

        LibraryProperties getProperties();

        void setKind(PersistentLibraryKind<?> type);

        PersistentLibraryKind<?> getKind();

        void addExcludedRoot( String url);

        boolean removeExcludedRoot( String url);

        
        String[] getExcludedRootUrls();
    }
}
