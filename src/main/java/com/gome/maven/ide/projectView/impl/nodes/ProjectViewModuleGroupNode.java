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

package com.gome.maven.ide.projectView.impl.nodes;

import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.projectView.impl.ModuleGroup;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiManager;

import java.lang.reflect.InvocationTargetException;

/**
 * User: anna
 * Date: Feb 22, 2005
 */
public class ProjectViewModuleGroupNode extends ModuleGroupNode {
    public ProjectViewModuleGroupNode(final Project project, final Object value, final ViewSettings viewSettings) {
        super(project, (ModuleGroup)value, viewSettings);
    }

    public ProjectViewModuleGroupNode(final Project project, final ModuleGroup value, final ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    protected AbstractTreeNode createModuleNode(Module module)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        if (roots.length == 1) {
            final PsiDirectory psi = PsiManager.getInstance(myProject).findDirectory(roots[0]);
            if (psi != null) {
                return new PsiDirectoryNode(myProject, psi, getSettings());
            }
        }

        return new ProjectViewModuleNode(getProject(), module, getSettings());
    }

    @Override
    protected ModuleGroupNode createModuleGroupNode(ModuleGroup moduleGroup) {
        return new ProjectViewModuleGroupNode(getProject(), moduleGroup, getSettings());
    }


}
