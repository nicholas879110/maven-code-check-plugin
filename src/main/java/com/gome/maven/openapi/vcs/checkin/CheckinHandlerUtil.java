/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.checkin;

import com.gome.maven.openapi.components.StorageScheme;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.roots.GeneratedSourcesFilter;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.util.PsiUtilCore;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author oleg
 */
public class CheckinHandlerUtil {
    public static List<VirtualFile> filterOutGeneratedAndExcludedFiles( Collection<VirtualFile> files,  Project project) {
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        List<VirtualFile> result = new ArrayList<VirtualFile>(files.size());
        for (VirtualFile file : files) {
            if (!fileIndex.isExcluded(file) && !GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project)) {
                result.add(file);
            }
        }
        return result;
    }

    public static PsiFile[] getPsiFiles(final Project project, final Collection<VirtualFile> selectedFiles) {
        ArrayList<PsiFile> result = new ArrayList<PsiFile>();
        PsiManager psiManager = PsiManager.getInstance(project);

        VirtualFile projectFileDir = null;
        final StorageScheme storageScheme = ((ProjectEx) project).getStateStore().getStorageScheme();
        if (StorageScheme.DIRECTORY_BASED.equals(storageScheme)) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                projectFileDir = baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
            }
        }

        for (VirtualFile file : selectedFiles) {
            if (file.isValid()) {
                if (isUnderProjectFileDir(projectFileDir, file) || !isFileUnderSourceRoot(project, file)) {
                    continue;
                }
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile != null) result.add(psiFile);
            }
        }
        return PsiUtilCore.toPsiFileArray(result);
    }

    private static boolean isUnderProjectFileDir( VirtualFile projectFileDir,  VirtualFile file) {
        return projectFileDir != null && VfsUtilCore.isAncestor(projectFileDir, file, false);
    }

    private static boolean isFileUnderSourceRoot( Project project,  VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        if (StdFileTypes.JAVA == file.getFileType()) {
            return index.isUnderSourceRootOfType(file, JavaModuleSourceRootTypes.SOURCES) && !index.isInLibrarySource(file);
        }
        else {
            return index.isInContent(file) && !index.isInLibrarySource(file) ;
        }
    }

    static void disableWhenDumb( Project project,  JCheckBox checkBox,  String tooltip) {
        boolean dumb = DumbService.isDumb(project);
        checkBox.setEnabled(!dumb);
        checkBox.setToolTipText(dumb ? tooltip : "");
    }
}
