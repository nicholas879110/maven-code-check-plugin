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

package com.gome.maven.psi;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;

import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public abstract class FileContextProvider {

    public static final ExtensionPointName<FileContextProvider> EP_NAME = new ExtensionPointName<FileContextProvider>("com.gome.maven.fileContextProvider");

    
    public static FileContextProvider getProvider( final PsiFile file) {
        for (FileContextProvider provider: Extensions.getExtensions(EP_NAME, file.getProject())) {
            if (provider.isAvailable(file)) {
                return provider;
            }
        }
        return null;
    }

    protected abstract boolean isAvailable(final PsiFile file);

    
    public abstract Collection<PsiFileSystemItem> getContextFolders(final PsiFile file);

    
    public abstract PsiFile getContextFile(final PsiFile file);
}
