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
package com.gome.maven.openapi.diff;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.LineSeparator;

import java.io.File;
import java.io.IOException;

public class FileContent extends DiffContent {
     private final VirtualFile myFile;
    private Document myDocument;
    private final Project myProject;
    private final FileType myType;

    public FileContent(Project project,  VirtualFile file) {
        myProject = project;
        myFile = file;
        myType = file.getFileType();
    }

    @Override
    public Document getDocument() {
        if (myDocument == null && DiffContentUtil.isTextFile(myFile)) {
            myDocument = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
                @Override
                public Document compute() {
                    return FileDocumentManager.getInstance().getDocument(myFile);
                }
            });
        }
        return myDocument;
    }

    @Override
    public OpenFileDescriptor getOpenFileDescriptor(int offset) {
        return new OpenFileDescriptor(myProject, myFile, offset);
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
        if (myFile.isDirectory()) return null;
        return myFile.contentsToByteArray();
    }

    @Override
    public boolean isBinary() {
        return !myFile.isDirectory() && myType.isBinary();
    }

    public static FileContent createFromTempFile(Project project, String name, String ext,  byte[] content) throws IOException {
        File tempFile = FileUtil.createTempFile(name, "." + ext);
        if (content.length != 0) {
            FileUtil.writeToFile(tempFile, content);
        }
        tempFile.deleteOnExit();
        final LocalFileSystem lfs = LocalFileSystem.getInstance();
        VirtualFile file = lfs.findFileByIoFile(tempFile);
        if (file == null) {
            file = lfs.refreshAndFindFileByIoFile(tempFile);
        }
        if (file != null) {
            return new FileContent(project, file);
        }
        throw new IOException("Can not create temp file for revision content");
    }

    
    @Override
    public LineSeparator getLineSeparator() {
        return LineSeparator.fromString(FileDocumentManager.getInstance().getLineSeparator(myFile, myProject));
    }

}
