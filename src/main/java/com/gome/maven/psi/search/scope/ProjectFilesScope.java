/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.psi.search.scope;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.search.scope.packageSet.AbstractPackageSet;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;
import com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder;

/**
 * @author Konstantin Bulenkov
 */
public class ProjectFilesScope extends NamedScope {
    public static final String NAME = "Project Files";
    public ProjectFilesScope() {
        super(NAME, new AbstractPackageSet("ProjectFiles") {
            @Override
            public boolean contains(VirtualFile file, NamedScopesHolder holder) {
                return contains(file, holder.getProject(), holder);
            }

            @Override
            public boolean contains(VirtualFile file,  Project project,  NamedScopesHolder holder) {
                if (file == null) return false;
                final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                return holder.getProject().isInitialized()
                        && !fileIndex.isExcluded(file)
                        && fileIndex.getContentRootForFile(file) != null;
            }
        });
    }
}
