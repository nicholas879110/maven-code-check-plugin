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

package com.gome.maven.ide.impl;

import com.gome.maven.ide.SelectInContext;
import com.gome.maven.ide.SelectInManager;
import com.gome.maven.ide.StandardTargetWeights;
import com.gome.maven.ide.projectView.impl.ProjectViewPane;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFileSystemItem;

public class ProjectPaneSelectInTarget extends ProjectViewSelectInTarget implements DumbAware {
    public ProjectPaneSelectInTarget(Project project) {
        super(project);
    }

    public String toString() {
        return SelectInManager.PROJECT;
    }

    @Override
    public boolean canSelect(PsiFileSystemItem file) {
        if (!super.canSelect(file)) return false;
        final VirtualFile vFile = file.getVirtualFile();
        return canSelect(vFile);
    }

    @Override
    public boolean isSubIdSelectable(String subId, SelectInContext context) {
        return canSelect(context);
    }

    private boolean canSelect(final VirtualFile vFile) {
        if (vFile != null && vFile.isValid()) {
            ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
            if (projectFileIndex.getModuleForFile(vFile) != null) {
                return true;
            }

            if (projectFileIndex.isInLibraryClasses(vFile) || projectFileIndex.isInLibrarySource(vFile)) {
                return true;
            }

            return Comparing.equal(vFile.getParent(), myProject.getBaseDir());
        }

        return false;
    }

    @Override
    public String getMinorViewId() {
        return ProjectViewPane.ID;
    }

    @Override
    public float getWeight() {
        return StandardTargetWeights.PROJECT_WEIGHT;
    }

    @Override
    protected boolean canWorkWithCustomObjects() {
        return false;
    }
}
