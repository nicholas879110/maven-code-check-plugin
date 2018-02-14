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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jul 17, 2007
 * Time: 3:20:51 PM
 */
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.ModificationTracker;
import com.gome.maven.openapi.util.SimpleModificationTracker;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

@State(
        name = "Encoding",
        storages = {
                @Storage(file = StoragePathMacros.PROJECT_FILE),
                @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/encodings.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public class EncodingProjectManagerImpl extends EncodingProjectManager implements NamedComponent, PersistentStateComponent<Element> {
     private static final String PROJECT_URL = "PROJECT";
    private final Project myProject;
    private boolean myNative2AsciiForPropertiesFiles;
    private Charset myDefaultCharsetForPropertiesFiles;
    private final SimpleModificationTracker myModificationTracker = new SimpleModificationTracker();

    // we should avoid changed file
    private String myOldUTFGuessing;
    private boolean myNative2AsciiForPropertiesFilesWasSpecified;

    public EncodingProjectManagerImpl(Project project, PsiDocumentManager documentManager) {
        myProject = project;
        documentManager.addListener(new PsiDocumentManager.Listener() {
            @Override
            public void documentCreated( Document document, PsiFile psiFile) {
                ((EncodingManagerImpl)EncodingManager.getInstance()).queueUpdateEncodingFromContent(document);
            }

            @Override
            public void fileCreated( PsiFile file,  Document document) {
            }
        });
    }

    //null key means project
    private final Map<VirtualFile, Charset> myMapping = ContainerUtil.newConcurrentMap();
    private volatile Charset myProjectCharset;

    @Override
    public Element getState() {
        Element element = new Element("x");
        if (!myMapping.isEmpty()) {
            List<VirtualFile> files = new ArrayList<VirtualFile>(myMapping.keySet());
            ContainerUtil.quickSort(files, new Comparator<VirtualFile>() {
                @Override
                public int compare(final VirtualFile o1, final VirtualFile o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
            for (VirtualFile file : files) {
                Charset charset = myMapping.get(file);
                Element child = new Element("file");
                element.addContent(child);
                child.setAttribute("url", file.getUrl());
                child.setAttribute("charset", charset.name());
            }
        }
        if (myProjectCharset != null) {
            Element child = new Element("file");
            element.addContent(child);
            child.setAttribute("url", PROJECT_URL);
            child.setAttribute("charset", myProjectCharset.name());
        }

        if (myOldUTFGuessing != null) {
            element.setAttribute("useUTFGuessing", myOldUTFGuessing);
        }

        if (myNative2AsciiForPropertiesFiles || myNative2AsciiForPropertiesFilesWasSpecified) {
            element.setAttribute("native2AsciiForPropertiesFiles", Boolean.toString(myNative2AsciiForPropertiesFiles));
        }

        if (myDefaultCharsetForPropertiesFiles != null) {
            element.setAttribute("defaultCharsetForPropertiesFiles", myDefaultCharsetForPropertiesFiles.name());
        }
        return element;
    }

    @Override
    public void loadState(Element element) {
        myMapping.clear();
        List<Element> files = element.getChildren("file");
        if (!files.isEmpty()) {
            Map<VirtualFile, Charset> mapping = new HashMap<VirtualFile, Charset>();
            for (Element fileElement : files) {
                String url = fileElement.getAttributeValue("url");
                String charsetName = fileElement.getAttributeValue("charset");
                Charset charset = CharsetToolkit.forName(charsetName);
                if (charset == null) continue;
                if (url.equals(PROJECT_URL)) {
                    myProjectCharset = charset;
                }
                else {
                    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
                    if (file != null) {
                        mapping.put(file, charset);
                    }
                }
            }
            myMapping.putAll(mapping);
        }

        String native2AsciiForPropertiesFiles = element.getAttributeValue("native2AsciiForPropertiesFiles");
        myNative2AsciiForPropertiesFiles = Boolean.parseBoolean(native2AsciiForPropertiesFiles);
        myDefaultCharsetForPropertiesFiles = CharsetToolkit.forName(element.getAttributeValue("defaultCharsetForPropertiesFiles"));

        myModificationTracker.incModificationCount();

        if (!myProject.isDefault()) {
            myOldUTFGuessing = element.getAttributeValue("useUTFGuessing");
            myNative2AsciiForPropertiesFilesWasSpecified = native2AsciiForPropertiesFiles != null;
        }
    }

    @Override
    
    
    public String getComponentName() {
        return "EncodingProjectManager";
    }

    @Override
    
    public Charset getEncoding( VirtualFile virtualFile, boolean useParentDefaults) {
        VirtualFile parent = virtualFile;
        while (parent != null) {
            Charset charset = myMapping.get(parent);
            if (charset != null || !useParentDefaults) return charset;
            parent = parent.getParent();
        }

        return getDefaultCharset();
    }

    
    public ModificationTracker getModificationTracker() {
        return myModificationTracker;
    }

    @Override
    public void setEncoding( final VirtualFile virtualFileOrDir,  final Charset charset) {
        Charset oldCharset;

        if (virtualFileOrDir == null) {
            oldCharset = myProjectCharset;
            myProjectCharset = charset;
        }
        else {
            if (charset == null) {
                oldCharset = myMapping.remove(virtualFileOrDir);
            }
            else {
                oldCharset = myMapping.put(virtualFileOrDir, charset);
            }
        }

        if (!Comparing.equal(oldCharset, charset)) {
            myModificationTracker.incModificationCount();
            if (virtualFileOrDir != null) {
                virtualFileOrDir.setCharset(virtualFileOrDir.getBOM() == null ? charset : null);
            }
            reloadAllFilesUnder(virtualFileOrDir);
        }
    }

    private static void clearAndReload( VirtualFile virtualFileOrDir) {
        virtualFileOrDir.setCharset(null);
        reload(virtualFileOrDir);
    }

    private static void reload( final VirtualFile virtualFile) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                FileDocumentManager documentManager = FileDocumentManager.getInstance();
                ((VirtualFileListener)documentManager)
                        .contentsChanged(new VirtualFileEvent(null, virtualFile, virtualFile.getName(), virtualFile.getParent()));
            }
        });
    }

    @Override
    
    public Collection<Charset> getFavorites() {
        Set<Charset> result = new HashSet<Charset>();
        result.addAll(myMapping.values());
        result.add(getDefaultCharset());
        result.add(CharsetToolkit.UTF8_CHARSET);
        result.add(CharsetToolkit.getDefaultSystemCharset());
        result.add(CharsetToolkit.UTF_16_CHARSET);
        result.add(CharsetToolkit.forName("ISO-8859-1"));
        result.add(CharsetToolkit.forName("US-ASCII"));
        result.add(EncodingManager.getInstance().getDefaultCharset());
        result.add(EncodingManager.getInstance().getDefaultCharsetForPropertiesFiles(null));

        result.remove(null);
        return result;
    }

    
    public Map<VirtualFile, Charset> getAllMappings() {
        return myMapping;
    }

    public void setMapping( final Map<VirtualFile, Charset> mapping) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        FileDocumentManager.getInstance().saveAllDocuments();  // consider all files as unmodified
        final Map<VirtualFile, Charset> newMap = new HashMap<VirtualFile, Charset>(mapping.size());
        final Map<VirtualFile, Charset> oldMap = new HashMap<VirtualFile, Charset>(myMapping);

        // ChangeFileEncodingAction should not start progress "reload files..."
        suppressReloadDuring(new Runnable() {
            @Override
            public void run() {
                ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
                for (Map.Entry<VirtualFile, Charset> entry : mapping.entrySet()) {
                    VirtualFile virtualFile = entry.getKey();
                    Charset charset = entry.getValue();
                    if (charset == null) throw new IllegalArgumentException("Null charset for " + virtualFile + "; mapping: " + mapping);
                    if (virtualFile == null) {
                        myProjectCharset = charset;
                    }
                    else {
                        if (!fileIndex.isInContent(virtualFile)) continue;
                        if (!virtualFile.isDirectory() && !Comparing.equal(charset, oldMap.get(virtualFile))) {
                            Document document;
                            byte[] bytes;
                            try {
                                document = FileDocumentManager.getInstance().getDocument(virtualFile);
                                if (document == null) throw new IOException();
                                bytes = virtualFile.contentsToByteArray();
                            }
                            catch (IOException e) {
                                continue;
                            }
                            // ask whether to reload/convert when in doubt
                            boolean changed = new ChangeFileEncodingAction().chosen(document, null, virtualFile, bytes, charset);

                            if (!changed) continue;
                        }
                        newMap.put(virtualFile, charset);
                    }
                }
            }
        });

        myMapping.clear();
        myMapping.putAll(newMap);

        final Set<VirtualFile> changed = new HashSet<VirtualFile>(oldMap.keySet());
        for (VirtualFile newFile : newMap.keySet()) {
            if (Comparing.equal(oldMap.get(newFile), newMap.get(newFile))) changed.remove(newFile);
        }

        Set<VirtualFile> added = new HashSet<VirtualFile>(newMap.keySet());
        added.removeAll(oldMap.keySet());

        Set<VirtualFile> removed = new HashSet<VirtualFile>(oldMap.keySet());
        removed.removeAll(newMap.keySet());

        changed.addAll(added);
        changed.addAll(removed);
        changed.remove(null);

        if (!changed.isEmpty()) {
            final Processor<VirtualFile> reloadProcessor = createChangeCharsetProcessor();
            tryStartReloadWithProgress(new Runnable() {
                @Override
                public void run() {
                    Set<VirtualFile> processed = new THashSet<VirtualFile>();
                    next:
                    for (VirtualFile changedFile : changed) {
                        for (VirtualFile processedFile : processed) {
                            if (VfsUtilCore.isAncestor(processedFile, changedFile, false)) continue next;
                        }
                        processSubFiles(changedFile, reloadProcessor);
                        processed.add(changedFile);
                    }
                }
            });
        }

        myModificationTracker.incModificationCount();
    }

    private static Processor<VirtualFile> createChangeCharsetProcessor() {
        return new Processor<VirtualFile>() {
            @Override
            public boolean process(final VirtualFile file) {
                if (!(file instanceof VirtualFileSystemEntry)) return false;
                Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(file);
                if (cachedDocument == null) return true;
                ProgressManager.progress("Reloading files...", file.getPresentableUrl());
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        clearAndReload(file);
                    }
                });
                return true;
            }
        };
    }

    private boolean processSubFiles( VirtualFile file,  final Processor<VirtualFile> processor) {
        if (file == null) {
            for (VirtualFile virtualFile : ProjectRootManager.getInstance(myProject).getContentRoots()) {
                if (!processSubFiles(virtualFile, processor)) return false;
            }
            return true;
        }

        return VirtualFileVisitor.CONTINUE == VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
            @Override
            public boolean visitFile( final VirtualFile file) {
                return processor.process(file);
            }
        });
    }

    //retrieves encoding for the Project node
    @Override
    
    public Charset getDefaultCharset() {
        Charset charset = myProjectCharset;
        return charset == null ? Charset.defaultCharset() : charset;
    }

    @Override
    public boolean isUseUTFGuessing(final VirtualFile virtualFile) {
        return true;
    }

    private static final ThreadLocal<Boolean> SUPPRESS_RELOAD = new ThreadLocal<Boolean>();
    static void suppressReloadDuring( Runnable action) {
        Boolean old = SUPPRESS_RELOAD.get();
        try {
            SUPPRESS_RELOAD.set(Boolean.TRUE);
            action.run();
        }
        finally {
            SUPPRESS_RELOAD.set(old);
        }
    }

    private boolean tryStartReloadWithProgress( final Runnable reloadAction) {
        Boolean suppress = SUPPRESS_RELOAD.get();
        if (suppress == Boolean.TRUE) return false;
        FileDocumentManager.getInstance().saveAllDocuments();  // consider all files as unmodified
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                suppressReloadDuring(reloadAction);
            }
        }, "Reload Files", false, myProject);
    }

    private void reloadAllFilesUnder( final VirtualFile root) {
        tryStartReloadWithProgress(new Runnable() {
            @Override
            public void run() {
                processSubFiles(root, new Processor<VirtualFile>() {
                    @Override
                    public boolean process(final VirtualFile file) {
                        if (!(file instanceof VirtualFileSystemEntry)) return true;
                        Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(file);
                        if (cachedDocument != null) {
                            ProgressManager.progress("Reloading file...", file.getPresentableUrl());
                            UIUtil.invokeLaterIfNeeded(new Runnable() {
                                @Override
                                public void run() {
                                    reload(file);
                                }
                            });
                        }
                        // for not loaded files deep under project, reset encoding to give them chance re-detect the right one later
                        else if (file.isCharsetSet() && !file.equals(root)) {
                            file.setCharset(null);
                        }
                        return true;
                    }
                });
            }
        });
    }

    @Override
    public boolean isNative2Ascii( final VirtualFile virtualFile) {
        return virtualFile.getFileType() == StdFileTypes.PROPERTIES && myNative2AsciiForPropertiesFiles;
    }

    @Override
    public boolean isNative2AsciiForPropertiesFiles() {
        return myNative2AsciiForPropertiesFiles;
    }

    @Override
    public void setNative2AsciiForPropertiesFiles(final VirtualFile virtualFile, final boolean native2Ascii) {
        if (myNative2AsciiForPropertiesFiles != native2Ascii) {
            myNative2AsciiForPropertiesFiles = native2Ascii;
            ((EncodingManagerImpl)EncodingManager.getInstance()).firePropertyChange(null, PROP_NATIVE2ASCII_SWITCH, !native2Ascii, native2Ascii);
        }
    }

     // empty means system default
    @Override
    public String getDefaultCharsetName() {
        Charset charset = getEncoding(null, false);
        return charset == null ? "" : charset.name();
    }

    @Override
    public void setDefaultCharsetName( String name) {
        setEncoding(null, name.isEmpty() ? null : CharsetToolkit.forName(name));
    }

    @Override
    
    public Charset getDefaultCharsetForPropertiesFiles( final VirtualFile virtualFile) {
        return myDefaultCharsetForPropertiesFiles;
    }

    @Override
    public void setDefaultCharsetForPropertiesFiles( final VirtualFile virtualFile,  Charset charset) {
        Charset old = myDefaultCharsetForPropertiesFiles;
        if (!Comparing.equal(old, charset)) {
            myDefaultCharsetForPropertiesFiles = charset;
            ((EncodingManagerImpl)EncodingManager.getInstance()).firePropertyChange(null, PROP_PROPERTIES_FILES_ENCODING, old, charset);
        }
    }

    @Override
    public void addPropertyChangeListener( PropertyChangeListener listener,  Disposable parentDisposable) {
        EncodingManager.getInstance().addPropertyChangeListener(listener,parentDisposable);
    }

    @Override
    
    public Charset getCachedCharsetFromContent( Document document) {
        return EncodingManager.getInstance().getCachedCharsetFromContent(document);
    }
}
