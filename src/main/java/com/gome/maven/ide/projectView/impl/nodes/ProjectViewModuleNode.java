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
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleFileIndex;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectViewModuleNode extends AbstractModuleNode {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.projectView.impl.nodes.ProjectViewModuleNode");

    public ProjectViewModuleNode(Project project, Module value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public ProjectViewModuleNode(Project project, Object value, ViewSettings viewSettings) {
        this(project, (Module)value, viewSettings);
    }

    @Override

    public Collection<AbstractTreeNode> getChildren() {
        Module module = getValue();
        if (module == null) {  // module has been disposed
            return Collections.emptyList();
        }
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        ModuleFileIndex moduleFileIndex = rootManager.getFileIndex();

        final VirtualFile[] contentRoots = rootManager.getContentRoots();
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>(contentRoots.length + 1);
        final PsiManager psiManager = PsiManager.getInstance(getProject());
        for (final VirtualFile contentRoot : contentRoots) {
            if (!moduleFileIndex.isInContent(contentRoot)) continue;

            AbstractTreeNode child;
            if (contentRoot.isDirectory()) {
                PsiDirectory directory = psiManager.findDirectory(contentRoot);
                LOG.assertTrue(directory != null);
                child = new PsiDirectoryNode(getProject(), directory, getSettings());
            }
            else {
                PsiFile file = psiManager.findFile(contentRoot);
                LOG.assertTrue(file != null);
                child = new PsiFileNode(getProject(), file, getSettings());
            }
            children.add(child);
        }

    /*
    if (getSettings().isShowLibraryContents()) {
      children.add(new LibraryGroupNode(getProject(), new LibraryGroupElement(getValue()), getSettings()));
    }
    */
        return children;
    }

    @Override
    public int getWeight() {
        return 10;
    }

    @Override
    public int getTypeSortWeight(final boolean sortByType) {
        return 2;
    }
}
