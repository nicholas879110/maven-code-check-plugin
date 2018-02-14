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

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.undo.DocumentReference;
import com.gome.maven.openapi.command.undo.DocumentReferenceManager;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileAdapter;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.containers.WeakKeyWeakValueHashMap;
import com.gome.maven.util.containers.WeakValueHashMap;
import com.gome.maven.util.io.fs.FilePath;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentReferenceManagerImpl extends DocumentReferenceManager implements ApplicationComponent {
    private static final Key<List<VirtualFile>> DELETED_FILES = Key.create(DocumentReferenceManagerImpl.class.getName() + ".DELETED_FILES");

    private final Map<Document, DocumentReference> myDocToRef = new WeakKeyWeakValueHashMap<Document, DocumentReference>();

    private static final Key<Reference<DocumentReference>> FILE_TO_REF_KEY = Key.create("FILE_TO_REF_KEY");
    private static final Key<DocumentReference> FILE_TO_STRONG_REF_KEY = Key.create("FILE_TO_STRONG_REF_KEY");
    private final Map<FilePath, DocumentReference> myDeletedFilePathToRef = new WeakValueHashMap<FilePath, DocumentReference>();

    @Override
    
    public String getComponentName() {
        return getClass().getSimpleName();
    }

    @Override
    public void initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void fileCreated( VirtualFileEvent event) {
                VirtualFile f = event.getFile();
                DocumentReference ref = myDeletedFilePathToRef.remove(new FilePath(f.getUrl()));
                if (ref != null) {
                    f.putUserData(FILE_TO_REF_KEY, new WeakReference<DocumentReference>(ref));
                    ((DocumentReferenceByVirtualFile)ref).update(f);
                }
            }

            @Override
            public void beforeFileDeletion( VirtualFileEvent event) {
                VirtualFile f = event.getFile();
                f.putUserData(DELETED_FILES, collectDeletedFiles(f, new ArrayList<VirtualFile>()));
            }

            @Override
            public void fileDeleted( VirtualFileEvent event) {
                VirtualFile f = event.getFile();
                List<VirtualFile> files = f.getUserData(DELETED_FILES);
                f.putUserData(DELETED_FILES, null);

                assert files != null : f;
                for (VirtualFile each : files) {
                    DocumentReference ref = SoftReference.dereference(each.getUserData(FILE_TO_REF_KEY));
                    each.putUserData(FILE_TO_REF_KEY, null);
                    if (ref != null) {
                        myDeletedFilePathToRef.put(new FilePath(each.getUrl()), ref);
                    }
                }
            }
        });
    }

    private static List<VirtualFile> collectDeletedFiles(VirtualFile f, List<VirtualFile> files) {
        if (!(f instanceof NewVirtualFile)) return files;

        if (!f.isDirectory()) {
            files.add(f);
        }
        else {
            for (VirtualFile each : ((NewVirtualFile)f).iterInDbChildren()) {
                collectDeletedFiles(each, files);
            }
        }
        return files;
    }

    @Override
    public void disposeComponent() {
    }

    
    @Override
    public DocumentReference create( Document document) {
        assertInDispatchThread();

        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file == null ? createFromDocument(document) : create(file);
    }

    
    private DocumentReference createFromDocument( final Document document) {
        DocumentReference result = myDocToRef.get(document);
        if (result == null) {
            result = new DocumentReferenceByDocument(document);
            myDocToRef.put(document, result);
        }
        return result;
    }

    
    @Override
    public DocumentReference create( VirtualFile file) {
        assertInDispatchThread();

        if (!file.isInLocalFileSystem()) { // we treat local files differently from non local because we can undo their deletion
            DocumentReference reference = file.getUserData(FILE_TO_STRONG_REF_KEY);
            if (reference == null) {
                file.putUserData(FILE_TO_STRONG_REF_KEY, reference = new DocumentReferenceByNonlocalVirtualFile(file));
            }
            return reference;
        }

        assert file.isValid() : "file is invalid: " + file;

        DocumentReference result = SoftReference.dereference(file.getUserData(FILE_TO_REF_KEY));
        if (result == null) {
            result = new DocumentReferenceByVirtualFile(file);
            file.putUserData(FILE_TO_REF_KEY, new WeakReference<DocumentReference>(result));
        }
        return result;
    }

    private static void assertInDispatchThread() {
        ApplicationManager.getApplication().assertIsDispatchThread();
    }
}