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
package com.gome.maven.diff;

import com.gome.maven.openapi.diff.SimpleContent;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * A {@link SimpleContent} which content is retrieved from a file which exists or existed in the project.
 */
public class FileAwareSimpleContent extends SimpleContent {

     private final Project myProject;
     private final FilePath myFilePath;

    public FileAwareSimpleContent( Project project,  FilePath filePath,  String text,  FileType type) {
        super(text, type);
        myProject = project;
        myFilePath = filePath;
    }

    @Override
    public OpenFileDescriptor getOpenFileDescriptor(int offset) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(myFilePath.getIOFile());
        return file == null ? null : new OpenFileDescriptor(myProject, file, offset);
    }

}
