/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.openapi.roots.libraries;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public abstract class LibraryPresentationProvider<P extends LibraryProperties> {
    public static final ExtensionPointName<LibraryPresentationProvider> EP_NAME = ExtensionPointName.create("com.intellij.library.presentationProvider");
    private final LibraryKind myKind;

    protected LibraryPresentationProvider( LibraryKind kind) {
        myKind = kind;
    }

    
    public LibraryKind getKind() {
        return myKind;
    }

    
    public abstract Icon getIcon();

    
    public String getDescription( P properties) {
        return null;
    }

    
    public abstract P detect( List<VirtualFile> classesRoots);
}
