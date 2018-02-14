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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.command.undo.DocumentReference;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.vfs.VirtualFile;

public class DocumentReferenceByVirtualFile implements DocumentReference {
    private VirtualFile myFile;

    DocumentReferenceByVirtualFile( VirtualFile file) {
        myFile = file;
    }

    @Override
    
    public Document getDocument() {
        assert myFile.isValid() : "should not be called on references to deleted file: " + myFile;
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

    public void update(VirtualFile f) {
        myFile = f;
    }
}
