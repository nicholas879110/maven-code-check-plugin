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

package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsShowConfirmationOption;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class SelectFilePathsDialog extends AbstractSelectFilesDialog<FilePath> {

    private final ChangesTreeList<FilePath> myFileList;

    public SelectFilePathsDialog(final Project project, List<FilePath> originalFiles, final String prompt,
                                 final VcsShowConfirmationOption confirmationOption,
                                  String okActionName,  String cancelActionName, boolean showDoNotAskOption) {
        super(project, false, confirmationOption, prompt, showDoNotAskOption);
        myFileList = new FilePathChangesTreeList(project, originalFiles, true, true, null, null);
        if (okActionName != null) {
            getOKAction().putValue(Action.NAME, okActionName);
        }
        if (cancelActionName != null) {
            getCancelAction().putValue(Action.NAME, cancelActionName);
        }
        myFileList.setChangesToDisplay(originalFiles);
        init();
    }

    public Collection<FilePath> getSelectedFiles() {
        return myFileList.getIncludedChanges();
    }

    
    @Override
    protected ChangesTreeList getFileList() {
        return myFileList;
    }
}
