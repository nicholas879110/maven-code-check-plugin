/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.source.resolve.reference.impl.providers;

import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.psi.PsiManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public abstract class FileReferenceHelper {

    public static final ExtensionPointName<FileReferenceHelper> EP_NAME = new ExtensionPointName<FileReferenceHelper>("com.gome.maven.psi.fileReferenceHelper");

    
    public String trimUrl( String url) {
        return url;
    }

    
    public List<? extends LocalQuickFix> registerFixes(FileReference reference) {
        return Collections.emptyList();
    }

    
    public PsiFileSystemItem getPsiFileSystemItem(final Project project,  final VirtualFile file) {
        final PsiManager psiManager = PsiManager.getInstance(project);
        return getPsiFileSystemItem(psiManager, file);
    }

    public static PsiFileSystemItem getPsiFileSystemItem(PsiManager psiManager, VirtualFile file) {
        return file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
    }

    
    public PsiFileSystemItem findRoot(final Project project,  final VirtualFile file) {
        return null;
    }

    
    public Collection<PsiFileSystemItem> getRoots( Module module) {
        return Collections.emptyList();
    }

    
    public abstract Collection<PsiFileSystemItem> getContexts(final Project project,  final VirtualFile file);

    public abstract boolean isMine(final Project project,  final VirtualFile file);

    public boolean isFallback() {
        return false;
    }
}
