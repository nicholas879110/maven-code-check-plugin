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
package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangeList;
import com.gome.maven.openapi.vcs.ui.Refreshable;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.PlaceProvider;

import java.io.File;
import java.util.Collection;

public interface VcsContext extends PlaceProvider<String> {
     Project getProject();

    
    VirtualFile getSelectedFile();

    
    VirtualFile[] getSelectedFiles();

    Editor getEditor();

    Collection<VirtualFile> getSelectedFilesCollection();

    File[] getSelectedIOFiles();

    int getModifiers();

    Refreshable getRefreshableDialog();

    File getSelectedIOFile();

    
    FilePath[] getSelectedFilePaths();

    
    FilePath getSelectedFilePath();

    
    ChangeList[] getSelectedChangeLists();

    
    Change[] getSelectedChanges();

    String getActionName();
}
