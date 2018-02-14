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
package com.gome.maven.openapi.fileEditor;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vfs.SavingRequestor;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * Tracks the correspondence between {@link VirtualFile} instances and corresponding {@link Document} instances.
 * Manages the saving of changes to disk.
 */
public abstract class FileDocumentManager implements SavingRequestor {
    
    public static FileDocumentManager getInstance() {
        return ApplicationManager.getApplication().getComponent(FileDocumentManager.class);
    }

    /**
     * Returns the document for the specified virtual file.<p/>
     *
     * Documents are cached on weak or strong references, depending on the nature of the virtual file. If the document for the given virtual file is not yet cached,
     * the file's contents are read from VFS and loaded into heap memory. An appropriate encoding is used. All line separators are converted to <code>\n</code>.<p/>
     *
     * Should be invoked in a read action.
     *
     * @param file the file for which the document is requested.
     * @return the document, or null if the file represents a directory, or is binary without an associated decompiler,
     * or is too large.
     * @see VirtualFile#contentsToByteArray()
     * @see Application#runReadAction(Computable)
     */
    
    public abstract Document getDocument( VirtualFile file);

    /**
     * Returns the document for the specified file which has already been loaded into memory.<p/>
     *
     * Client code shouldn't normally use this method, because it's unpredictable and any garbage collection can result in it returning null.
     *
     * @param file the file for which the document is requested.
     * @return the document, or null if the specified virtual file hasn't been loaded into memory.
     */
    
    public abstract Document getCachedDocument( VirtualFile file);

    /**
     * Returns the virtual file corresponding to the specified document.
     *
     * @param document the document for which the virtual file is requested.
     * @return the file, or null if the document wasn't created from a virtual file.
     */
    
    public abstract VirtualFile getFile( Document document);

    /**
     * Saves all unsaved documents to disk. This operation can modify documents that will be saved
     * (due to 'Strip trailing spaces on Save' functionality). When saving, <code>\n</code> line separators are converted into
     * the ones used normally on the system, or the ones explicitly specified by the user. Encoding settings are honored.<p/>
     *
     * Should be invoked in a write action.
     */
    public abstract void saveAllDocuments();

    /**
     * Saves the specified document to disk. This operation can modify the document (due to 'Strip
     * trailing spaces on Save' functionality). When saving, <code>\n</code> line separators are converted into
     * the ones used normally on the system, or the ones explicitly specified by the user. Encoding settings are honored.<p/>
     *
     * Should be invoked in a write action.
     * @param document the document to save.
     */
    public abstract void saveDocument( Document document);

    /**
     * Saves the document without stripping the trailing spaces or adding a blank line in the end of the file.<p/>
     *
     * Should be invoked in a write action.
     *
     * @param document the document to save.
     */
    public abstract void saveDocumentAsIs( Document document);

    /**
     * Returns all documents that have unsaved changes.
     * @return the documents that have unsaved changes.
     */
    
    public abstract Document[] getUnsavedDocuments();

    /**
     * Checks if the document has unsaved changes.
     *
     * @param document the document to check.
     * @return true if the document has unsaved changes, false otherwise.
     */
    public abstract boolean isDocumentUnsaved( Document document);

    /**
     * Checks if the document corresponding to the specified file has unsaved changes.
     *
     * @param file the file to check.
     * @return true if the file has unsaved changes, false otherwise.
     */
    public abstract boolean isFileModified( VirtualFile file);

    /**
     * Discards unsaved changes for the specified document and reloads it from disk.
     *
     * @param document the document to reload.
     */
    public abstract void reloadFromDisk( Document document);

    
    public abstract String getLineSeparator( VirtualFile file,  Project project);

    /**
     * Requests writing access on given document, possibly involving interaction with user.
     *
     * @param document document
     * @param project project
     * @return true if writing access allowed
     * @see com.gome.maven.openapi.vfs.ReadonlyStatusHandler#ensureFilesWritable(com.gome.maven.openapi.project.Project, com.gome.maven.openapi.vfs.VirtualFile...)
     */
    public abstract boolean requestWriting( Document document,  Project project);

    public static boolean fileForDocumentCheckedOutSuccessfully( Document document,  Project project) {
        return getInstance().requestWriting(document, project);
    }

    /**
     * Discards unsaved changes for the specified files.
     *
     * @param files the files to discard the changes for.
     */
    public abstract void reloadFiles( VirtualFile... files);
}
