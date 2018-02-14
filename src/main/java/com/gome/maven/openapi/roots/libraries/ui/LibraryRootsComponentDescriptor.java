/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.openapi.roots.libraries.ui;

import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptorFactory;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.libraries.ui.impl.LibraryRootsDetectorImpl;
import com.gome.maven.openapi.util.text.StringUtil;

import java.util.List;

/**
 * Allows to customize a library editor
 *
 * @see com.gome.maven.openapi.roots.libraries.LibraryType#createLibraryRootsComponentDescriptor
 *
 * @author nik
 */
public abstract class LibraryRootsComponentDescriptor {
    /**
     * Defines presentation for root type nodes in the library roots editor
     * @return custom presentation or {@code null} if default presentation should be used
     */
    
    public abstract OrderRootTypePresentation getRootTypePresentation( OrderRootType type);

    /**
     * Provides separate detectors for root types supported by the library type.
     *
     * @return non-empty list of {@link RootDetector}'s implementations
     */
    
    public abstract List<? extends RootDetector> getRootDetectors();

    /**
     * Provides root detector for 'Attach Files' button. It will be used to automatically assign {@link OrderRootType}s for selected files.
     * Also this detector is used when a new library is created
     *
     * @return {@link LibraryRootsDetector}'s implementation
     */
    
    public LibraryRootsDetector getRootsDetector() {
        final List<? extends RootDetector> detectors = getRootDetectors();
        if (detectors.isEmpty()) {
            throw new IllegalStateException("Detectors list is empty for " + this);
        }
        return new LibraryRootsDetectorImpl(detectors);
    }


    /**
     * @return descriptor for the file chooser which will be shown when 'Attach Files' button is pressed
     * @param libraryName
     */
    
    public FileChooserDescriptor createAttachFilesChooserDescriptor( String libraryName) {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleJavaPathDescriptor();
        descriptor.setTitle(StringUtil.isEmpty(libraryName) ? ProjectBundle.message("library.attach.files.action")
                : ProjectBundle.message("library.attach.files.to.library.action", libraryName));
        descriptor.setDescription(ProjectBundle.message("library.attach.files.description"));
        return descriptor;
    }

    /**
     * @return descriptors for additional 'Attach' buttons in the library roots editor
     */
    
    public abstract List<? extends AttachRootButtonDescriptor> createAttachButtons();

    /**
     * @return Array of root types supported by a library type associated with the roots
     *         component descriptor. All persistent root types are returned by default.
     */
    public OrderRootType[] getRootTypes() {
        return OrderRootType.getAllTypes();
    }

    public String getAttachFilesActionName() {
        return ProjectBundle.message("button.text.attach.files");
    }
}
