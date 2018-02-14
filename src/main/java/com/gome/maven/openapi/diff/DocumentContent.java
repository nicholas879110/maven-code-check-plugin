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
package com.gome.maven.openapi.diff;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.LineSeparator;

public class DocumentContent extends DiffContent {
    private final Document myDocument;
    private final VirtualFile myFile;
    private final FileType myOverriddenType;
    private final Project myProject;
    private final FileDocumentManager myDocumentManager;

    public DocumentContent(Project project, Document document) {
        this(project, document, null);
    }

    public DocumentContent(Project project,  Document document, FileType type) {
        myProject = project;
        myDocument = document;
        myDocumentManager = FileDocumentManager.getInstance();
        myFile = myDocumentManager.getFile(document);
        myOverriddenType = type;
    }

    public DocumentContent( Document document) {
        this(null, document, null);
    }

    public DocumentContent( Document document,  FileType type) {
        this(null, document, type);
    }

    @Override
    
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public OpenFileDescriptor getOpenFileDescriptor(int offset) {
        VirtualFile file = getFile();
        if (file == null) return null;
        if (myProject == null) return null;
        return new OpenFileDescriptor(myProject, file, offset);
    }

    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    @Override
    
    public FileType getContentType() {
        return myOverriddenType == null ? DiffContentUtil.getContentType(getFile()) : myOverriddenType;
    }

    @Override
    public byte[] getBytes() {
        return myDocument.getText().getBytes();
    }

    
    @Override
    public LineSeparator getLineSeparator() {
        return LineSeparator.fromString(myDocumentManager.getLineSeparator(myFile, myProject));
    }
}
