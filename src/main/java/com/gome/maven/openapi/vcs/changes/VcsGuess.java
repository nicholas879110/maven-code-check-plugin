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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.ProjectLevelVcsManager;
import com.gome.maven.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.gome.maven.openapi.vfs.VirtualFile;

public class VcsGuess {

     private final Project myProject;
    private final ProjectLevelVcsManagerImpl myVcsManager;
    private final FileIndexFacade myExcludedFileIndex;

    public VcsGuess( Project project) {
        myProject = project;
        myVcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(myProject);
        myExcludedFileIndex = PeriodicalTasksCloser.getInstance().safeGetService(myProject, FileIndexFacade.class);
    }

    
    public AbstractVcs getVcsForDirty( final VirtualFile file) {
        if (!file.isInLocalFileSystem()) {
            return null;
        }
        if (isFileInIndex(null, file)) {
            return myVcsManager.getVcsFor(file);
        }
        return null;
    }

    
    public AbstractVcs getVcsForDirty( final FilePath filePath) {
        if (filePath.isNonLocal()) {
            return null;
        }
        final VirtualFile validParent = ChangesUtil.findValidParentAccurately(filePath);
        if (validParent == null) {
            return null;
        }
        final boolean take = isFileInIndex(filePath, validParent);
        if (take) {
            return myVcsManager.getVcsFor(validParent);
        }
        return null;
    }

    private Boolean isFileInIndex( final FilePath filePath, final VirtualFile validParent) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            public Boolean compute() {
                if (myProject.isDisposed()) return false;
                final boolean inContent = myVcsManager.isFileInContent(validParent);
                if (inContent) return true;
                if (filePath != null) {
                    return isFileInBaseDir(filePath, myProject.getBaseDir()) && !myVcsManager.isIgnored(validParent);
                }
                return false;
            }
        });
    }

    private static boolean isFileInBaseDir(final FilePath filePath, final VirtualFile baseDir) {
        final VirtualFile parent = filePath.getVirtualFileParent();
        return !filePath.isDirectory() && parent != null && parent.equals(baseDir);
    }
}
