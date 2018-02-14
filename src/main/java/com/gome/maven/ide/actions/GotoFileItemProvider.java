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
package com.gome.maven.ide.actions;

import com.gome.maven.ide.util.gotoByName.ChooseByNameBase;
import com.gome.maven.ide.util.gotoByName.DefaultChooseByNameItemProvider;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.Processor;

/**
 * @author peter
 */
public class GotoFileItemProvider extends DefaultChooseByNameItemProvider {
    private final Project myProject;

    public GotoFileItemProvider( Project project,  PsiElement context) {
        super(context);
        myProject = project;
    }

    @Override
    public boolean filterElements( ChooseByNameBase base,
                                   String pattern,
                                  boolean everywhere,
                                   ProgressIndicator indicator,
                                   Processor<Object> consumer) {
        if (pattern.contains("/") || pattern.contains("\\")) {
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByPathIfCached(FileUtil.toSystemIndependentName(pattern));
            if (vFile != null) {
                ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(myProject);
                if (index.isInContent(vFile) || index.isInLibraryClasses(vFile) || index.isInLibrarySource(vFile)) {
                    PsiFileSystemItem fileOrDir = vFile.isDirectory() ?
                            PsiManager.getInstance(myProject).findDirectory(vFile) :
                            PsiManager.getInstance(myProject).findFile(vFile);
                    if (fileOrDir != null && !consumer.process(fileOrDir)) {
                        return false;
                    }
                }
            }
        }

        return super.filterElements(base, pattern, everywhere, indicator, consumer);
    }
}
