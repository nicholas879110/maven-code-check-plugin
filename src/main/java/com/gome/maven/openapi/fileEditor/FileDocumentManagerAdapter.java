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
package com.gome.maven.openapi.fileEditor;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.vfs.VirtualFile;

public abstract class FileDocumentManagerAdapter implements FileDocumentManagerListener {
    @Override
    public void beforeAllDocumentsSaving() {
    }

    @Override
    public void beforeDocumentSaving( Document document) {
    }

    @Override
    public void fileWithNoDocumentChanged( VirtualFile file) {
    }

    @Override
    public void beforeFileContentReload(VirtualFile file,  Document document) {
    }

    @Override
    public void fileContentReloaded( VirtualFile file,  Document document) {
    }

    @Override
    public void fileContentLoaded( VirtualFile file,  Document document) {
    }

    @Override
    public void unsavedDocumentsDropped() {
    }
}
