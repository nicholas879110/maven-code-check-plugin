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
package com.gome.maven.ide;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.application.impl.LaterInvocator;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ex.ProjectManagerEx;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.ManagingFS;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.openapi.vfs.newvfs.RefreshQueue;
import com.gome.maven.openapi.vfs.newvfs.RefreshSession;
import com.gome.maven.util.SingleAlarm;
import com.gome.maven.util.containers.ContainerUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class SaveAndSyncHandlerImpl extends SaveAndSyncHandler implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(SaveAndSyncHandler.class);

    private final Runnable myIdleListener;
    private final PropertyChangeListener myGeneralSettingsListener;
    private final GeneralSettings mySettings;
    private final ProgressManager myProgressManager;
    private final SingleAlarm myRefreshDelayAlarm;

    private final AtomicInteger myBlockSaveOnFrameDeactivationCount = new AtomicInteger();
    private final AtomicInteger myBlockSyncOnFrameActivationCount = new AtomicInteger();
    private volatile long myRefreshSessionId = 0;

    public SaveAndSyncHandlerImpl( GeneralSettings generalSettings,
                                   ProgressManager progressManager,
                                   FrameStateManager frameStateManager,
                                   final FileDocumentManager fileDocumentManager) {
        mySettings = generalSettings;
        myProgressManager = progressManager;

        myIdleListener = new Runnable() {
            @Override
            public void run() {
                if (mySettings.isAutoSaveIfInactive() && canSyncOrSave()) {
                    ((FileDocumentManagerImpl)fileDocumentManager).saveAllDocuments(false);
                }
            }
        };
        IdeEventQueue.getInstance().addIdleListener(myIdleListener, mySettings.getInactiveTimeout() * 1000);

        myGeneralSettingsListener = new PropertyChangeListener() {
            @Override
            public void propertyChange( PropertyChangeEvent e) {
                if (GeneralSettings.PROP_INACTIVE_TIMEOUT.equals(e.getPropertyName())) {
                    IdeEventQueue eventQueue = IdeEventQueue.getInstance();
                    eventQueue.removeIdleListener(myIdleListener);
                    Integer timeout = (Integer)e.getNewValue();
                    eventQueue.addIdleListener(myIdleListener, timeout.intValue() * 1000);
                }
            }
        };
        mySettings.addPropertyChangeListener(myGeneralSettingsListener);

        myRefreshDelayAlarm = new SingleAlarm(new Runnable() {
            @Override
            public void run() {
                if (canSyncOrSave()) {
                    refreshOpenFiles();
                }
                maybeRefresh(ModalityState.NON_MODAL);
            }
        }, 300);

        frameStateManager.addListener(new FrameStateListener() {
            @Override
            public void onFrameDeactivated() {
                if (canSyncOrSave()) {
                    saveProjectsAndDocuments();
                }
            }

            @Override
            public void onFrameActivated() {
                if (!ApplicationManager.getApplication().isDisposed() && mySettings.isSyncOnFrameActivation()) {
                    scheduleRefresh();
                }
            }
        });
    }

    @Override
    
    public String getComponentName() {
        return "SaveAndSyncHandler";
    }

    @Override
    public void initComponent() { }

    @Override
    public void disposeComponent() {
        myRefreshDelayAlarm.cancel();
        RefreshQueue.getInstance().cancelSession(myRefreshSessionId);
        mySettings.removePropertyChangeListener(myGeneralSettingsListener);
        IdeEventQueue.getInstance().removeIdleListener(myIdleListener);
    }

    private boolean canSyncOrSave() {
        return !LaterInvocator.isInModalContext() && !myProgressManager.hasModalProgressIndicator();
    }

    @Override
    public void saveProjectsAndDocuments() {
        LOG.debug("enter: save()");

        if (!ApplicationManager.getApplication().isDisposed() &&
                mySettings.isSaveOnFrameDeactivation() &&
                myBlockSaveOnFrameDeactivationCount.get() == 0) {
            FileDocumentManager.getInstance().saveAllDocuments();

            for (Project project : ProjectManagerEx.getInstanceEx().getOpenProjects()) {
                if (LOG.isDebugEnabled()) LOG.debug("saving project: " + project);
                project.save();
            }

            LOG.debug("saving application settings");
            ApplicationManagerEx.getApplicationEx().saveSettings();

            LOG.debug("exit: save()");
        }
    }

    @Override
    public void scheduleRefresh() {
        myRefreshDelayAlarm.cancelAndRequest();
    }

    public void maybeRefresh( ModalityState modalityState) {
        if (myBlockSyncOnFrameActivationCount.get() == 0 && mySettings.isSyncOnFrameActivation()) {
            RefreshQueue queue = RefreshQueue.getInstance();
            queue.cancelSession(myRefreshSessionId);

            RefreshSession session = queue.createSession(true, true, null, modalityState);
            session.addAllFiles(ManagingFS.getInstance().getLocalRoots());
            myRefreshSessionId = session.getId();
            session.launch();
        }
    }

    @Override
    public void refreshOpenFiles() {
        List<VirtualFile> files = ContainerUtil.newArrayList();

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            for (VirtualFile file : FileEditorManager.getInstance(project).getSelectedFiles()) {
                if (file instanceof NewVirtualFile) {
                    files.add(file);
                }
            }
        }

        if (!files.isEmpty()) {
            // refresh open files synchronously so it doesn't wait for potentially longish refresh request in the queue to finish
            RefreshQueue.getInstance().refresh(false, false, null, files);
        }
    }

    @Override
    public void blockSaveOnFrameDeactivation() {
        myBlockSaveOnFrameDeactivationCount.incrementAndGet();
    }

    @Override
    public void unblockSaveOnFrameDeactivation() {
        myBlockSaveOnFrameDeactivationCount.decrementAndGet();
    }

    @Override
    public void blockSyncOnFrameActivation() {
        myBlockSyncOnFrameActivationCount.incrementAndGet();
    }

    @Override
    public void unblockSyncOnFrameActivation() {
        myBlockSyncOnFrameActivationCount.decrementAndGet();
    }
}
