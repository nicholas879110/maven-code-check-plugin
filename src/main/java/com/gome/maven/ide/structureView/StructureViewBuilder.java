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
package com.gome.maven.ide.structureView;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.KeyedFactoryEPBean;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileTypes.FileTypeExtensionFactory;
import com.gome.maven.openapi.project.Project;

/**
 * Defines the implementation of Structure View and the file structure popup for
 * a file type. This class allows to replace the entire Structure View component
 * implementation. If it is acceptable to have the standard component implementation
 * and to customize only how the Structure View is populated with the file data,
 * the standard implementation of this interface - {@link TreeBasedStructureViewBuilder} -
 * should be used.
 *
 * @see com.gome.maven.lang.LanguageStructureViewBuilder#getStructureViewBuilder(com.gome.maven.psi.PsiFile)
 */

public interface StructureViewBuilder {
    ExtensionPointName<KeyedFactoryEPBean> EP_NAME = ExtensionPointName.create("com.gome.maven.structureViewBuilder");

    StructureViewBuilderProvider PROVIDER =
            new FileTypeExtensionFactory<StructureViewBuilderProvider>(StructureViewBuilderProvider.class, EP_NAME).get();

    /**
     * Returns the structure view implementation for the file displayed in the specified
     * editor.
     *
     * @param fileEditor the editor for which the structure view is requested.
     * @param project    the project containing the file for which the structure view is requested.
     * @return the structure view implementation.
     * @see TreeBasedStructureViewBuilder
     */
    StructureView createStructureView(FileEditor fileEditor,  Project project);
}
