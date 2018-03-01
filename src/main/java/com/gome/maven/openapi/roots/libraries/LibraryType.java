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
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.gome.maven.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.gome.maven.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.gome.maven.openapi.roots.ui.configuration.FacetsProvider;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.List;

/**
 * Override this class to provide custom library type. The implementation should be registered in plugin.xml:
 * <p>
 * &lt;extensions defaultExtensionNs="com.gome.maven"&gt;<br>
 * &nbsp;&nbsp;&lt;library.type implementation="qualified-class-name"/&gt;<br>
 * &lt;/extensions&gt;
 *
 * @author nik
 */
public abstract class LibraryType<P extends LibraryProperties> extends LibraryPresentationProvider<P> {
    public static final ExtensionPointName<LibraryType<?>> EP_NAME = ExtensionPointName.create("com.gome.maven.library.type");

    public final static OrderRootType[] DEFAULT_EXTERNAL_ROOT_TYPES = {OrderRootType.CLASSES};

    protected LibraryType( PersistentLibraryKind<P> libraryKind) {
        super(libraryKind);
    }

    
    @Override
    public PersistentLibraryKind<P> getKind() {
        return (PersistentLibraryKind<P>) super.getKind();
    }

    /**
     * @return text to show in 'New Library' popup. Return {@code null} if the type should not be shown in the 'New Library' popup
     */
    
    public abstract String getCreateActionName();

    /**
     * Called when a new library of this type is created in Project Structure dialog
     */
    
    public abstract NewLibraryConfiguration createNewLibrary( JComponent parentComponent,  VirtualFile contextDirectory,
                                                              Project project);

    /**
     * @return {@code true} if library of this type can be added as a dependency to {@code module}
     */
    public boolean isSuitableModule( Module module,  FacetsProvider facetsProvider) {
        return true;
    }

    /**
     * Override this method to customize the library roots editor
     * @return {@link com.gome.maven.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor} instance
     */
    
    public LibraryRootsComponentDescriptor createLibraryRootsComponentDescriptor() {
        return null;
    }

    
    public abstract LibraryPropertiesEditor createPropertiesEditor( LibraryEditorComponent<P> editorComponent);

    @Override
    public P detect( List<VirtualFile> classesRoots) {
        return null;
    }

    /**
     * @return Root types to collect library files which do not belong to the project and therefore
     *         indicate that the library is external.
     */
    public OrderRootType[] getExternalRootTypes() {
        return DEFAULT_EXTERNAL_ROOT_TYPES;
    }

    public static LibraryType findByKind(LibraryKind kind) {
        for (LibraryType type : EP_NAME.getExtensions()) {
            if (type.getKind() == kind) {
                return type;
            }
        }
        throw new IllegalArgumentException("Library with kind " + kind + " is not registered");
    }
}
