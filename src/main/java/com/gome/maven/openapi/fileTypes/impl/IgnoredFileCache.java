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
package com.gome.maven.openapi.fileTypes.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.gome.maven.util.containers.ConcurrentBitSet;
import com.gome.maven.util.containers.ConcurrentIntObjectMap;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBusConnection;

import java.util.List;

/**
 * @author peter
 */
class IgnoredFileCache {
    private final ConcurrentBitSet myCheckedIds = new ConcurrentBitSet();
    private final ConcurrentIntObjectMap<Object> myIgnoredIds = ContainerUtil.createConcurrentIntObjectMap();
    private final IgnoredPatternSet myIgnoredPatterns;
    private volatile int myVfsEventNesting = 0;

    IgnoredFileCache( IgnoredPatternSet ignoredPatterns) {
        myIgnoredPatterns = ignoredPatterns;
        MessageBusConnection connect = ApplicationManager.getApplication().getMessageBus().connect();
        connect.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void before( List<? extends VFileEvent> events) {
                // during VFS event processing the system may be in inconsistent state, don't cache it
                myVfsEventNesting++;
                clearCacheForChangedFiles(events);
            }

            @Override
            public void after( List<? extends VFileEvent> events) {
                clearCacheForChangedFiles(events);
                myVfsEventNesting--;
            }

            private void clearCacheForChangedFiles( List<? extends VFileEvent> events) {
                for (final VFileEvent event : events) {
                    VirtualFile file = event.getFile();
                    if (file instanceof NewVirtualFile && event instanceof VFilePropertyChangeEvent) {
                        int id = ((NewVirtualFile)file).getId();
                        if (id >= 0) {
                            myCheckedIds.clear(id);
                            myIgnoredIds.remove(id);
                        }
                    }
                }
            }
        });
    }

    void clearCache() {
        myCheckedIds.clear();
        myIgnoredIds.clear();
    }

    boolean isFileIgnored( VirtualFile file) {
        if (myVfsEventNesting != 0 || !(file instanceof NewVirtualFile)) {
            return isFileIgnoredNoCache(file);
        }

        int id = ((NewVirtualFile)file).getId();
        if (id < 0) {
            return isFileIgnoredNoCache(file);
        }

        ConcurrentBitSet checkedIds = myCheckedIds;
        if (checkedIds.get(id)) {
            return myIgnoredIds.containsKey(id);
        }

        boolean result = isFileIgnoredNoCache(file);
        if (result) {
            myIgnoredIds.put(id, Boolean.TRUE);
        }
        else {
            myIgnoredIds.remove(id);
        }
        checkedIds.set(id);
        return result;
    }

    private boolean isFileIgnoredNoCache( VirtualFile file) {
        return myIgnoredPatterns.isIgnored(file.getName());
    }
}
