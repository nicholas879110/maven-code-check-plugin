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
package com.gome.maven.openapi.vfs;

import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.newvfs.ManagingFS;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.gome.maven.util.Processor;
import com.gome.maven.util.io.fs.IFile;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;

public abstract class LocalFileSystem extends NewVirtualFileSystem {
     public static final String PROTOCOL = StandardFileSystems.FILE_PROTOCOL;
     public static final String PROTOCOL_PREFIX = StandardFileSystems.FILE_PROTOCOL_PREFIX;

    @SuppressWarnings("UtilityClassWithoutPrivateConstructor")
    private static class LocalFileSystemHolder {
        private static final LocalFileSystem ourInstance = (LocalFileSystem)VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
    }

    public static LocalFileSystem getInstance() {
        return LocalFileSystemHolder.ourInstance;
    }

    /**
     * Checks whether given file is a symbolic link.
     *
     * @param file a file to check.
     * @return <code>true</code> if the file is a symbolic link, <code>false</code> otherwise
     * @since 11.0
     */
    @Override
    public boolean isSymLink( final VirtualFile file) {
        return false;
    }

    /**
     * Checks whether given file is a special file.
     *
     * @param file a file to check.
     * @return <code>true</code> if the file exists and is a special one, <code>false</code> otherwise
     * @since 11.0
     */
    @Override
    public boolean isSpecialFile( final VirtualFile file) {
        return false;
    }

    
    public abstract VirtualFile findFileByIoFile( File file);

    
    public abstract VirtualFile findFileByIoFile( IFile file);

    
    public abstract VirtualFile refreshAndFindFileByIoFile( File file);

    
    public abstract VirtualFile refreshAndFindFileByIoFile( IFile ioFile);

    /**
     * Performs a non-recursive synchronous refresh of specified files.
     *
     * @param files files to refresh.
     * @since 6.0
     */
    public abstract void refreshIoFiles( Iterable<File> files);

    public abstract void refreshIoFiles( Iterable<File> files, boolean async, boolean recursive,  Runnable onFinish);

    /**
     * Performs a non-recursive synchronous refresh of specified files.
     *
     * @param files files to refresh.
     * @since 6.0
     */
    public abstract void refreshFiles( Iterable<VirtualFile> files);

    public abstract void refreshFiles( Iterable<VirtualFile> files, boolean async, boolean recursive,  Runnable onFinish);

    /** @deprecated fake root considered harmful (to remove in IDEA 14) */
    public final VirtualFile getRoot() {
        VirtualFile[] roots = ManagingFS.getInstance().getLocalRoots();
        assert roots.length > 0 : SystemInfo.OS_NAME;
        return roots[0];
    }

    public interface WatchRequest {
        
        String getRootPath();

        boolean isToWatchRecursively();
    }

    
    public WatchRequest addRootToWatch( final String rootPath, final boolean watchRecursively) {
        final Set<WatchRequest> result = addRootsToWatch(singleton(rootPath), watchRecursively);
        return result.size() == 1 ? result.iterator().next() : null;
    }

    
    public abstract Set<WatchRequest> addRootsToWatch( final Collection<String> rootPaths, final boolean watchRecursively);

    public void removeWatchedRoot( final WatchRequest watchRequest) {
        if (watchRequest != null) {
            removeWatchedRoots(singleton(watchRequest));
        }
    }

    public abstract void removeWatchedRoots( final Collection<WatchRequest> watchRequests);

    
    public WatchRequest replaceWatchedRoot( final WatchRequest watchRequest,
                                            final String rootPath,
                                           final boolean watchRecursively) {
        final Set<WatchRequest> requests = watchRequest != null ? singleton(watchRequest) : Collections.<WatchRequest>emptySet();
        final Set<WatchRequest> result = watchRecursively ? replaceWatchedRoots(requests, singleton(rootPath), null)
                : replaceWatchedRoots(requests, null, singleton(rootPath));
        return result.size() == 1 ? result.iterator().next() : null;
    }

    public abstract Set<WatchRequest> replaceWatchedRoots( final Collection<WatchRequest> watchRequests,
                                                           final Collection<String> recursiveRoots,
                                                           final Collection<String> flatRoots);

    /**
     * Registers a handler that allows a version control system plugin to intercept file operations in the local file system
     * and to perform them through the VCS tool.
     *
     * @param handler the handler instance.
     */
    public abstract void registerAuxiliaryFileOperationsHandler( LocalFileOperationsHandler handler);

    /**
     * Unregisters a handler that allows a version control system plugin to intercept file operations in the local file system
     * and to perform them through the VCS tool.
     *
     * @param handler the handler instance.
     */
    public abstract void unregisterAuxiliaryFileOperationsHandler( LocalFileOperationsHandler handler);

    public abstract boolean processCachedFilesInSubtree( VirtualFile file,  Processor<VirtualFile> processor);
}
