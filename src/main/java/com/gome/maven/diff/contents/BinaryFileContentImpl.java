/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff.contents;

import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.io.IOException;

/**
 * Allows to compare binary files
 */
public class BinaryFileContentImpl implements DiffContent, BinaryFileContent {
     private final VirtualFile myFile;
     private final Project myProject;
     private final FileType myType;

    public BinaryFileContentImpl( Project project,  VirtualFile file) {
        assert file.isValid() && !file.isDirectory();
        myProject = project;
        myFile = file;
        myType = file.getFileType();
    }

    
    @Override
    public OpenFileDescriptor getOpenFileDescriptor() {
        if (myProject == null || myProject.isDefault()) return null;
        return new OpenFileDescriptor(myProject, myFile);
    }

    
    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    
    @Override
    public FileType getContentType() {
        return myType;
    }

    
    @Override
    public byte[] getBytes() throws IOException {
        return myFile.contentsToByteArray();
    }

    
    public String getFilePath() {
        return myFile.getPath();
    }

    @Override
    public void onAssigned(boolean isAssigned) {
    }
}
