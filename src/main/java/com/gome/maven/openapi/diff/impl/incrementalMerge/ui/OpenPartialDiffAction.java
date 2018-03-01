/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.diff.impl.incrementalMerge.ui;

import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.DiffBundle;
import com.gome.maven.openapi.diff.DocumentContent;
import com.gome.maven.openapi.diff.SimpleDiffRequest;
import com.gome.maven.openapi.diff.impl.external.DiffManagerImpl;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;

class OpenPartialDiffAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.diff.impl.incrementalMerge.ui.OpenPartialDiffAction");
    private final int myLeftIndex;
    private final int myRightIndex;

    public OpenPartialDiffAction(int leftIndex, int rightIndex, Icon icon) {
        super("", null, icon);
        myLeftIndex = leftIndex;
        myRightIndex = rightIndex;
    }

    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        MergePanel2 mergePanel = MergePanel2.fromDataContext(dataContext);
        Project project = projectFromDataContext(dataContext);
        Editor leftEditor = mergePanel.getEditor(myLeftIndex);
        Editor rightEditor = mergePanel.getEditor(myRightIndex);
        FileType type = mergePanel.getContentType();
        SimpleDiffRequest diffData = new SimpleDiffRequest(project, composeName());
        diffData.setContents(new DocumentContent(project, leftEditor.getDocument(), type), new DocumentContent(project, rightEditor.getDocument(), type));
        diffData.setContentTitles(mergePanel.getVersionTitle(myLeftIndex), mergePanel.getVersionTitle(myRightIndex));
        LOG.assertTrue(DiffManagerImpl.INTERNAL_DIFF.canShow(diffData));
        DiffManagerImpl.INTERNAL_DIFF.show(diffData);
    }

    public void update(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        MergePanel2 mergePanel = MergePanel2.fromDataContext(dataContext);
        Project project = projectFromDataContext(dataContext);
        Presentation presentation = e.getPresentation();
        if (mergePanel == null || project == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        presentation.setVisible(true);
        Editor leftEditor = mergePanel.getEditor(myLeftIndex);
        Editor rightEditor = mergePanel.getEditor(myRightIndex);
        if (leftEditor == null || rightEditor == null) {
            presentation.setEnabled(false);
            return;
        }
        presentation.setText(composeName());
        presentation.setEnabled(true);
    }

    private String composeName() {
        if (myLeftIndex == 0 && myRightIndex == 1) {
            return DiffBundle.message("merge.partial.diff.action.name.0.1");
        }
        if (myLeftIndex == 1 && myRightIndex == 2) {
            return DiffBundle.message("merge.partial.diff.action.name.1.2");
        }

        return DiffBundle.message("merge.partial.diff.action.name");
    }


    private static Project projectFromDataContext(DataContext dataContext) {
        return CommonDataKeys.PROJECT.getData(dataContext);
    }
}
