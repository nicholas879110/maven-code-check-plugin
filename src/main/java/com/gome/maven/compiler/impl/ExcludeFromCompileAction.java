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
package com.gome.maven.compiler.impl;

import com.gome.maven.compiler.CompilerConfiguration;
import com.gome.maven.ide.errorTreeView.ErrorTreeElement;
import com.gome.maven.ide.errorTreeView.ErrorTreeNodeDescriptor;
import com.gome.maven.ide.errorTreeView.GroupingElement;
import com.gome.maven.ide.errorTreeView.NewErrorTreeViewPanel;
import com.gome.maven.ide.util.treeView.NodeDescriptor;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.compiler.CompilerBundle;
import com.gome.maven.openapi.compiler.options.ExcludeEntryDescription;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author Eugene Zhuravlev
 *         Date: 9/12/12
 */
class ExcludeFromCompileAction extends AnAction {
    private final Project myProject;
    private final NewErrorTreeViewPanel myErrorTreeView;

    public ExcludeFromCompileAction(Project project, NewErrorTreeViewPanel errorTreeView) {
        super(CompilerBundle.message("actions.exclude.from.compile.text"));
        myProject = project;
        myErrorTreeView = errorTreeView;
    }

    public void actionPerformed(AnActionEvent e) {
        VirtualFile file = getSelectedFile();

        if (file != null && file.isValid()) {
            ExcludeEntryDescription description = new ExcludeEntryDescription(file, false, true, myProject);
            CompilerConfiguration.getInstance(myProject).getExcludedEntriesConfiguration().addExcludeEntryDescription(description);
            FileStatusManager.getInstance(myProject).fileStatusesChanged();
        }
    }


    private VirtualFile getSelectedFile() {
        final ErrorTreeNodeDescriptor descriptor = myErrorTreeView.getSelectedNodeDescriptor();
        ErrorTreeElement element = descriptor != null? descriptor.getElement() : null;
        if (element != null && !(element instanceof GroupingElement)) {
            NodeDescriptor parent = descriptor.getParentDescriptor();
            if (parent instanceof ErrorTreeNodeDescriptor) {
                element = ((ErrorTreeNodeDescriptor)parent).getElement();
            }
        }
        return element instanceof GroupingElement? ((GroupingElement)element).getFile() : null;
    }

    public void update(AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final boolean isApplicable = getSelectedFile() != null;
        presentation.setEnabled(isApplicable);
        presentation.setVisible(isApplicable);
    }
}
