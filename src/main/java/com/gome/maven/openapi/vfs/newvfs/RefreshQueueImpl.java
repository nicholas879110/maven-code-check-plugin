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
package com.gome.maven.openapi.vfs.newvfs;

import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.diagnostic.FrequentEventDetector;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.vfs.VfsBundle;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.io.storage.HeavyProcessLatch;
import gnu.trove.TLongObjectHashMap;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * @author max
 */
public class RefreshQueueImpl extends RefreshQueue {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vfs.newvfs.RefreshQueueImpl");

    private final ExecutorService myQueue = ConcurrencyUtil.newSingleThreadExecutor("FS Synchronizer");
    private final ProgressIndicator myRefreshIndicator = RefreshProgress.create(VfsBundle.message("file.synchronize.progress"));
    private final TLongObjectHashMap<RefreshSession> mySessions = new TLongObjectHashMap<RefreshSession>();
    private final FrequentEventDetector myEventCounter = new FrequentEventDetector(100, 100, FrequentEventDetector.Level.ERROR);

    public void execute( RefreshSessionImpl session) {
        if (session.isAsynchronous()) {
            ModalityState state = session.getModalityState();
            queueSession(session, state);
        }
        else {
            Application app = ApplicationManager.getApplication();
            if (app.isDispatchThread()) {
                doScan(session);
                session.fireEvents(app.isWriteAccessAllowed());
            }
            else {
                if (((ApplicationEx)app).holdsReadLock()) {
                    LOG.error("Do not call synchronous refresh under read lock (except from EDT) - " +
                            "this will cause a deadlock if there are any events to fire.");
                    return;
                }
                queueSession(session, ModalityState.defaultModalityState());
                session.waitFor();
            }
        }
    }

    private void queueSession( final RefreshSessionImpl session,  final ModalityState modality) {
        myQueue.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    myRefreshIndicator.start();
                    AccessToken token = HeavyProcessLatch.INSTANCE.processStarted("Doing file refresh. " + session.toString());
                    try {
                        doScan(session);
                    }
                    finally {
                        token.finish();
                        myRefreshIndicator.stop();
                    }
                }
                finally {
                    ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
                        @Override
                        public void run() {
                            session.fireEvents(false);
                        }
                    }, modality);
                }
            }
        });
        myEventCounter.eventHappened();
    }

    private void doScan(RefreshSessionImpl session) {
        try {
            updateSessionMap(session, true);
            session.scan();
        }
        finally {
            updateSessionMap(session, false);
        }
    }

    private void updateSessionMap(RefreshSession session, boolean add) {
        long id = session.getId();
        if (id != 0) {
            synchronized (mySessions) {
                if (add) {
                    mySessions.put(id, session);
                }
                else {
                    mySessions.remove(id);
                }
            }
        }
    }

    @Override
    public void cancelSession(long id) {
        RefreshSession session;
        synchronized (mySessions) {
            session = mySessions.get(id);
        }
        if (session instanceof RefreshSessionImpl) {
            ((RefreshSessionImpl)session).cancel();
        }
    }

    
    @Override
    public RefreshSession createSession(boolean async, boolean recursively,  Runnable finishRunnable,  ModalityState state) {
        return new RefreshSessionImpl(async, recursively, finishRunnable, state);
    }

    @Override
    public void processSingleEvent( VFileEvent event) {
        new RefreshSessionImpl(Collections.singletonList(event)).launch();
    }

    public static boolean isRefreshInProgress() {
        RefreshQueueImpl refreshQueue = (RefreshQueueImpl)RefreshQueue.getInstance();
        synchronized (refreshQueue.mySessions) {
            return !refreshQueue.mySessions.isEmpty();
        }
    }
}