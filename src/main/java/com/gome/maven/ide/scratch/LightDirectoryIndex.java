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
package com.gome.maven.ide.scratch;

import com.gome.maven.openapi.fileTypes.FileTypeEvent;
import com.gome.maven.openapi.fileTypes.FileTypeListener;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.VirtualFileWithId;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.util.PairConsumer;
import com.gome.maven.util.containers.ConcurrentIntObjectMap;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBusConnection;

import java.util.List;

/**
 * This is a light version of DirectoryIndexImpl.
 *
 * @author gregsh
 */
abstract class LightDirectoryIndex<T> {
    private final ConcurrentIntObjectMap<T> myInfoCache = ContainerUtil.createConcurrentIntObjectMap();
    private final T myDefValue;

    public LightDirectoryIndex( MessageBusConnection connection,  T defValue) {
        myDefValue = defValue;
        reinitRoots();
        connection.subscribe(FileTypeManager.TOPIC, new FileTypeListener.Adapter() {
            @Override
            public void fileTypesChanged( FileTypeEvent event) {
                reinitRoots();
            }
        });
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void before( List<? extends VFileEvent> events) {
            }

            @Override
            public void after( List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    VirtualFile file = event.getFile();
                    if (file == null || file.isDirectory()) {
                        reinitRoots();
                        break;
                    }
                }
            }
        });
    }

    public void reinitRoots() {
        myInfoCache.clear();
        collectRoots(new PairConsumer<VirtualFile, T>() {
            @Override
            public void consume(VirtualFile file, T info) {
                cacheInfo(file, info);
            }
        });
    }

    protected abstract void collectRoots( PairConsumer<VirtualFile, T> consumer);

    
    public T getInfoForFile( VirtualFile file) {
        VirtualFile dir;
        if (!file.isDirectory()) {
            T info = getCachedInfo(file);
            if (info != null) {
                return info;
            }
            dir = file.getParent();
        }
        else {
            dir = file;
        }

        int count = 0;
        for (VirtualFile root = dir; root != null; root = root.getParent()) {
            if (++count > 1000) {
                throw new IllegalStateException("Possible loop in tree, started at " + dir.getName());
            }
            T info = getCachedInfo(root);
            if (info != null) {
                if (!dir.equals(root)) {
                    cacheInfos(dir, root, info);
                }
                return info;
            }
        }

        return cacheInfos(dir, null, myDefValue);
    }

    
    private T cacheInfos(VirtualFile dir,  VirtualFile stopAt,  T info) {
        while (dir != null) {
            cacheInfo(dir, info);
            if (dir.equals(stopAt)) {
                break;
            }
            dir = dir.getParent();
        }
        return info;
    }

    private void cacheInfo(VirtualFile file, T info) {
        myInfoCache.put(((VirtualFileWithId)file).getId(), info);
    }

    private T getCachedInfo(VirtualFile file) {
        return myInfoCache.get(((VirtualFileWithId)file).getId());
    }

}
