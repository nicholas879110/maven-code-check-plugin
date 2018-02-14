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
package com.gome.maven.openapi.vfs.newvfs;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.ZipFileCache;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.impl.ArchiveHandler;
import com.gome.maven.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gome.maven.util.containers.ContainerUtil.newTroveMap;

public class VfsImplUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vfs.newvfs.VfsImplUtil");

    private static final String FILE_SEPARATORS = "/" + File.separator;

    private VfsImplUtil() {
    }

    
    public static NewVirtualFile findFileByPath( NewVirtualFileSystem vfs,   String path) {
        Pair<NewVirtualFile, Iterable<String>> data = prepare(vfs, path);
        if (data == null) {
            return null;
        }

        NewVirtualFile file = data.first;
        for (String pathElement : data.second) {
            if (pathElement.isEmpty() || ".".equals(pathElement)) continue;
            if ("..".equals(pathElement)) {
                if (file.is(VFileProperty.SYMLINK)) {
                    final NewVirtualFile canonicalFile = file.getCanonicalFile();
                    file = canonicalFile != null ? canonicalFile.getParent() : null;
                }
                else {
                    file = file.getParent();
                }
            }
            else {
                file = file.findChild(pathElement);
            }

            if (file == null) return null;
        }

        return file;
    }

    
    public static NewVirtualFile findFileByPathIfCached( NewVirtualFileSystem vfs,   String path) {
        Pair<NewVirtualFile, Iterable<String>> data = prepare(vfs, path);
        if (data == null) {
            return null;
        }

        NewVirtualFile file = data.first;
        for (String pathElement : data.second) {
            if (pathElement.isEmpty() || ".".equals(pathElement)) continue;
            if ("..".equals(pathElement)) {
                if (file.is(VFileProperty.SYMLINK)) {
                    final String canonicalPath = file.getCanonicalPath();
                    final NewVirtualFile canonicalFile = canonicalPath != null ? findFileByPathIfCached(vfs, canonicalPath) : null;
                    file = canonicalFile != null ? canonicalFile.getParent() : null;
                }
                else {
                    file = file.getParent();
                }
            }
            else {
                file = file.findChildIfCached(pathElement);
            }

            if (file == null) return null;
        }

        return file;
    }

    
    public static NewVirtualFile refreshAndFindFileByPath( NewVirtualFileSystem vfs,   String path) {
        Pair<NewVirtualFile, Iterable<String>> data = prepare(vfs, path);
        if (data == null) {
            return null;
        }

        NewVirtualFile file = data.first;
        for (String pathElement : data.second) {
            if (pathElement.isEmpty() || ".".equals(pathElement)) continue;
            if ("..".equals(pathElement)) {
                if (file.is(VFileProperty.SYMLINK)) {
                    final String canonicalPath = file.getCanonicalPath();
                    final NewVirtualFile canonicalFile = canonicalPath != null ? refreshAndFindFileByPath(vfs, canonicalPath) : null;
                    file = canonicalFile != null ? canonicalFile.getParent() : null;
                }
                else {
                    file = file.getParent();
                }
            }
            else {
                file = file.refreshAndFindChild(pathElement);
            }

            if (file == null) return null;
        }

        return file;
    }

    
    private static Pair<NewVirtualFile, Iterable<String>> prepare( NewVirtualFileSystem vfs,  String path) {
        String normalizedPath = normalize(vfs, path);
        if (StringUtil.isEmptyOrSpaces(normalizedPath)) {
            return null;
        }

        String basePath = vfs.extractRootPath(normalizedPath);
        if (basePath.length() > normalizedPath.length()) {
            LOG.error(vfs + " failed to extract root path '" + basePath + "' from '" + normalizedPath + "' (original '" + path + "')");
            return null;
        }

        NewVirtualFile root = ManagingFS.getInstance().findRoot(basePath, vfs);
        if (root == null || !root.exists()) {
            return null;
        }

        Iterable<String> parts = StringUtil.tokenize(normalizedPath.substring(basePath.length()), FILE_SEPARATORS);
        return Pair.create(root, parts);
    }

    public static void refresh( NewVirtualFileSystem vfs, boolean asynchronous) {
        VirtualFile[] roots = ManagingFS.getInstance().getRoots(vfs);
        if (roots.length > 0) {
            RefreshQueue.getInstance().refresh(asynchronous, true, null, roots);
        }
    }

    
    public static String normalize( NewVirtualFileSystem vfs,  String path) {
        return vfs.normalize(path);
    }

    private static final AtomicBoolean ourSubscribed = new AtomicBoolean(false);
    private static final Object ourLock = new Object();
    private static final Map<String, Pair<ArchiveFileSystem, ArchiveHandler>> ourHandlers = newTroveMap(FileUtil.PATH_HASHING_STRATEGY);
    private static final Map<String, Set<String>> ourDominatorsMap = newTroveMap(FileUtil.PATH_HASHING_STRATEGY);

    
    public static <T extends ArchiveHandler> T getHandler( ArchiveFileSystem vfs,
                                                           VirtualFile entryFile,
                                                           Function<String, T> producer) {
        String localPath = vfs.extractLocalPath(vfs.extractRootPath(entryFile.getPath()));
        return getHandler(vfs, localPath, producer);
    }

    
    public static <T extends ArchiveHandler> T getHandler( ArchiveFileSystem vfs,
                                                           String localPath,
                                                           Function<String, T> producer) {
        checkSubscription();

        ArchiveHandler handler;

        synchronized (ourLock) {
            Pair<ArchiveFileSystem, ArchiveHandler> record = ourHandlers.get(localPath);
            if (record == null) {
                handler = producer.fun(localPath);
                record = Pair.create(vfs, handler);
                ourHandlers.put(localPath, record);

                final String finalRootPath = localPath;
                forEachDirectoryComponent(localPath, new Consumer<String>() {
                    @Override
                    public void consume(String containingDirectoryPath) {
                        Set<String> handlers = ourDominatorsMap.get(containingDirectoryPath);
                        if (handlers == null) {
                            ourDominatorsMap.put(containingDirectoryPath, handlers = ContainerUtil.newTroveSet());
                        }
                        handlers.add(finalRootPath);
                    }
                });
            }
            handler = record.second;
        }

        @SuppressWarnings("unchecked") T t = (T)handler;
        return t;
    }

    private static void forEachDirectoryComponent(String rootPath, Consumer<String> consumer) {
        int index = rootPath.lastIndexOf('/');
        while (index > 0) {
            String containingDirectoryPath = rootPath.substring(0, index);
            consumer.consume(containingDirectoryPath);
            index = rootPath.lastIndexOf('/', index - 1);
        }
    }

    private static void checkSubscription() {
        if (ourSubscribed.getAndSet(true)) return;

        Application app = ApplicationManager.getApplication();
        app.getMessageBus().connect(app).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after( List<? extends VFileEvent> events) {
                InvalidationState state = null;

                synchronized (ourLock) {
                    for (VFileEvent event : events) {
                        if (!(event.getFileSystem() instanceof LocalFileSystem)) continue;

                        if (event instanceof VFileCreateEvent) continue; // created file should not invalidate + getFile is costly

                        if (event instanceof VFilePropertyChangeEvent &&
                                !VirtualFile.PROP_NAME.equals(((VFilePropertyChangeEvent)event).getPropertyName())) {
                            continue;
                        }

                        String path = event.getPath();
                        if (event instanceof VFilePropertyChangeEvent) {
                            path = ((VFilePropertyChangeEvent)event).getOldPath();
                        }
                        else if (event instanceof VFileMoveEvent) {
                            path = ((VFileMoveEvent)event).getOldPath();
                        }

                        VirtualFile file = event.getFile();
                        if (file == null || !file.isDirectory()) {
                            state = InvalidationState.invalidate(state, path);
                        }
                        else {
                            Collection<String> affectedPaths = ourDominatorsMap.get(path);
                            if (affectedPaths != null) {
                                affectedPaths = ContainerUtil.newArrayList(affectedPaths);  // defensive copying; original may be updated on invalidation
                                for (String affectedPath : affectedPaths) {
                                    state = InvalidationState.invalidate(state, affectedPath);
                                }
                            }
                        }
                    }
                }

                if (state != null) state.scheduleRefresh();
            }
        });
    }

    private static class InvalidationState {
        private Map<String, VirtualFile> rootsToRefresh;

        
        public static InvalidationState invalidate( InvalidationState state, final String path) {
            Pair<ArchiveFileSystem, ArchiveHandler> handlerPair = ourHandlers.remove(path);
            if (handlerPair != null) {
                forEachDirectoryComponent(path, new Consumer<String>() {
                    @Override
                    public void consume(String containingDirectoryPath) {
                        Set<String> handlers = ourDominatorsMap.get(containingDirectoryPath);
                        if (handlers != null && handlers.remove(path) && handlers.size() == 0) {
                            ourDominatorsMap.remove(containingDirectoryPath);
                        }
                    }
                });

                if (state == null) state = new InvalidationState();
                state.registerPathToRefresh(path, handlerPair.first);
            }

            return state;
        }

        private void registerPathToRefresh(String path, ArchiveFileSystem vfs) {
            NewVirtualFile root = ManagingFS.getInstance().findRoot(vfs.composeRootPath(path), vfs);
            if (root != null) {
                if (rootsToRefresh == null) rootsToRefresh = ContainerUtil.newHashMap();
                rootsToRefresh.put(path, root);
            }
        }

        public void scheduleRefresh() {
            if (rootsToRefresh != null) {
                for (VirtualFile root : rootsToRefresh.values()) {
                    ((NewVirtualFile)root).markDirtyRecursively();
                }
                ZipFileCache.reset(rootsToRefresh.keySet());
                boolean async = !ApplicationManager.getApplication().isUnitTestMode();
                RefreshQueue.getInstance().refresh(async, true, null, rootsToRefresh.values());
            }
        }
    }
}
