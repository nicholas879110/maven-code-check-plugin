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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public class FilePathChangesTreeList extends ChangesTreeList<FilePath> {

    public FilePathChangesTreeList( Project project,  List<FilePath> originalFiles,
                                   boolean showCheckboxes, boolean highlightProblems,
                                    Runnable inclusionListener,  ChangeNodeDecorator nodeDecorator) {
        super(project, originalFiles, showCheckboxes, highlightProblems, inclusionListener, nodeDecorator);
    }

    protected DefaultTreeModel buildTreeModel(final List<FilePath> changes, ChangeNodeDecorator changeNodeDecorator) {
        return new TreeModelBuilder(myProject, false).buildModelFromFilePaths(changes);
    }

    protected List<FilePath> getSelectedObjects(final ChangesBrowserNode<FilePath> node) {
        return node.getAllFilePathsUnder();
    }

    
    protected FilePath getLeadSelectedObject(final ChangesBrowserNode node) {
        Object userObject = node.getUserObject();
        return userObject instanceof FilePath ? (FilePath)userObject : null;
    }
}
