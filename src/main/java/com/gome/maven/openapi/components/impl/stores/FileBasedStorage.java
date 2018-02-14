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

import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.tracker.VirtualFileTracker;
import com.gome.maven.util.LineSeparator;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Set;

public class FileBasedStorage extends XmlElementStorage {
    private final String myFilePath;
    private final File myFile;
    private volatile VirtualFile myCachedVirtualFile;
    private LineSeparator myLineSeparator;

    public FileBasedStorage( String filePath,
                             String fileSpec,
                             RoamingType roamingType,
                             TrackingPathMacroSubstitutor pathMacroManager,
                             String rootElementName,
                             Disposable parentDisposable,
                             final Listener listener,
                             StreamProvider streamProvider) {
        super(fileSpec, roamingType, pathMacroManager, rootElementName, streamProvider);

        myFilePath = filePath;
        myFile = new File(filePath);

        if (listener != null) {
            VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
            if (virtualFileTracker != null) {
                virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + myFile.getAbsolutePath().replace(File.separatorChar, '/'), new VirtualFileAdapter() {
                    @Override
                    public void fileMoved( VirtualFileMoveEvent event) {
                        myCachedVirtualFile = null;
                    }

                    @Override
                    public void fileDeleted( VirtualFileEvent event) {
                        myCachedVirtualFile = null;
                    }

                    @Override
                    public void fileCreated( VirtualFileEvent event) {
                        myCachedVirtualFile = event.getFile();
                    }

                    @Override
                    public void contentsChanged( final VirtualFileEvent event) {
                        listener.storageFileChanged(event, FileBasedStorage.this);
                    }
                }, false, parentDisposable);
            }
        }
    }

    protected boolean isUseXmlProlog() {
        return false;
    }

    protected boolean isUseLfLineSeparatorByDefault() {
        return isUseXmlProlog();
    }

    
    @Override
    protected XmlElementStorageSaveSession createSaveSession( StorageData storageData) {
        return new FileSaveSession(storageData);
    }

    public void forceSave() {
        XmlElementStorageSaveSession externalizationSession = startExternalization();
        if (externalizationSession != null) {
            externalizationSession.forceSave();
        }
    }

    private class FileSaveSession extends XmlElementStorageSaveSession {
        protected FileSaveSession( StorageData storageData) {
            super(storageData);
        }

        @Override
        protected void doSave( Element element) throws IOException {
            if (myLineSeparator == null) {
                myLineSeparator = isUseLfLineSeparatorByDefault() ? LineSeparator.LF : LineSeparator.getSystemLineSeparator();
            }

            BufferExposingByteArrayOutputStream content = element == null ? null : StorageUtil.writeToBytes(element, myLineSeparator.getSeparatorString());
            if (ApplicationManager.getApplication().isUnitTestMode() && StringUtil.startsWithChar(myFile.getPath(), '$')) {
                throw new StateStorageException("It seems like some macros were not expanded for path: " + myFile.getPath());
            }

            try {
                if (myStreamProvider != null && myStreamProvider.isEnabled()) {
                    // stream provider always use LF separator
                    saveForProvider(myLineSeparator == LineSeparator.LF ? content : null, element);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }

            if (LOG.isDebugEnabled() && myFileSpec.equals(StoragePathMacros.MODULE_FILE)) {
                LOG.debug("doSave " + getFilePath());
            }

            if (content == null) {
                StorageUtil.deleteFile(myFile, this, getVirtualFile());
                myCachedVirtualFile = null;
            }
            else {
                VirtualFile file = getVirtualFile();
                if (file == null || !file.exists()) {
                    FileUtil.createParentDirs(myFile);
                    file = null;
                }
                myCachedVirtualFile = StorageUtil.writeFile(myFile, this, file, content, isUseXmlProlog() ? myLineSeparator : null);
            }
        }
    }

    @Override
    
    protected StorageData createStorageData() {
        return new StorageData(myRootElementName);
    }

    
    public VirtualFile getVirtualFile() {
        VirtualFile virtualFile = myCachedVirtualFile;
        if (virtualFile == null) {
            myCachedVirtualFile = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myFile);
        }
        return virtualFile;
    }

    
    public File getFile() {
        return myFile;
    }

    
    public String getFilePath() {
        return myFilePath;
    }

    @Override
    
    protected Element loadLocalData() {
        myBlockSavingTheContent = false;
        try {
            VirtualFile file = getVirtualFile();
            if (file == null || file.isDirectory() || !file.isValid()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Document was not loaded for " + myFileSpec + " file is " + (file == null ? "null" : "directory"));
                }
                return null;
            }
            if (file.getLength() == 0) {
                return processReadException(null);
            }

            CharBuffer charBuffer = CharsetToolkit.UTF8_CHARSET.decode(ByteBuffer.wrap(file.contentsToByteArray()));
            myLineSeparator = StorageUtil.detectLineSeparators(charBuffer, isUseLfLineSeparatorByDefault() ? null : LineSeparator.LF);
            return JDOMUtil.loadDocument(charBuffer).detachRootElement();
        }
        catch (JDOMException e) {
            return processReadException(e);
        }
        catch (IOException e) {
            return processReadException(e);
        }
    }

    
    private Element processReadException( Exception e) {
        boolean contentTruncated = e == null;
        myBlockSavingTheContent = !contentTruncated && (StorageUtil.isProjectOrModuleFile(myFileSpec) || myFileSpec.equals(StoragePathMacros.WORKSPACE_FILE));
        if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
            if (e != null) {
                LOG.info(e);
            }
            new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Load Settings",
                    "Cannot load settings from file '" +
                            myFile.getPath() + "': " +
                            (e == null ? "content truncated" : e.getMessage()) + "\n" +
                            (myBlockSavingTheContent ? "Please correct the file content" : "File content will be recreated"),
                    NotificationType.WARNING).notify(null);
        }
        return null;
    }

    @Override
    public void setDefaultState( Element element) {
        element.setName(myRootElementName);
        super.setDefaultState(element);
    }

    @SuppressWarnings("unused")
    public void updatedFromStreamProvider( Set<String> changedComponentNames, boolean deleted) {
        if (myRoamingType == RoamingType.DISABLED) {
            // storage roaming was changed to DISABLED, but settings repository has old state
            return;
        }

        try {
            Element newElement = deleted ? null : loadDataFromStreamProvider();
            if (newElement == null) {
                StorageUtil.deleteFile(myFile, this, myCachedVirtualFile);
                // if data was loaded, mark as changed all loaded components
                if (myStorageData != null) {
                    changedComponentNames.addAll(myStorageData.getComponentNames());
                    myStorageData = null;
                }
            }
            else if (myStorageData != null) {
                StorageData newStorageData = createStorageData();
                loadState(newStorageData, newElement);
                changedComponentNames.addAll(myStorageData.getChangedComponentNames(newStorageData, myPathMacroSubstitutor));
                myStorageData = newStorageData;
            }
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @Override
    public String toString() {
        return getFilePath();
    }
}
