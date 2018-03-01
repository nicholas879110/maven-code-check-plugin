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
package com.gome.maven.psi;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.impl.compiled.ClsFileImpl;

/**
 * @author max
 */
public class ClassFileViewProvider extends SingleRootFileViewProvider {
    public ClassFileViewProvider( final PsiManager manager,  final VirtualFile file) {
        super(manager, file);
    }

    public ClassFileViewProvider( final PsiManager manager,  final VirtualFile virtualFile, final boolean eventSystemEnabled) {
        super(manager, virtualFile, eventSystemEnabled);
    }

    @Override
    protected PsiFile createFile( final Project project,  final VirtualFile vFile,  final FileType fileType) {
        FileIndexFacade fileIndex = ServiceManager.getService(project, FileIndexFacade.class);
        if (!fileIndex.isInLibraryClasses(vFile) && fileIndex.isInSource(vFile)) {
            return null;
        }

        // skip inners & anonymous
        if (isInnerClass(vFile)) return null;

        return new ClsFileImpl(this);
    }

    public static boolean isInnerClass( VirtualFile vFile) {
        String name = vFile.getNameWithoutExtension();
        int index = name.lastIndexOf('$', name.length());
        if (index > 0 && index < name.length() - 1) {
            String supposedParentName = name.substring(0, index) + ".class";
            if (vFile.getParent().findChild(supposedParentName) != null) {
                return true;
            }
        }
        return false;
    }

    
    @Override
    public SingleRootFileViewProvider createCopy( final VirtualFile copy) {
        return new ClassFileViewProvider(getManager(), copy, false);
    }
}