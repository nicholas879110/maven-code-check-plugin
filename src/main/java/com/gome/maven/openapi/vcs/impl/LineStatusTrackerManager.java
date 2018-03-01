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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 31.07.2006
 * Time: 13:24:17
 */
package com.gome.maven.openapi.vcs.impl;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.ProjectComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.colors.EditorColorsListener;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.event.EditorFactoryAdapter;
import com.gome.maven.openapi.editor.event.EditorFactoryEvent;
import com.gome.maven.openapi.editor.event.EditorFactoryListener;
import com.gome.maven.openapi.editor.ex.DocumentBulkUpdateListener;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.impl.DirectoryIndex;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.ex.LineStatusTracker;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileAdapter;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.concurrency.QueueProcessorRemovePartner;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashMap;

import java.util.Map;

public class LineStatusTrackerManager implements ProjectComponent, LineStatusTrackerManagerI {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.impl.LineStatusTrackerManager");

     protected static final String IGNORE_CHANGEMARKERS_KEY = "idea.ignore.changemarkers";

     public final Object myLock = new Object();

     private final Project myProject;
     private final ProjectLevelVcsManager myVcsManager;
     private final VcsBaseContentProvider myStatusProvider;
     private final Application myApplication;
     private final FileEditorManager myFileEditorManager;
     private final Disposable myDisposable;

     private final Map<Document, LineStatusTracker> myLineStatusTrackers;

     private final QueueProcessorRemovePartner<Document, BaseRevisionLoader> myPartner;
    private long myLoadCounter;

    public static LineStatusTrackerManagerI getInstance(final Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetComponent(project, LineStatusTrackerManagerI.class);
    }

    public LineStatusTrackerManager( final Project project,
                                     final ProjectLevelVcsManager vcsManager,
                                     final VcsBaseContentProvider statusProvider,
                                     final Application application,
                                     final FileEditorManager fileEditorManager,
                                    @SuppressWarnings("UnusedParameters") DirectoryIndex makeSureIndexIsInitializedFirst) {
        myLoadCounter = 0;
        myProject = project;
        myVcsManager = vcsManager;
        myStatusProvider = statusProvider;
        myApplication = application;
        myFileEditorManager = fileEditorManager;

        myLineStatusTrackers = new HashMap<Document, LineStatusTracker>();
        myPartner = new QueueProcessorRemovePartner<Document, BaseRevisionLoader>(myProject, new Consumer<BaseRevisionLoader>() {
            @Override
            public void consume(BaseRevisionLoader baseRevisionLoader) {
                baseRevisionLoader.run();
            }
        });

        project.getMessageBus().connect().subscribe(DocumentBulkUpdateListener.TOPIC, new DocumentBulkUpdateListener.Adapter() {
            public void updateStarted( final Document doc) {
                final LineStatusTracker tracker = getLineStatusTracker(doc);
                if (tracker != null) tracker.startBulkUpdate();
            }

            public void updateFinished( final Document doc) {
                final LineStatusTracker tracker = getLineStatusTracker(doc);
                if (tracker != null) tracker.finishBulkUpdate();
            }
        });

        myDisposable = new Disposable() {
            @Override
            public void dispose() {
                synchronized (myLock) {
                    for (final LineStatusTracker tracker : myLineStatusTrackers.values()) {
                        tracker.release();
                    }

                    myLineStatusTrackers.clear();
                    myPartner.clear();
                }
            }
        };
        Disposer.register(myProject, myDisposable);
    }

    public void projectOpened() {
        StartupManager.getInstance(myProject).registerPreStartupActivity(new Runnable() {
            @Override
            public void run() {
                final MyFileStatusListener fileStatusListener = new MyFileStatusListener();
                final EditorFactoryListener editorFactoryListener = new MyEditorFactoryListener();
                final MyVirtualFileListener virtualFileListener = new MyVirtualFileListener();
                final EditorColorsListener editorColorsListener = new MyEditorColorsListener();

                final FileStatusManager fsManager = FileStatusManager.getInstance(myProject);
                fsManager.addFileStatusListener(fileStatusListener, myDisposable);

                final EditorFactory editorFactory = EditorFactory.getInstance();
                editorFactory.addEditorFactoryListener(editorFactoryListener, myDisposable);

                final VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
                virtualFileManager.addVirtualFileListener(virtualFileListener, myDisposable);

                final EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
                editorColorsManager.addEditorColorsListener(editorColorsListener, myDisposable);
            }
        });
    }

    public void projectClosed() {
    }

    
    
    public String getComponentName() {
        return "LineStatusTrackerManager";
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public boolean isDisabled() {
        return !myProject.isOpen() || myProject.isDisposed();
    }

    @Override
    public LineStatusTracker getLineStatusTracker(final Document document) {
        synchronized (myLock) {
            if (isDisabled()) return null;

            return myLineStatusTrackers.get(document);
        }
    }

    private void resetTrackers() {
        synchronized (myLock) {
            if (isDisabled()) return;

            if (LOG.isDebugEnabled()) {
                LOG.debug("resetTrackers");
            }

            for (LineStatusTracker tracker : ContainerUtil.newArrayList(myLineStatusTrackers.values())) {
                resetTracker(tracker.getDocument(), tracker.getVirtualFile(), tracker);
            }

            final VirtualFile[] openFiles = myFileEditorManager.getOpenFiles();
            for (final VirtualFile openFile : openFiles) {
                resetTracker(openFile, true);
            }
        }
    }

    private void resetTracker( final VirtualFile virtualFile) {
        resetTracker(virtualFile, false);
    }

    private void resetTracker( final VirtualFile virtualFile, boolean insertOnly) {
        final Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
        if (document == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("resetTracker: no cached document for " + virtualFile.getPath());
            }
            return;
        }

        synchronized (myLock) {
            if (isDisabled()) return;

            final LineStatusTracker tracker = myLineStatusTrackers.get(document);
            if (insertOnly && tracker != null) return;
            resetTracker(document, virtualFile, tracker);
        }
    }

    private void resetTracker( Document document,  VirtualFile virtualFile,  LineStatusTracker tracker) {
        final boolean editorOpened = myFileEditorManager.isFileOpen(virtualFile);
        final boolean shouldBeInstalled = editorOpened && shouldBeInstalled(virtualFile);

        if (LOG.isDebugEnabled()) {
            LOG.debug("resetTracker: shouldBeInstalled - " + shouldBeInstalled + ", tracker - " + (tracker == null ? "null" : "found"));
        }

        if (tracker != null && shouldBeInstalled) {
            refreshTracker(tracker);
        }
        else if (tracker != null) {
            releaseTracker(document);
        }
        else if (shouldBeInstalled) {
            installTracker(virtualFile, document);
        }
    }

    private boolean shouldBeInstalled( final VirtualFile virtualFile) {
        if (isDisabled()) return false;

        if (virtualFile == null || virtualFile instanceof LightVirtualFile) return false;
        if (!virtualFile.isInLocalFileSystem()) return false;
        final FileStatusManager statusManager = FileStatusManager.getInstance(myProject);
        if (statusManager == null) return false;
        final AbstractVcs activeVcs = myVcsManager.getVcsFor(virtualFile);
        if (activeVcs == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("shouldBeInstalled: for file " + virtualFile.getPath() + " failed: no active VCS");
            }
            return false;
        }
        final FileStatus status = statusManager.getStatus(virtualFile);
        if (status == FileStatus.NOT_CHANGED || status == FileStatus.ADDED || status == FileStatus.UNKNOWN || status == FileStatus.IGNORED) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("shouldBeInstalled: for file " + virtualFile.getPath() + " skipped: status=" + status);
            }
            return false;
        }
        return true;
    }

    private void refreshTracker( LineStatusTracker tracker) {
        synchronized (myLock) {
            if (isDisabled()) return;

            startAlarm(tracker.getDocument(), tracker.getVirtualFile());
        }
    }

    private void releaseTracker( final Document document) {
        synchronized (myLock) {
            if (isDisabled()) return;

            myPartner.remove(document);
            final LineStatusTracker tracker = myLineStatusTrackers.remove(document);
            if (tracker != null) {
                tracker.release();
            }
        }
    }

    private void installTracker( final VirtualFile virtualFile,  final Document document) {
        synchronized (myLock) {
            if (isDisabled()) return;

            if (myLineStatusTrackers.containsKey(document)) return;
            assert !myPartner.containsKey(document);

            final LineStatusTracker tracker = LineStatusTracker.createOn(virtualFile, document, myProject);
            myLineStatusTrackers.put(document, tracker);

            startAlarm(document, virtualFile);
        }
    }

    private void startAlarm( final Document document,  final VirtualFile virtualFile) {
        synchronized (myLock) {
            myPartner.add(document, new BaseRevisionLoader(document, virtualFile));
        }
    }

    private class BaseRevisionLoader implements Runnable {
         private final VirtualFile myVirtualFile;
         private final Document myDocument;

        private BaseRevisionLoader( final Document document,  final VirtualFile virtualFile) {
            myDocument = document;
            myVirtualFile = virtualFile;
        }

        @Override
        public void run() {
            if (isDisabled()) return;

            if (!myVirtualFile.isValid()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("BaseRevisionLoader: for file " + myVirtualFile.getPath() + " failed: virtual file not valid");
                }
                reportTrackerBaseLoadFailed();
                return;
            }

            final Pair<VcsRevisionNumber, String> baseRevision = myStatusProvider.getBaseRevision(myVirtualFile);
            if (baseRevision == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("BaseRevisionLoader: for file " + myVirtualFile.getPath() + " failed: null returned for base revision");
                }
                reportTrackerBaseLoadFailed();
                return;
            }

            // loads are sequential (in single threaded QueueProcessor);
            // so myLoadCounter can't take less value for greater base revision -> the only thing we want from it
            final LineStatusTracker.RevisionPack revisionPack = new LineStatusTracker.RevisionPack(myLoadCounter, baseRevision.first);
            myLoadCounter++;

            final String converted = StringUtil.convertLineSeparators(baseRevision.second);
            final Runnable runnable = new Runnable() {
                public void run() {
                    synchronized (myLock) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("BaseRevisionLoader: initializing tracker for file " + myVirtualFile.getPath());
                        }
                        final LineStatusTracker tracker = myLineStatusTrackers.get(myDocument);
                        if (tracker != null) {
                            tracker.initialize(converted, revisionPack);
                        }
                    }
                }
            };
            nonModalAliveInvokeLater(runnable);
        }

        private void nonModalAliveInvokeLater( Runnable runnable) {
            myApplication.invokeLater(runnable, ModalityState.NON_MODAL, new Condition() {
                @Override
                public boolean value(final Object ignore) {
                    return isDisabled();
                }
            });
        }

        private void reportTrackerBaseLoadFailed() {
            synchronized (myLock) {
                releaseTracker(myDocument);
            }
        }
    }

    private class MyFileStatusListener implements FileStatusListener {
        public void fileStatusesChanged() {
            resetTrackers();
        }

        public void fileStatusChanged( VirtualFile virtualFile) {
            resetTracker(virtualFile);
        }
    }

    private class MyEditorFactoryListener extends EditorFactoryAdapter {
        public void editorCreated( EditorFactoryEvent event) {
            // note that in case of lazy loading of configurables, this event can happen
            // outside of EDT, so the EDT check mustn't be done here
            Editor editor = event.getEditor();
            if (editor.getProject() != null && editor.getProject() != myProject) return;
            final Document document = editor.getDocument();
            final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (virtualFile == null) return;
            if (shouldBeInstalled(virtualFile)) {
                installTracker(virtualFile, document);
            }
        }

        public void editorReleased( EditorFactoryEvent event) {
            final Editor editor = event.getEditor();
            if (editor.getProject() != null && editor.getProject() != myProject) return;
            final Document doc = editor.getDocument();
            final Editor[] editors = event.getFactory().getEditors(doc, myProject);
            if (editors.length == 0) {
                releaseTracker(doc);
            }
        }
    }

    private class MyVirtualFileListener extends VirtualFileAdapter {
        public void beforeContentsChange( VirtualFileEvent event) {
            if (event.isFromRefresh()) {
                resetTracker(event.getFile());
            }
        }
    }

    private class MyEditorColorsListener implements EditorColorsListener {
        public void globalSchemeChange(EditorColorsScheme scheme) {
            resetTrackers();
        }
    }
}
