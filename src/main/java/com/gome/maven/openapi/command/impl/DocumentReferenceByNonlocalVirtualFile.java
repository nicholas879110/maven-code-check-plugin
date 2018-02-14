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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.command.undo.DocumentReference;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.vfs.VirtualFile;

class DocumentReferenceByNonlocalVirtualFile implements DocumentReference {
    private final VirtualFile myFile;

    DocumentReferenceByNonlocalVirtualFile( VirtualFile file) {
        myFile = file;
    }

    @Override
    
    public Document getDocument() {
        return FileDocumentManager.getInstance().getDocument(myFile);
    }

    @Override
    
    public VirtualFile getFile() {
        return myFile;
    }

    @Override
    public String toString() {
        return myFile.toString();
    }
}
