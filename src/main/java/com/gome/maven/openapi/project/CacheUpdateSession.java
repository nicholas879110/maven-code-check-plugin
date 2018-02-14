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

import com.gome.maven.ide.caches.CacheUpdater;
import com.gome.maven.ide.caches.FileContent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashSet;

import java.util.*;

public class CacheUpdateSession {
    private static final Logger LOG = Logger.getInstance("#" + CacheUpdateSession.class.getName());
    private final List<VirtualFile> myFilesToUpdate;
    private final int myJobsToDo;
    private final List<Pair<CacheUpdater, Collection<VirtualFile>>> myUpdatersWithFiles;

    CacheUpdateSession( Collection<CacheUpdater> updaters,  ProgressIndicator indicator) {
        List<CacheUpdater> processedUpdaters = new ArrayList<CacheUpdater>();

        LinkedHashSet<VirtualFile> set = ContainerUtil.newLinkedHashSet();
        List<Pair<CacheUpdater, Collection<VirtualFile>>> list = new ArrayList<Pair<CacheUpdater, Collection<VirtualFile>>>();
        try {
            int jobsCount = 0;
            for (CacheUpdater each : updaters) {
                indicator.checkCanceled();
                try {
                    jobsCount += each.getNumberOfPendingUpdateJobs();
                    List<VirtualFile> updaterFiles = Arrays.asList(each.queryNeededFiles(indicator));
                    processedUpdaters.add(each);
                    set.addAll(updaterFiles);
                    list.add(Pair.create(each, (Collection<VirtualFile>)new THashSet<VirtualFile>(updaterFiles)));
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
            myJobsToDo = jobsCount;
        }
        catch (ProcessCanceledException e) {
            for (CacheUpdater each : processedUpdaters) {
                each.canceled();
            }
            throw e;
        }
        myUpdatersWithFiles = ContainerUtil.createLockFreeCopyOnWriteList(list);
        myFilesToUpdate = ContainerUtil.newArrayList(set);
    }

    int getNumberOfPendingUpdateJobs() {
        return myJobsToDo;
    }

    
    public Collection<VirtualFile> getFilesToUpdate() {
        return myFilesToUpdate;
    }

    
    private Pair<CacheUpdater, Collection<VirtualFile>> getPair( final VirtualFile file) {
        return ContainerUtil.find(myUpdatersWithFiles, new Condition<Pair<CacheUpdater, Collection<VirtualFile>>>() {
            @Override
            public boolean value(Pair<CacheUpdater, Collection<VirtualFile>> cacheUpdaterCollectionPair) {
                Collection<VirtualFile> second = cacheUpdaterCollectionPair.second;
                synchronized (second) {
                    return second.contains(file);
                }
            }
        });
    }

    void processFile( FileContent content) {
        VirtualFile file = content.getVirtualFile();
        boolean isValid = file.isValid() && !file.isDirectory();

        Throwable exception = null;
        while (true) {
            Pair<CacheUpdater, Collection<VirtualFile>> pair = getPair(file);
            if (pair == null) break;
            CacheUpdater eachUpdater = pair.getFirst();
            Collection<VirtualFile> eachFiles = pair.getSecond();
            try {
                if (isValid && exception == null) {
                    eachUpdater.processFile(content);
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                exception = e;
            }
            removeFile(file, eachUpdater, eachFiles);
        }
        if (exception instanceof RuntimeException) throw (RuntimeException)exception;
        if (exception instanceof Error) throw (Error)exception;
        if (exception != null) throw new RuntimeException(exception);
    }

    private void removeFile( VirtualFile file,  CacheUpdater eachUpdater,  Collection<VirtualFile> eachFiles) {
        synchronized (eachFiles) {
            eachFiles.remove(file);
        }
    }

    void updatingDone() {
        for (Pair<CacheUpdater, Collection<VirtualFile>> eachPair : myUpdatersWithFiles) {
            try {
                CacheUpdater eachUpdater = eachPair.first;
                eachUpdater.updatingDone();
                Collection<VirtualFile> second = eachPair.second;
                synchronized (second) {
                    if (!second.isEmpty()) {
                        LOG.error(CacheUpdater.class.getSimpleName() + " " + eachUpdater + " has not finished yet:\n" + new ArrayList<VirtualFile>(second));
                    }
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    void canceled() {
        for (Pair<CacheUpdater, Collection<VirtualFile>> eachPair : myUpdatersWithFiles) {
            eachPair.first.canceled();
        }
    }
}
