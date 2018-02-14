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
package com.gome.maven.openapi.project;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.caches.CacheUpdater;
import com.gome.maven.ide.caches.FileContent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationAdapter;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.util.ProgressIndicatorBase;
import com.gome.maven.openapi.progress.util.ProgressWrapper;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;
import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheUpdateRunner extends DumbModeTask {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.project.CacheUpdateRunner");
    private static final Key<Boolean> FAILED_TO_INDEX = Key.create("FAILED_TO_INDEX");
    private static final int PROC_COUNT = Runtime.getRuntime().availableProcessors();
    private final Project myProject;
    private final Collection<CacheUpdater> myUpdaters;
    private CacheUpdateSession mySession;

    CacheUpdateRunner( Project project,  Collection<CacheUpdater> updaters) {
        myProject = project;
        myUpdaters = updaters;
    }

    @Override
    public String toString() {
        return new ArrayList<CacheUpdater>(myUpdaters).toString();
    }

    private int queryNeededFiles( ProgressIndicator indicator) {
        // can be queried twice in DumbService
        return getSession(indicator).getFilesToUpdate().size();
    }

    
    private CacheUpdateSession getSession( ProgressIndicator indicator) {
        CacheUpdateSession session = mySession;
        if (session == null) {
            mySession = session = new CacheUpdateSession(myUpdaters, indicator);
        }
        return session;
    }

    private void processFiles( final ProgressIndicator indicator, boolean processInReadAction) {
        try {
            Collection<VirtualFile> files = mySession.getFilesToUpdate();

            processFiles(indicator, processInReadAction, files, myProject, new Consumer<FileContent>() {
                @Override
                public void consume(FileContent content) {
                    mySession.processFile(content);
                }
            });
        }
        catch (ProcessCanceledException e) {
            mySession.canceled();
            throw e;
        }
    }

    private static final int FILE_SIZE_TO_SHOW_THRESHOLD = 500 * 1024;

    public static void processFiles(final ProgressIndicator indicator,
                                    boolean processInReadAction,
                                    Collection<VirtualFile> files,
                                    Project project, Consumer<FileContent> processor) {
        indicator.checkCanceled();
        final FileContentQueue queue = new FileContentQueue();
        final double total = files.size();
        queue.queue(files, indicator);

        Consumer<VirtualFile> progressUpdater = new Consumer<VirtualFile>() {
            // need set here to handle queue.pushbacks after checkCancelled() in order
            // not to count the same file several times
            final Set<VirtualFile> processed = new THashSet<VirtualFile>();
            private boolean fileNameWasShown;

            @Override
            public void consume(VirtualFile virtualFile) {
                indicator.checkCanceled();
                synchronized (processed) {
                    boolean added = processed.add(virtualFile);
                    indicator.setFraction(processed.size() / total);
                    if (!added || (virtualFile.isValid() && virtualFile.getLength() > FILE_SIZE_TO_SHOW_THRESHOLD)) {
                        indicator.setText2(virtualFile.getPresentableUrl());
                        fileNameWasShown = true;
                    } else if (fileNameWasShown) {
                        indicator.setText2("");
                        fileNameWasShown = false;
                    }
                }
            }
        };

        while (!project.isDisposed()) {
            indicator.checkCanceled();
            // todo wait for the user...
            if (processSomeFilesWhileUserIsInactive(queue, progressUpdater, processInReadAction, project, processor)) {
                break;
            }
        }

        if (project.isDisposed()) {
            indicator.cancel();
            indicator.checkCanceled();
        }
    }

    private void updatingDone() {
        try {
            mySession.updatingDone();
        }
        catch (ProcessCanceledException e) {
            mySession.canceled();
            throw e;
        }
    }

    private static boolean processSomeFilesWhileUserIsInactive( FileContentQueue queue,
                                                                Consumer<VirtualFile> progressUpdater,
                                                               final boolean processInReadAction,
                                                                Project project,
                                                                Consumer<FileContent> fileProcessor) {
        final ProgressIndicatorBase innerIndicator = new ProgressIndicatorBase() {
            @Override
            protected boolean isCancelable() {
                return true; // the inner indicator must be always cancelable
            }
        };
        final ApplicationAdapter canceller = new ApplicationAdapter() {
            @Override
            public void beforeWriteActionStart(Object action) {
                innerIndicator.cancel();
            }
        };
        final Application application = ApplicationManager.getApplication();
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                application.addApplicationListener(canceller);
            }
        }, ModalityState.any());

        final AtomicBoolean isFinished = new AtomicBoolean();
        try {
            int threadsCount = Registry.intValue("caches.indexerThreadsCount");
            if (threadsCount <= 0) {
                threadsCount = Math.max(1, Math.min(PROC_COUNT - 1, 4));
            }
            if (threadsCount == 1 || application.isWriteAccessAllowed()) {
                Runnable process = new MyRunnable(innerIndicator, queue, isFinished, progressUpdater, processInReadAction, project, fileProcessor);
                ProgressManager.getInstance().runProcess(process, innerIndicator);
            }
            else {
                AtomicBoolean[] finishedRefs = new AtomicBoolean[threadsCount];
                Future<?>[] futures = new Future<?>[threadsCount];
                for (int i = 0; i < threadsCount; i++) {
                    AtomicBoolean ref = new AtomicBoolean();
                    finishedRefs[i] = ref;
                    Runnable process = new MyRunnable(innerIndicator, queue, ref, progressUpdater, processInReadAction, project, fileProcessor);
                    futures[i] = ApplicationManager.getApplication().executeOnPooledThread(process);
                }
                isFinished.set(waitForAll(finishedRefs, futures));
            }
        }
        finally {
            application.removeApplicationListener(canceller);
        }

        return isFinished.get();
    }

    private static boolean waitForAll( AtomicBoolean[] finishedRefs,  Future<?>[] futures) {
        assert !ApplicationManager.getApplication().isWriteAccessAllowed();
        try {
            for (Future<?> future : futures) {
                future.get();
            }

            boolean allFinished = true;
            for (AtomicBoolean ref : finishedRefs) {
                if (!ref.get()) {
                    allFinished = false;
                    break;
                }
            }
            return allFinished;
        }
        catch (InterruptedException ignored) {
        }
        catch (Throwable throwable) {
            LOG.error(throwable);
        }
        return false;
    }

    @Override
    public void performInDumbMode( ProgressIndicator indicator) {
        indicator.checkCanceled();
        indicator.setIndeterminate(true);
        indicator.setText(IdeBundle.message("progress.indexing.scanning"));
        int count = queryNeededFiles(indicator);

        indicator.setIndeterminate(false);
        indicator.setText(IdeBundle.message("progress.indexing.updating"));
        if (count > 0) {
            processFiles(indicator, true);
        }
        updatingDone();
    }

    private static class MyRunnable implements Runnable {
        private final ProgressIndicatorBase myInnerIndicator;
        private final FileContentQueue myQueue;
        private final AtomicBoolean myFinished;
        private final Consumer<VirtualFile> myProgressUpdater;
        private final boolean myProcessInReadAction;
         private final Project myProject;
         private final Consumer<FileContent> myProcessor;

        public MyRunnable( ProgressIndicatorBase innerIndicator,
                           FileContentQueue queue,
                           AtomicBoolean finished,
                           Consumer<VirtualFile> progressUpdater,
                          boolean processInReadAction,
                           Project project,
                           Consumer<FileContent> fileProcessor) {
            myInnerIndicator = innerIndicator;
            myQueue = queue;
            myFinished = finished;
            myProgressUpdater = progressUpdater;
            myProcessInReadAction = processInReadAction;
            myProject = project;
            myProcessor = fileProcessor;
        }

        @Override
        public void run() {
            while (true) {
                if (myProject.isDisposed() || myInnerIndicator.isCanceled()) {
                    return;
                }
                try {
                    final FileContent fileContent = myQueue.take(myInnerIndicator);
                    if (fileContent == null) {
                        myFinished.set(true);
                        return;
                    }

                    final Runnable action = new Runnable() {
                        @Override
                        public void run() {
                            myInnerIndicator.checkCanceled();
                            if (!myProject.isDisposed()) {
                                final VirtualFile file = fileContent.getVirtualFile();
                                try {
                                    myProgressUpdater.consume(file);
                                    if (file.isValid() && !file.isDirectory() && !Boolean.TRUE.equals(file.getUserData(FAILED_TO_INDEX))) {
                                        myProcessor.consume(fileContent);
                                    }
                                }
                                catch (ProcessCanceledException e) {
                                    throw e;
                                }
                                catch (Throwable e) {
                                    LOG.error("Error while indexing " + file.getPresentableUrl() + "\n" + "To reindex this file IDEA has to be restarted", e);
                                    file.putUserData(FAILED_TO_INDEX, Boolean.TRUE);
                                }
                            }
                        }
                    };
                    try {
                        ProgressManager.getInstance().runProcess(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (myProcessInReadAction) {
                                            // in wait methods we don't want to deadlock by grabbing write lock (or having it in queue) and trying to run read action in separate thread
                                            if (!ApplicationManagerEx.getApplicationEx().tryRunReadAction(action)) {
                                                throw new ProcessCanceledException();
                                            }
                                        }
                                        else {
                                            action.run();
                                        }
                                    }
                                },
                                ProgressWrapper.wrap(myInnerIndicator)
                        );
                    }
                    catch (ProcessCanceledException e) {
                        myQueue.pushback(fileContent);
                        return;
                    }
                    finally {
                        myQueue.release(fileContent);
                    }
                }
                catch (ProcessCanceledException e) {
                    return;
                }
            }
        }
    }
}
