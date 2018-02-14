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

import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

public interface FileChooserDialog {
    DataKey<Boolean> PREFER_LAST_OVER_TO_SELECT = PathChooserDialog.PREFER_LAST_OVER_EXPLICIT;

    /**
     * @deprecated Please use {@link #choose(com.gome.maven.openapi.project.Project, com.gome.maven.openapi.vfs.VirtualFile...)} because
     * it supports several selections
     */
    @Deprecated
    
    VirtualFile[] choose( VirtualFile toSelect,  Project project);

    /**
     * Choose one or more files
     *
     * @param project  use this project (you may pass null if you already set project in ctor)
     * @param toSelect files to be selected automatically.
     * @return files chosen by user
     */
    
    VirtualFile[] choose( Project project,  VirtualFile... toSelect);
}