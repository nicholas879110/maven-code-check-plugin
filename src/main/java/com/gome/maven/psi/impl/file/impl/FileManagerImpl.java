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

package com.gome.maven.psi.impl.file.impl;

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.ContentBasedFileSubstitutor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.LowMemoryWatcher;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileVisitor;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.*;
import com.gome.maven.psi.impl.file.PsiDirectoryFactory;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBusConnection;
import gnu.trove.THashMap;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class FileManagerImpl implements FileManager {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.file.impl.FileManagerImpl");
    private final Key<FileViewProvider> myPsiHardRefKey = Key.create("HARD_REFERENCE_TO_PSI"); //non-static!

    private final PsiManagerImpl myManager;
    private final FileIndexFacade myFileIndex;

    private final ConcurrentMap<VirtualFile, PsiDirectory> myVFileToPsiDirMap = ContainerUtil.createConcurrentSoftValueMap();
    private final ConcurrentMap<VirtualFile, FileViewProvider> myVFileToViewProviderMap = ContainerUtil.createConcurrentWeakValueMap();

    private boolean myInitialized = false;
    private boolean myDisposed = false;

    private final FileDocumentManager myFileDocumentManager;
    private final MessageBusConnection myConnection;

    public FileManagerImpl(PsiManagerImpl manager, FileDocumentManager fileDocumentManager, FileIndexFacade fileIndex) {
        myManager = manager;
        myFileIndex = fileIndex;
        myConnection = manager.getProject().getMessageBus().connect();

        myFileDocumentManager = fileDocumentManager;

        myConnection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                updateAllViewProviders();
            }

            @Override
            public void exitDumbMode() {
                updateAllViewProviders();
            }
        });
        Disposer.register(manager.getProject(), this);
        LowMemoryWatcher.register(new Runnable() {
            @Override
            public void run() {
                processQueue();
            }
        }, this);
    }

    private static final VirtualFile NULL = new LightVirtualFile();

    public void processQueue() {
        // just to call processQueue()
        myVFileToViewProviderMap.remove(NULL);
    }


    public ConcurrentMap<VirtualFile, FileViewProvider> getVFileToViewProviderMap() {
        return myVFileToViewProviderMap;
    }

    private void updateAllViewProviders() {
        handleFileTypesChange(new FileTypesChanged() {
            @Override
            protected void updateMaps() {
                for (final FileViewProvider provider : myVFileToViewProviderMap.values()) {
                    if (!provider.getVirtualFile().isValid()) {
                        continue;
                    }

                    for (Language language : provider.getLanguages()) {
                        final PsiFile psi = provider.getPsi(language);
                        if (psi instanceof PsiFileImpl) {
                            ((PsiFileImpl)psi).clearCaches();
                        }
                    }
                }
                removeInvalidFilesAndDirs(false);
                checkLanguageChange();
            }
        });
    }

    private void checkLanguageChange() {
        Map<VirtualFile, FileViewProvider> fileToPsiFileMap = new THashMap<VirtualFile, FileViewProvider>(myVFileToViewProviderMap);
        myVFileToViewProviderMap.clear();
        for (Iterator<VirtualFile> iterator = fileToPsiFileMap.keySet().iterator(); iterator.hasNext();) {
            VirtualFile vFile = iterator.next();
            Language language = getLanguage(vFile);
            if (language != null && language != fileToPsiFileMap.get(vFile).getBaseLanguage()) {
                iterator.remove();
            }
        }
        myVFileToViewProviderMap.putAll(fileToPsiFileMap);
    }

    public void forceReload( VirtualFile vFile) {
        if (findCachedViewProvider(vFile) == null) {
            return;
        }
        setViewProvider(vFile, null);

        VirtualFile dir = vFile.getParent();
        PsiDirectory parentDir = dir == null ? null : getCachedDirectory(dir);
        if (parentDir != null) {
            PsiTreeChangeEventImpl treeEvent = new PsiTreeChangeEventImpl(myManager);
            treeEvent.setParent(parentDir);
            myManager.childrenChanged(treeEvent);
        }
    }

    @Override
    public void dispose() {
        if (myInitialized) {
            myConnection.disconnect();
        }
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        myDisposed = true;
    }

    @Override
    public void cleanupForNextTest() {
        myVFileToViewProviderMap.clear();
        myVFileToPsiDirMap.clear();
        processQueue();
    }

    @Override
    
    public FileViewProvider findViewProvider( final VirtualFile file) {
        assert !file.isDirectory();
        FileViewProvider viewProvider = findCachedViewProvider(file);
        if (viewProvider != null) return viewProvider;
        viewProvider = myVFileToViewProviderMap.get(file);
        if(viewProvider == null) {
            viewProvider = ConcurrencyUtil.cacheOrGet(myVFileToViewProviderMap, file, createFileViewProvider(file, true));
        }
        return viewProvider;
    }

    @Override
    public FileViewProvider findCachedViewProvider( final VirtualFile file) {
        FileViewProvider viewProvider = getFromInjected(file);
        if (viewProvider == null) viewProvider = myVFileToViewProviderMap.get(file);
        if (viewProvider == null) viewProvider = file.getUserData(myPsiHardRefKey);
        return viewProvider;
    }

    
    private FileViewProvider getFromInjected( VirtualFile file) {
        if (file instanceof VirtualFileWindow) {
            DocumentWindow document = ((VirtualFileWindow)file).getDocumentWindow();
            PsiFile psiFile = PsiDocumentManager.getInstance(myManager.getProject()).getCachedPsiFile(document);
            if (psiFile == null) return null;
            return psiFile.getViewProvider();
        }
        return null;
    }

    @Override
    public void setViewProvider( final VirtualFile virtualFile,  final FileViewProvider fileViewProvider) {
        FileViewProvider prev = findCachedViewProvider(virtualFile);
        if (prev != null) {
            DebugUtil.startPsiModification(null);
            try {
                DebugUtil.onInvalidated(prev);
            }
            finally {
                DebugUtil.finishPsiModification();
            }
        }

        if (!(virtualFile instanceof VirtualFileWindow)) {
            if (fileViewProvider == null) {
                myVFileToViewProviderMap.remove(virtualFile);

                Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
                if (document != null) {
                    PsiDocumentManagerBase.cachePsi(document, null);
                }
                virtualFile.putUserData(myPsiHardRefKey, null);
            }
            else {
                if (virtualFile instanceof LightVirtualFile) {
                    virtualFile.putUserData(myPsiHardRefKey, fileViewProvider);
                } else {
                    myVFileToViewProviderMap.put(virtualFile, fileViewProvider);
                }
            }
        }
    }

    @Override
    
    public FileViewProvider createFileViewProvider( final VirtualFile file, boolean eventSystemEnabled) {
        Language language = getLanguage(file);
        return createFileViewProvider(file, eventSystemEnabled, language);
    }

    
    private FileViewProvider createFileViewProvider( VirtualFile file, boolean eventSystemEnabled, Language language) {
        final FileViewProviderFactory factory = language == null
                ? FileTypeFileViewProviders.INSTANCE.forFileType(file.getFileType())
                : LanguageFileViewProviders.INSTANCE.forLanguage(language);
        FileViewProvider viewProvider = factory == null ? null : factory.createFileViewProvider(file, language, myManager, eventSystemEnabled);

        return viewProvider == null ? new SingleRootFileViewProvider(myManager, file, eventSystemEnabled) : viewProvider;
    }

    
    private Language getLanguage( VirtualFile file) {
        final FileType fileType = file.getFileType();
        Project project = myManager.getProject();
        if (fileType instanceof LanguageFileType) {
            return LanguageSubstitutors.INSTANCE.substituteLanguage(((LanguageFileType)fileType).getLanguage(), file, project);
        }
        // Define language for binary file
        final ContentBasedFileSubstitutor[] processors = Extensions.getExtensions(ContentBasedFileSubstitutor.EP_NAME);
        for (ContentBasedFileSubstitutor processor : processors) {
            Language language = processor.obtainLanguageForFile(file);
            if (language != null) {
                return language;
            }
        }

        return null;
    }

    public void markInitialized() {
        LOG.assertTrue(!myInitialized);
        myDisposed = false;
        myInitialized = true;
    }

    public boolean isInitialized() {
        return myInitialized;
    }

    void processFileTypesChanged() {
        handleFileTypesChange(new FileTypesChanged() {
            @Override
            protected void updateMaps() {
                removeInvalidFilesAndDirs(true);
            }
        });
    }

    private abstract class FileTypesChanged implements Runnable {
        protected abstract void updateMaps();

        @Override
        public void run() {
            PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(myManager);
            event.setPropertyName(PsiTreeChangeEvent.PROP_FILE_TYPES);
            myManager.beforePropertyChange(event);

            updateMaps();

            myManager.propertyChanged(event);
        }
    }

    private boolean myProcessingFileTypesChange = false;
    private void handleFileTypesChange( FileTypesChanged runnable) {
        if (myProcessingFileTypesChange) return;
        myProcessingFileTypesChange = true;
        try {
            ApplicationManager.getApplication().runWriteAction(runnable);
        }
        finally {
            myProcessingFileTypesChange = false;
        }
    }

    void dispatchPendingEvents() {
        if (!myInitialized) {
            LOG.error("Project is not yet initialized: "+myManager.getProject());
        }
        if (myDisposed) {
            LOG.error("Project is already disposed: "+myManager.getProject());
        }

        myConnection.deliverImmediately();
    }

    public void checkConsistency() {
        HashMap<VirtualFile, FileViewProvider> fileToViewProvider = new HashMap<VirtualFile, FileViewProvider>(myVFileToViewProviderMap);
        myVFileToViewProviderMap.clear();
        for (VirtualFile vFile : fileToViewProvider.keySet()) {
            final FileViewProvider fileViewProvider = fileToViewProvider.get(vFile);

            LOG.assertTrue(vFile.isValid());
            PsiFile psiFile1 = findFile(vFile);
            if (psiFile1 != null && fileViewProvider != null && fileViewProvider.isPhysical()) { // might get collected
                PsiFile psi = fileViewProvider.getPsi(fileViewProvider.getBaseLanguage());
                assert psi != null : fileViewProvider +"; "+fileViewProvider.getBaseLanguage()+"; "+psiFile1;
                assert psiFile1.getClass().equals(psi.getClass()) : psiFile1 +"; "+psi + "; "+psiFile1.getClass() +"; "+psi.getClass();
            }
        }

        HashMap<VirtualFile, PsiDirectory> fileToPsiDirMap = new HashMap<VirtualFile, PsiDirectory>(myVFileToPsiDirMap);
        myVFileToPsiDirMap.clear();

        for (VirtualFile vFile : fileToPsiDirMap.keySet()) {
            LOG.assertTrue(vFile.isValid());
            PsiDirectory psiDir1 = findDirectory(vFile);
            LOG.assertTrue(psiDir1 != null);

            VirtualFile parent = vFile.getParent();
            if (parent != null) {
                LOG.assertTrue(myVFileToPsiDirMap.containsKey(parent));
            }
        }
    }

    @Override
    
    public PsiFile findFile( VirtualFile vFile) {
        if (vFile.isDirectory()) return null;
        final Project project = myManager.getProject();
        if (project.isDefault()) return null;

        ApplicationManager.getApplication().assertReadAccessAllowed();
        if (!vFile.isValid()) {
            LOG.error("Invalid file: " + vFile);
            return null;
        }

        dispatchPendingEvents();
        final FileViewProvider viewProvider = findViewProvider(vFile);
        return viewProvider.getPsi(viewProvider.getBaseLanguage());
    }

    @Override
    
    public PsiFile getCachedPsiFile( VirtualFile vFile) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        LOG.assertTrue(vFile.isValid(), "Invalid file");
        if (myDisposed) {
            LOG.error("Project is already disposed: " + myManager.getProject());
        }
        if (!myInitialized) return null;

        dispatchPendingEvents();

        return getCachedPsiFileInner(vFile);
    }

    @Override
    
    public PsiDirectory findDirectory( VirtualFile vFile) {
        LOG.assertTrue(myInitialized, "Access to psi files should be performed only after startup activity");
        if (myDisposed) {
            LOG.error("Access to psi files should not be performed after project disposal: "+myManager.getProject());
        }


        ApplicationManager.getApplication().assertReadAccessAllowed();
        if (!vFile.isValid()) {
            LOG.error("File is not valid:" + vFile);
        }

        if (!vFile.isDirectory()) return null;
        dispatchPendingEvents();

        return findDirectoryImpl(vFile);
    }

    
    private PsiDirectory findDirectoryImpl( VirtualFile vFile) {
        PsiDirectory psiDir = myVFileToPsiDirMap.get(vFile);
        if (psiDir != null) return psiDir;

        if (Registry.is("ide.hide.excluded.files")) {
            if (myFileIndex.isExcludedFile(vFile)) return null;
        }
        else {
            if (myFileIndex.isUnderIgnored(vFile)) return null;
        }

        VirtualFile parent = vFile.getParent();
        if (parent != null) { //?
            findDirectoryImpl(parent);// need to cache parent directory - used for firing events
        }

        psiDir = PsiDirectoryFactory.getInstance(myManager.getProject()).createDirectory(vFile);
        return ConcurrencyUtil.cacheOrGet(myVFileToPsiDirMap, vFile, psiDir);
    }

    public PsiDirectory getCachedDirectory( VirtualFile vFile) {
        return myVFileToPsiDirMap.get(vFile);
    }

    void removeFilesAndDirsRecursively( VirtualFile vFile) {
        VfsUtilCore.visitChildrenRecursively(vFile, new VirtualFileVisitor() {
            @Override
            public boolean visitFile( VirtualFile file) {
                if (file.isDirectory()) {
                    myVFileToPsiDirMap.remove(file);
                }
                else {
                    myVFileToViewProviderMap.remove(file);
                }
                return true;
            }
        });
    }

    
    PsiFile getCachedPsiFileInner( VirtualFile file) {
        FileViewProvider fileViewProvider = myVFileToViewProviderMap.get(file);
        if (fileViewProvider == null) fileViewProvider = file.getUserData(myPsiHardRefKey);
        return fileViewProvider instanceof SingleRootFileViewProvider
                ? ((SingleRootFileViewProvider)fileViewProvider).getCachedPsi(fileViewProvider.getBaseLanguage()) : null;
    }

    
    @Override
    public List<PsiFile> getAllCachedFiles() {
        List<PsiFile> files = new ArrayList<PsiFile>();
        for (FileViewProvider provider : myVFileToViewProviderMap.values()) {
            if (provider instanceof SingleRootFileViewProvider) {
                ContainerUtil.addIfNotNull(files, ((SingleRootFileViewProvider)provider).getCachedPsi(provider.getBaseLanguage()));
            }
        }
        return files;
    }

    void removeInvalidFilesAndDirs(boolean useFind) {
        Map<VirtualFile, PsiDirectory> fileToPsiDirMap = new THashMap<VirtualFile, PsiDirectory>(myVFileToPsiDirMap);
        if (useFind) {
            myVFileToPsiDirMap.clear();
        }
        for (Iterator<VirtualFile> iterator = fileToPsiDirMap.keySet().iterator(); iterator.hasNext();) {
            VirtualFile vFile = iterator.next();
            if (!vFile.isValid()) {
                iterator.remove();
            }
            else {
                PsiDirectory psiDir = findDirectory(vFile);
                if (psiDir == null) {
                    iterator.remove();
                }
            }
        }
        myVFileToPsiDirMap.clear();
        myVFileToPsiDirMap.putAll(fileToPsiDirMap);

        // note: important to update directories map first - findFile uses findDirectory!
        Map<VirtualFile, FileViewProvider> fileToPsiFileMap = new THashMap<VirtualFile, FileViewProvider>(myVFileToViewProviderMap);
        if (useFind) {
            myVFileToViewProviderMap.clear();
        }
        for (Iterator<VirtualFile> iterator = fileToPsiFileMap.keySet().iterator(); iterator.hasNext();) {
            VirtualFile vFile = iterator.next();

            if (!vFile.isValid()) {
                iterator.remove();
                continue;
            }

            if (useFind) {
                FileViewProvider view = fileToPsiFileMap.get(vFile);
                if (view == null) { // soft ref. collected
                    iterator.remove();
                    continue;
                }
                PsiFile psiFile1 = findFile(vFile);
                if (psiFile1 == null) {
                    iterator.remove();
                    continue;
                }

                PsiFile psi = view.getPsi(view.getBaseLanguage());
                if (psi == null || !psiFile1.getClass().equals(psi.getClass()) ||
                        psiFile1.getViewProvider().getBaseLanguage() != view.getBaseLanguage() // e.g. JSP <-> JSPX
                        ) {
                    iterator.remove();
                }
                else if (psi instanceof PsiFileImpl) {
                    ((PsiFileImpl)psi).clearCaches();
                }
            }
        }
        myVFileToViewProviderMap.clear();
        myVFileToViewProviderMap.putAll(fileToPsiFileMap);
    }

    @Override
    public void reloadFromDisk( PsiFile file) {
        reloadFromDisk(file, false);
    }

    void reloadFromDisk( PsiFile file, boolean ignoreDocument) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        VirtualFile vFile = file.getVirtualFile();
        assert vFile != null;

        if (file instanceof PsiBinaryFile) return;
        FileDocumentManager fileDocumentManager = myFileDocumentManager;
        Document document = fileDocumentManager.getCachedDocument(vFile);
        if (document != null && !ignoreDocument){
            fileDocumentManager.reloadFromDisk(document);
        }
        else {
            FileViewProvider latestProvider = createFileViewProvider(vFile, false);
            if (latestProvider.getPsi(latestProvider.getBaseLanguage()) instanceof PsiBinaryFile) {
                forceReload(vFile);
                return;
            }

            PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(myManager);
            event.setParent(file);
            event.setFile(file);
            if (file instanceof PsiFileImpl && ((PsiFileImpl)file).isContentsLoaded()) {
                event.setOffset(0);
                event.setOldLength(file.getTextLength());
            }
            myManager.beforeChildrenChange(event);

            if (file instanceof PsiFileEx) {
                ((PsiFileEx)file).onContentReload();
            }

            myManager.childrenChanged(event);
        }
    }
}
