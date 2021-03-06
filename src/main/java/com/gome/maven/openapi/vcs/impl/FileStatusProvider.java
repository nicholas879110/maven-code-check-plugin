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
package com.gome.maven.openapi.vcs.impl;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ThreeState;

/**
 * @author yole
 */
public interface FileStatusProvider {

    ExtensionPointName<FileStatusProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.vcs.fileStatusProvider");

    FileStatus getFileStatus( VirtualFile virtualFile);
    void refreshFileStatusFromDocument( VirtualFile virtualFile,  Document doc);

    
    ThreeState getNotChangedDirectoryParentingStatus( VirtualFile virtualFile);
}