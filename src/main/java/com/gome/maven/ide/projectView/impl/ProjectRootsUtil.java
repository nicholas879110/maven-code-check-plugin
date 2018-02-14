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
package com.gome.maven.ide.projectView.impl;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiCodeFragment;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;

/**
 * @author anna
 * Date: 17-Jan-2008
 */
public class ProjectRootsUtil {
    private ProjectRootsUtil() { }

    public static boolean isSourceRoot(final PsiDirectory psiDirectory) {
        return isSourceRoot(psiDirectory.getVirtualFile(), psiDirectory.getProject());
    }

    public static boolean isSourceRoot(final VirtualFile directoryFile, final Project project) {
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        return directoryFile.equals(fileIndex.getSourceRootForFile(directoryFile));
    }

    public static boolean isInSource( PsiDirectory directory) {
        return isInSource(directory.getVirtualFile(), directory.getProject());
    }

    public static boolean isInSource( VirtualFile directoryFile,  Project project) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        return projectFileIndex.isInSourceContent(directoryFile);
    }

    public static boolean isInTestSource( PsiFile file) {
        VirtualFile vFile = file.getVirtualFile();
        return vFile != null && isInTestSource(vFile, file.getProject());
    }

    public static boolean isInTestSource( VirtualFile directoryFile,  Project project) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        return projectFileIndex.isInTestSourceContent(directoryFile);
    }

    public static boolean isModuleSourceRoot( VirtualFile virtualFile,  final Project project) {
        return getModuleSourceRoot(virtualFile, project) != null;
    }

    
    public static SourceFolder getModuleSourceRoot( VirtualFile root,  Project project) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        final Module module = projectFileIndex.getModuleForFile(root);
        return module != null && !module.isDisposed() ? findSourceFolder(module, root) : null;
    }

    public static boolean isLibraryRoot( VirtualFile directoryFile,  Project project) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (projectFileIndex.isInLibraryClasses(directoryFile)) {
            final VirtualFile parent = directoryFile.getParent();
            return parent == null || !projectFileIndex.isInLibraryClasses(parent);
        }
        return false;
    }

    public static boolean isModuleContentRoot( PsiDirectory directory) {
        return isModuleContentRoot(directory.getVirtualFile(), directory.getProject());
    }

    public static boolean isModuleContentRoot( final VirtualFile directoryFile,  Project project) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        final VirtualFile contentRootForFile = projectFileIndex.getContentRootForFile(directoryFile);
        return directoryFile.equals(contentRootForFile);
    }

    public static boolean isProjectHome( PsiDirectory psiDirectory) {
        return psiDirectory.getVirtualFile().equals(psiDirectory.getProject().getBaseDir());
    }

    public static boolean isOutsideSourceRoot( PsiFile psiFile) {
        if (psiFile == null) return false;
        if (psiFile instanceof PsiCodeFragment) return false;
        final VirtualFile file = psiFile.getVirtualFile();
        if (file == null) return false;
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
        return !projectFileIndex.isInSource(file) && !projectFileIndex.isInLibraryClasses(file);
    }

    
    public static SourceFolder findSourceFolder( Module module,  VirtualFile root) {
        for (ContentEntry entry : ModuleRootManager.getInstance(module).getContentEntries()) {
            for (SourceFolder folder : entry.getSourceFolders()) {
                if (root.equals(folder.getFile())) {
                    return folder;
                }
            }
        }
        return null;
    }

    
    public static ExcludeFolder findExcludeFolder( Module module,  VirtualFile root) {
        for (ContentEntry entry : ModuleRootManager.getInstance(module).getContentEntries()) {
            for (ExcludeFolder folder : entry.getExcludeFolders()) {
                if (root.equals(folder.getFile())) {
                    return folder;
                }
            }
        }
        return null;
    }
}