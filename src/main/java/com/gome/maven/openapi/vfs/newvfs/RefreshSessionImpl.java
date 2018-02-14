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

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.ex.VirtualFileManagerEx;
import com.gome.maven.openapi.vfs.impl.local.FileWatcher;
import com.gome.maven.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.openapi.vfs.newvfs.persistent.PersistentFS;
import com.gome.maven.openapi.vfs.newvfs.persistent.RefreshWorker;
import com.gome.maven.util.concurrency.Semaphore;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author max
 */
public class RefreshSessionImpl extends RefreshSession {
    private static final Logger LOG = Logger.getInstance(RefreshSession.class);

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private final long myId = ID_COUNTER.incrementAndGet();
    private final boolean myIsAsync;
    private final boolean myIsRecursive;
    private final Runnable myFinishRunnable;
    private final ModalityState myModalityState;
    private final Semaphore mySemaphore = new Semaphore();

    private List<VirtualFile> myWorkQueue = new ArrayList<VirtualFile>();
    private List<VFileEvent> myEvents = new ArrayList<VFileEvent>();
    private volatile boolean iHaveEventsToFire;
    private volatile RefreshWorker myWorker = null;
    private volatile boolean myCancelled = false;

    public RefreshSessionImpl(boolean async, boolean recursive,  Runnable finishRunnable) {
        this(async, recursive, finishRunnable, ModalityState.NON_MODAL);
    }

    public RefreshSessionImpl(boolean async, boolean recursive,  Runnable finishRunnable,  ModalityState modalityState) {
        myIsAsync = async;
        myIsRecursive = recursive;
        myFinishRunnable = finishRunnable;
        myModalityState = modalityState;
    }

    public RefreshSessionImpl( List<VFileEvent> events) {
        this(false, false, null, ModalityState.NON_MODAL);
        myEvents.addAll(events);
    }

    @Override
    public long getId() {
        return myId;
    }

    @Override
    public void addAllFiles( Collection<VirtualFile> files) {
        for (VirtualFile file : files) {
            if (file == null) {
                LOG.error("null passed among " + files);
            }
            else {
                myWorkQueue.add(file);
            }
        }
    }

    @Override
    public void addFile( VirtualFile file) {
        myWorkQueue.add(file);
    }

    @Override
    public boolean isAsynchronous() {
        return myIsAsync;
    }

    @Override
    public void launch() {
        mySemaphore.down();
        ((RefreshQueueImpl)RefreshQueue.getInstance()).execute(this);
    }

    public void scan() {
        List<VirtualFile> workQueue = myWorkQueue;
        myWorkQueue = new ArrayList<VirtualFile>();
        boolean haveEventsToFire = myFinishRunnable != null || !myEvents.isEmpty();

        if (!workQueue.isEmpty()) {
            LocalFileSystemImpl fs = (LocalFileSystemImpl)LocalFileSystem.getInstance();
            fs.markSuspiciousFilesDirty(workQueue);
            FileWatcher watcher = fs.getFileWatcher();

            long t = 0;
            if (LOG.isDebugEnabled()) {
                LOG.debug("scanning " + workQueue);
                t = System.currentTimeMillis();
            }

            for (VirtualFile file : workQueue) {
                if (myCancelled) break;

                NewVirtualFile nvf = (NewVirtualFile)file;
                if (!myIsRecursive && (!myIsAsync || !watcher.isWatched(nvf))) {
                    // we're unable to definitely refresh synchronously by means of file watcher.
                    nvf.markDirty();
                }

                RefreshWorker worker = myWorker = new RefreshWorker(nvf, myIsRecursive);
                worker.scan();
                List<VFileEvent> events = worker.getEvents();
                if (myEvents.addAll(events)) {
                    haveEventsToFire = true;
                }
            }

            if (t != 0) {
                t = System.currentTimeMillis() - t;
                LOG.debug((myCancelled ? "cancelled, " : "done, ") + t + " ms, events " + myEvents);
            }
        }

        myWorker = null;
        iHaveEventsToFire = haveEventsToFire;
    }

    public void cancel() {
        myCancelled = true;

        RefreshWorker worker = myWorker;
        if (worker != null) {
            worker.cancel();
        }
    }

    public void fireEvents(boolean hasWriteAction) {
        try {
            if (!iHaveEventsToFire || ApplicationManager.getApplication().isDisposed()) return;

            if (hasWriteAction) {
                fireEventsInWriteAction();
            }
            else {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        fireEventsInWriteAction();
                    }
                });
            }
        }
        finally {
            mySemaphore.up();
        }
    }

    protected void fireEventsInWriteAction() {
        final VirtualFileManagerEx manager = (VirtualFileManagerEx)VirtualFileManager.getInstance();

        manager.fireBeforeRefreshStart(myIsAsync);
        try {
            while (!myWorkQueue.isEmpty() || !myEvents.isEmpty()) {
                PersistentFS.getInstance().processEvents(mergeEventsAndReset());
                scan();
            }
        }
        finally {
            try {
                manager.fireAfterRefreshFinish(myIsAsync);
            }
            finally {
                if (myFinishRunnable != null) {
                    myFinishRunnable.run();
                }
            }
        }
    }

    public void waitFor() {
        mySemaphore.waitFor();
    }

    private List<VFileEvent> mergeEventsAndReset() {
        Set<VFileEvent> mergedEvents = new LinkedHashSet<VFileEvent>(myEvents);
        List<VFileEvent> events = new ArrayList<VFileEvent>(mergedEvents);
        myEvents = new ArrayList<VFileEvent>();
        return events;
    }

    
    public ModalityState getModalityState() {
        return myModalityState;
    }

    @Override
    public String toString() {
        return myWorkQueue.size() <= 1 ? "" : myWorkQueue.size() + " roots in queue.";
    }
}
