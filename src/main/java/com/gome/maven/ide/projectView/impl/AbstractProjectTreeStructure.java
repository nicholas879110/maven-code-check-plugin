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

package com.gome.maven.ide.projectView.impl;

import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.psi.PsiDocumentManager;

public abstract class AbstractProjectTreeStructure extends ProjectAbstractTreeStructureBase implements ViewSettings {
    private final AbstractTreeNode myRoot;

    public AbstractProjectTreeStructure(Project project) {
        super(project);
        myRoot = createRoot(project, this);
    }

    protected AbstractTreeNode createRoot(final Project project, ViewSettings settings) {
        return new ProjectViewProjectNode(myProject, this);
    }

    @Override
    public abstract boolean isShowMembers();

    @Override
    public final Object getRootElement() {
        return myRoot;
    }

    @Override
    public final void commit() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    }


    @Override
    public ActionCallback asyncCommit() {
        return asyncCommitDocuments(myProject);
    }

    @Override
    public final boolean hasSomethingToCommit() {
        return !myProject.isDisposed()
                && PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
    }

    @Override
    public boolean isStructureView() {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(Object element) {
        if (element instanceof ProjectViewNode) {
            return ((ProjectViewNode)element).isAlwaysLeaf();
        }
        return super.isAlwaysLeaf(element);
    }
}