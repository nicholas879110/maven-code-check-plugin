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
package com.gome.maven.openapi.fileChooser;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.List;

public interface FileSystemTree extends Disposable {
    DataKey<FileSystemTree> DATA_KEY = DataKey.create("FileSystemTree");

    JTree getTree();

    void updateTree();

    
    VirtualFile getSelectedFile();

    
    VirtualFile[] getSelectedFiles();

    
    VirtualFile getNewFileParent();

    
    <T> T getData(DataKey<T> key);

    void select(VirtualFile file,  Runnable onDone);
    void select(VirtualFile[] files,  Runnable onDone);

    void expand(VirtualFile file,  Runnable onDone);

    void addListener(Listener listener, Disposable parent);

    boolean isUnderRoots( VirtualFile file);

    boolean selectionExists();

    boolean areHiddensShown();

    void showHiddens(boolean showHidden);

    interface Listener {
        void selectionChanged(List<VirtualFile> selection);
    }
}
