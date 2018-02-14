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
package com.gome.maven.history.integration;

import com.gome.maven.history.core.LocalHistoryFacade;
import com.gome.maven.history.core.Paths;
import com.gome.maven.history.core.StoredContent;
import com.gome.maven.history.core.tree.DirectoryEntry;
import com.gome.maven.history.core.tree.Entry;
import com.gome.maven.history.core.tree.FileEntry;
import com.gome.maven.history.core.tree.RootEntry;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.Clock;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.encoding.EncodingRegistry;
import com.gome.maven.openapi.vfs.newvfs.ManagingFS;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.containers.ContainerUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IdeaGateway {
    private static final Key<ContentAndTimestamps> SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY
            = Key.create("LocalHistory.SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY");

    public boolean isVersioned( VirtualFile f) {
        return isVersioned(f, false);
    }

    public boolean isVersioned( VirtualFile f, boolean shouldBeInContent) {
        if (!f.isInLocalFileSystem()) return false;

        if (!f.isDirectory() && StringUtil.endsWith(f.getNameSequence(), ".class")) return false;

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        boolean isInContent = false;
        for (Project each : openProjects) {
            if (each.isDefault()) continue;
            if (!each.isInitialized()) continue;
            if (Comparing.equal(each.getWorkspaceFile(), f)) return false;
            ProjectFileIndex index = ProjectRootManager.getInstance(each).getFileIndex();

            if (index.isExcluded(f)) return false;
            isInContent |= index.isInContent(f);
        }
        if (shouldBeInContent && !isInContent) return false;

        // optimisation: FileTypeManager.isFileIgnored(f) already checked inside ProjectFileIndex.isIgnored()
        return openProjects.length != 0 || !FileTypeManager.getInstance().isFileIgnored(f);
    }

    public boolean areContentChangesVersioned( VirtualFile f) {
        return isVersioned(f) && !f.isDirectory() && !f.getFileType().isBinary();
    }

    public boolean areContentChangesVersioned( String fileName) {
        return !FileTypeManager.getInstance().getFileTypeByFileName(fileName).isBinary();
    }

    public boolean ensureFilesAreWritable( Project p,  List<VirtualFile> ff) {
        ReadonlyStatusHandler h = ReadonlyStatusHandler.getInstance(p);
        return !h.ensureFilesWritable(VfsUtilCore.toVirtualFileArray(ff)).hasReadonlyFiles();
    }

    
    public VirtualFile findVirtualFile( String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    
    public VirtualFile findOrCreateFileSafely( VirtualFile parent,  String name, boolean isDirectory) throws IOException {
        VirtualFile f = parent.findChild(name);
        if (f != null && f.isDirectory() != isDirectory) {
            f.delete(this);
            f = null;
        }
        if (f == null) {
            f = isDirectory
                    ? parent.createChildDirectory(this, name)
                    : parent.createChildData(this, name);
        }
        return f;
    }

    
    public VirtualFile findOrCreateFileSafely( String path, boolean isDirectory) throws IOException {
        VirtualFile f = findVirtualFile(path);
        if (f != null && f.isDirectory() != isDirectory) {
            f.delete(this);
            f = null;
        }
        if (f == null) {
            VirtualFile parent = findOrCreateFileSafely(Paths.getParentOf(path), true);
            String name = Paths.getNameOf(path);
            f = isDirectory
                    ? parent.createChildDirectory(this, name)
                    : parent.createChildData(this, name);
        }
        return f;
    }

    public List<VirtualFile> getAllFilesFrom( String path) {
        VirtualFile f = findVirtualFile(path);
        if (f == null) return Collections.emptyList();
        return collectFiles(f, new ArrayList<VirtualFile>());
    }

    
    private static List<VirtualFile> collectFiles( VirtualFile f,  List<VirtualFile> result) {
        if (f.isDirectory()) {
            for (VirtualFile child : iterateDBChildren(f)) {
                collectFiles(child, result);
            }
        }
        else {
            result.add(f);
        }
        return result;
    }

    
    public static Iterable<VirtualFile> iterateDBChildren(VirtualFile f) {
        if (!(f instanceof NewVirtualFile)) return Collections.emptyList();
        NewVirtualFile nf = (NewVirtualFile)f;
        return nf.iterInDbChildren();
    }

    
    public static Iterable<VirtualFile> loadAndIterateChildren(VirtualFile f) {
        if (!(f instanceof NewVirtualFile)) return Collections.emptyList();
        NewVirtualFile nf = (NewVirtualFile)f;
        return Arrays.asList(nf.getChildren());
    }

    
    public RootEntry createTransientRootEntry() {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        RootEntry root = new RootEntry();
        doCreateChildren(root, getLocalRoots(), false);
        return root;
    }

    
    public RootEntry createTransientRootEntryForPathOnly( String path) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        RootEntry root = new RootEntry();
        doCreateChildrenForPathOnly(root, path, getLocalRoots());
        return root;
    }

    private static List<VirtualFile> getLocalRoots() {
        return Arrays.asList(ManagingFS.getInstance().getLocalRoots());
    }

    private void doCreateChildrenForPathOnly( DirectoryEntry parent,
                                              String path,
                                              Iterable<VirtualFile> children) {
        for (VirtualFile child : children) {
            String name = StringUtil.trimStart(child.getName(), "/"); // on Mac FS root name is "/"
            if (!path.startsWith(name)) continue;
            String rest = path.substring(name.length());
            if (!rest.isEmpty() && rest.charAt(0) != '/') continue;
            if (!rest.isEmpty() && rest.charAt(0) == '/') {
                rest = rest.substring(1);
            }
            Entry e = doCreateEntryForPathOnly(child, rest);
            if (e == null) continue;
            parent.addChild(e);
        }
    }

    
    private Entry doCreateEntryForPathOnly( VirtualFile file,  String path) {
        if (!file.isDirectory()) {
            if (!isVersioned(file)) return null;

            Pair<StoredContent, Long> contentAndStamps = getActualContentNoAcquire(file);
            return new FileEntry(file.getName(), contentAndStamps.first, contentAndStamps.second, !file.isWritable());
        }
        DirectoryEntry newDir = new DirectoryEntry(file.getName());
        doCreateChildrenForPathOnly(newDir, path, iterateDBChildren(file));
        if (!isVersioned(file) && newDir.getChildren().isEmpty()) return null;
        return newDir;
    }

    
    public Entry createTransientEntry( VirtualFile file) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        return doCreateEntry(file, false);
    }

    
    public Entry createEntryForDeletion( VirtualFile file) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        return doCreateEntry(file, true);
    }

    
    private Entry doCreateEntry( VirtualFile file, boolean forDeletion) {
        if (!file.isDirectory()) {
            if (!isVersioned(file)) return null;

            Pair<StoredContent, Long> contentAndStamps;
            if (forDeletion) {
                FileDocumentManager m = FileDocumentManager.getInstance();
                Document d = m.isFileModified(file) ? m.getCachedDocument(file) : null; // should not try to load document
                contentAndStamps = acquireAndClearCurrentContent(file, d);
            }
            else {
                contentAndStamps = getActualContentNoAcquire(file);
            }
            return new FileEntry(file.getName(), contentAndStamps.first, contentAndStamps.second, !file.isWritable());
        }
        DirectoryEntry newDir = new DirectoryEntry(file.getName());
        doCreateChildren(newDir, iterateDBChildren(file), forDeletion);
        if (!isVersioned(file) && newDir.getChildren().isEmpty()) return null;
        return newDir;
    }

    private void doCreateChildren( DirectoryEntry parent, Iterable<VirtualFile> children, final boolean forDeletion) {
        List<Entry> entries = ContainerUtil.mapNotNull(children, new NullableFunction<VirtualFile, Entry>() {
            @Override
            public Entry fun( VirtualFile each) {
                return doCreateEntry(each, forDeletion);
            }
        });
        parent.addChildren(entries);
    }

    public void registerUnsavedDocuments( final LocalHistoryFacade vcs) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                vcs.beginChangeSet();
                for (Document d : FileDocumentManager.getInstance().getUnsavedDocuments()) {
                    VirtualFile f = getFile(d);
                    if (!shouldRegisterDocument(f)) continue;
                    registerDocumentContents(vcs, f, d);
                }
                vcs.endChangeSet(null);
            }
        });
    }

    private boolean shouldRegisterDocument( VirtualFile f) {
        return f != null && f.isValid() && areContentChangesVersioned(f);
    }

    private void registerDocumentContents( LocalHistoryFacade vcs,  VirtualFile f, Document d) {
        Pair<StoredContent, Long> contentAndStamp = acquireAndUpdateActualContent(f, d);
        if (contentAndStamp != null) {
            vcs.contentChanged(f.getPath(), contentAndStamp.first, contentAndStamp.second);
        }
    }

    // returns null is content has not been changes since last time
    
    public Pair<StoredContent, Long> acquireAndUpdateActualContent( VirtualFile f,  Document d) {
        ContentAndTimestamps contentAndStamp = f.getUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY);
        if (contentAndStamp == null) {
            if (d != null) saveDocumentContent(f, d);
            return Pair.create(StoredContent.acquireContent(f), f.getTimeStamp());
        }

        // if no need to save current document content when simply return and clear stored one
        if (d == null) {
            f.putUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY, null);
            return Pair.create(contentAndStamp.content, contentAndStamp.registeredTimestamp);
        }

        // if the stored content equals the current one, do not store it and return null
        if (d.getModificationStamp() == contentAndStamp.documentModificationStamp) return null;

        // is current content has been changed, store it and return the previous one
        saveDocumentContent(f, d);
        return Pair.create(contentAndStamp.content, contentAndStamp.registeredTimestamp);
    }

    private static void saveDocumentContent( VirtualFile f,  Document d) {
        f.putUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY,
                new ContentAndTimestamps(Clock.getTime(),
                        StoredContent.acquireContent(bytesFromDocument(d)),
                        d.getModificationStamp()));
    }

    
    public Pair<StoredContent, Long> acquireAndClearCurrentContent( VirtualFile f,  Document d) {
        ContentAndTimestamps contentAndStamp = f.getUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY);
        f.putUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY, null);

        if (d != null && contentAndStamp != null) {
            // if previously stored content was not changed, return it
            if (d.getModificationStamp() == contentAndStamp.documentModificationStamp) {
                return Pair.create(contentAndStamp.content, contentAndStamp.registeredTimestamp);
            }
        }

        // release previously stored
        if (contentAndStamp != null) {
            contentAndStamp.content.release();
        }

        // take document's content if any
        if (d != null) {
            return Pair.create(StoredContent.acquireContent(bytesFromDocument(d)), Clock.getTime());
        }

        return Pair.create(StoredContent.acquireContent(f), f.getTimeStamp());
    }

    
    private static Pair<StoredContent, Long> getActualContentNoAcquire( VirtualFile f) {
        ContentAndTimestamps result = f.getUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY);
        if (result == null) {
            return Pair.create(StoredContent.transientContent(f), f.getTimeStamp());
        }
        return Pair.create(result.content, result.registeredTimestamp);
    }

    private static byte[] bytesFromDocument( Document d) {
        try {
            return d.getText().getBytes(getFile(d).getCharset().name());
        }
        catch (UnsupportedEncodingException e) {
            return d.getText().getBytes();
        }
    }

    public String stringFromBytes( byte[] bytes,  String path) {
        try {
            VirtualFile file = findVirtualFile(path);
            if (file == null) {
                return CharsetToolkit.bytesToString(bytes, EncodingRegistry.getInstance().getDefaultCharset());
            }
            return new String(bytes, file.getCharset().name());
        }
        catch (UnsupportedEncodingException e1) {
            return new String(bytes);
        }
    }

    public void saveAllUnsavedDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    
    private static VirtualFile getFile( Document d) {
        return FileDocumentManager.getInstance().getFile(d);
    }

    
    public Document getDocument( String path) {
        return FileDocumentManager.getInstance().getDocument(findVirtualFile(path));
    }

    
    public FileType getFileType( String fileName) {
        return FileTypeManager.getInstance().getFileTypeByFileName(fileName);
    }

    private static class ContentAndTimestamps {
        long registeredTimestamp;
        StoredContent content;
        long documentModificationStamp;

        private ContentAndTimestamps(long registeredTimestamp, StoredContent content, long documentModificationStamp) {
            this.registeredTimestamp = registeredTimestamp;
            this.content = content;
            this.documentModificationStamp = documentModificationStamp;
        }
    }
}
