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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.Disposable;
//import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.StateSplitter;
import com.gome.maven.openapi.components.StateStorageException;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
//import com.gome.maven.openapi.components.store.ReadOnlyModificationException;
//import com.gome.maven.openapi.editor.DocumentRunnable;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileAdapter;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.openapi.vfs.tracker.VirtualFileTracker;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.containers.SmartHashSet;
import gnu.trove.TObjectObjectProcedure;
import org.jdom.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class DirectoryBasedStorage extends StateStorageBase<DirectoryStorageData> {
    private final File myDir;
    private volatile VirtualFile myVirtualFile;
    @SuppressWarnings("deprecation")
    private final StateSplitter mySplitter;

    public DirectoryBasedStorage( TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                  String dir,
                                 @SuppressWarnings("deprecation")  StateSplitter splitter,
                                  Disposable parentDisposable,
                                  final Listener listener) {
        super(pathMacroSubstitutor);

        myDir = new File(dir);
        mySplitter = splitter;

        VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
        if (virtualFileTracker != null && listener != null) {
            virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + myDir.getAbsolutePath().replace(File.separatorChar, '/'), new VirtualFileAdapter() {
                @Override
                public void contentsChanged( VirtualFileEvent event) {
                    notifyIfNeed(event);
                }

                @Override
                public void fileDeleted( VirtualFileEvent event) {
                    if (event.getFile().equals(myVirtualFile)) {
                        myVirtualFile = null;
                    }
                    notifyIfNeed(event);
                }

                @Override
                public void fileCreated( VirtualFileEvent event) {
                    notifyIfNeed(event);
                }

                private void notifyIfNeed( VirtualFileEvent event) {
                    // storage directory will be removed if the only child was removed
                    if (event.getFile().isDirectory() || DirectoryStorageData.isStorageFile(event.getFile())) {
                        listener.storageFileChanged(event, DirectoryBasedStorage.this);
                    }
                }
            }, false, parentDisposable);
        }
    }

    @Override
    public void analyzeExternalChangesAndUpdateIfNeed( Collection<VirtualFile> changedFiles,  Set<String> componentNames) {
        // todo reload only changed file, compute diff
        DirectoryStorageData oldData = myStorageData;
        DirectoryStorageData newData = loadData();
        myStorageData = newData;
        if (oldData == null) {
            componentNames.addAll(newData.getComponentNames());
        }
        else {
            componentNames.addAll(oldData.getComponentNames());
            componentNames.addAll(newData.getComponentNames());
        }
    }

    
    @Override
    protected Element getStateAndArchive( DirectoryStorageData storageData, Object component,  String componentName) {
        return storageData.getCompositeStateAndArchive(componentName, mySplitter);
    }

    
    @Override
    protected DirectoryStorageData loadData() {
        DirectoryStorageData storageData = new DirectoryStorageData();
        storageData.loadFrom(getVirtualFile(), myPathMacroSubstitutor);
        return storageData;
    }

    
    private VirtualFile getVirtualFile() {
        VirtualFile virtualFile = myVirtualFile;
        if (virtualFile == null) {
            myVirtualFile = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myDir);
        }
        return virtualFile;
    }

    @Override
    
    public ExternalizationSession startExternalization() {
        return checkIsSavingDisabled() ? null : new MySaveSession(this, getStorageData());
    }

    
    public static VirtualFile createDir( File ioDir,  Object requestor) {
        //noinspection ResultOfMethodCallIgnored
        ioDir.mkdirs();
        String parentFile = ioDir.getParent();
        VirtualFile parentVirtualFile = parentFile == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByPath(parentFile.replace(File.separatorChar, '/'));
        if (parentVirtualFile == null) {
            throw new StateStorageException(ProjectBundle.message("project.configuration.save.file.not.found", parentFile));
        }
        return getFile(ioDir.getName(), parentVirtualFile, requestor);
    }

    
    public static VirtualFile getFile( String fileName,  VirtualFile parentVirtualFile,  Object requestor) {
        VirtualFile file = parentVirtualFile.findChild(fileName);
        if (file != null) {
            return file;
        }

//        AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
        try {
            return parentVirtualFile.createChildData(requestor, fileName);
        }
        catch (IOException e) {
            throw new StateStorageException(e);
        }
        finally {
//            token.finish();
        }
    }

    private static class MySaveSession extends SaveSessionBase {
        private final DirectoryBasedStorage storage;
        private final DirectoryStorageData originalStorageData;
        private DirectoryStorageData copiedStorageData;

        private final Set<String> dirtyFileNames = new SmartHashSet<String>();
        private final Set<String> removedFileNames = new SmartHashSet<String>();

        private MySaveSession( DirectoryBasedStorage storage,  DirectoryStorageData storageData) {
            this.storage = storage;
            originalStorageData = storageData;
        }

        @Override
        protected void setSerializedState( Object component,  String componentName,  Element element) {
            removedFileNames.addAll(originalStorageData.getFileNames(componentName));
            if (JDOMUtil.isEmpty(element)) {
                doSetState(componentName, null, null);
            }
            else {
                for (Pair<Element, String> pair : storage.mySplitter.splitState(element)) {
                    removedFileNames.remove(pair.second);
                    doSetState(componentName, pair.second, pair.first);
                }

                if (!removedFileNames.isEmpty()) {
                    for (String fileName : removedFileNames) {
                        doSetState(componentName, fileName, null);
                    }
                }
            }
        }

        private void doSetState( String componentName,  String fileName,  Element subState) {
            if (copiedStorageData == null) {
                copiedStorageData = DirectoryStorageData.setStateAndCloneIfNeed(componentName, fileName, subState, originalStorageData);
                if (copiedStorageData != null && fileName != null) {
                    dirtyFileNames.add(fileName);
                }
            }
            else if (copiedStorageData.setState(componentName, fileName, subState) != null && fileName != null) {
                dirtyFileNames.add(fileName);
            }
        }

        @Override
        
        public SaveSession createSaveSession() {
            return storage.checkIsSavingDisabled() || copiedStorageData == null ? null : this;
        }

        @Override
        public void save() throws IOException {
            VirtualFile dir = storage.getVirtualFile();
            if (copiedStorageData.isEmpty()) {
                if (dir != null && dir.exists()) {
                    StorageUtil.deleteFile(this, dir);
                }
                storage.myStorageData = copiedStorageData;
                return;
            }

            if (dir == null || !dir.isValid()) {
                dir = createDir(storage.myDir, this);
            }

            if (!dirtyFileNames.isEmpty()) {
                saveStates(dir);
            }
            if (dir.exists() && !removedFileNames.isEmpty()) {
                deleteFiles(dir);
            }

            storage.myVirtualFile = dir;
            storage.myStorageData = copiedStorageData;
        }

        private void saveStates( final VirtualFile dir) {
            final Element storeElement = new Element(StorageData.COMPONENT);

            for (final String componentName : copiedStorageData.getComponentNames()) {
                copiedStorageData.processComponent(componentName, new TObjectObjectProcedure<String, Object>() {
                    @Override
                    public boolean execute(String fileName, Object state) {
                        if (!dirtyFileNames.contains(fileName)) {
                            return true;
                        }

                        Element element = copiedStorageData.stateToElement(fileName, state);
                        if (storage.myPathMacroSubstitutor != null) {
                            storage.myPathMacroSubstitutor.collapsePaths(element);
                        }

                        try {
                            storeElement.setAttribute(StorageData.NAME, componentName);
                            storeElement.addContent(element);

                            BufferExposingByteArrayOutputStream byteOut;
                            VirtualFile file = getFile(fileName, dir, MySaveSession.this);
                            if (file.exists()) {
                                byteOut = StorageUtil.writeToBytes(storeElement, StorageUtil.loadFile(file).second);
                            }
                            else {
                                byteOut = StorageUtil.writeToBytes(storeElement, SystemProperties.getLineSeparator());
                            }
                            StorageUtil.writeFile(null, MySaveSession.this, file, byteOut, null);
                        }
                        catch (IOException e) {
                            LOG.error(e);
                        }
                        finally {
                            element.detach();
                        }
                        return true;
                    }
                });
            }
        }

        private void deleteFiles( VirtualFile dir) throws IOException {
//            AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
            try {
                for (VirtualFile file : dir.getChildren()) {
                    if (removedFileNames.contains(file.getName())) {
                        try {
                            file.delete(this);
                        }
                        catch (FileNotFoundException e) {
//                            throw new ReadOnlyModificationException(file, e, null);
                        }
                    }
                }
            }
            finally {
//                token.finish();
            }
        }
    }
}
