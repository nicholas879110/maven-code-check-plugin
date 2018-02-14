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

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.util.ModificationTracker;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.util.io.URLUtil;
import com.gome.maven.util.messages.Topic;

/**
 * Manages virtual file systems.
 *
 * @see VirtualFileSystem
 */
public abstract class VirtualFileManager implements ModificationTracker {
    public static final Topic<BulkFileListener> VFS_CHANGES =
            new Topic<BulkFileListener>("NewVirtualFileSystem changes", BulkFileListener.class);

    /**
     * Gets the instance of <code>VirtualFileManager</code>.
     *
     * @return <code>VirtualFileManager</code>
     */
    
    public static VirtualFileManager getInstance() {
        return ApplicationManager.getApplication().getComponent(VirtualFileManager.class);
    }

    /**
     * Gets VirtualFileSystem with the specified protocol.
     *
     * @param protocol String representing the protocol
     * @return {@link VirtualFileSystem}
     * @see VirtualFileSystem#getProtocol
     */
    public abstract VirtualFileSystem getFileSystem(String protocol);

    /**
     * <p>Refreshes the cached file systems information from the physical file systems synchronously.<p/>
     *
     * <p><strong>Note</strong>: this method should be only called within a write-action
     * (see {@linkplain com.gome.maven.openapi.application.Application#runWriteAction})</p>
     *
     * @return refresh session ID.
     */
    public abstract long syncRefresh();

    /**
     * Refreshes the cached file systems information from the physical file systems asynchronously.
     * Launches specified action when refresh is finished.
     *
     * @return refresh session ID.
     */
    public abstract long asyncRefresh( Runnable postAction);

    public abstract void refreshWithoutFileWatcher(boolean asynchronous);

    /**
     * Searches for the file specified by given URL. URL is a string which uniquely identifies file in all
     * file systems.
     *
     * @param url the URL to find file by
     * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
     * @see VirtualFile#getUrl
     * @see VirtualFileSystem#findFileByPath
     * @see #refreshAndFindFileByUrl
     */
    
    public abstract VirtualFile findFileByUrl( String url);

    /**
     * Refreshes only the part of the file system needed for searching the file by the given URL and finds file
     * by the given URL.<br>
     * <p/>
     * This method is useful when the file was created externally and you need to find <code>{@link VirtualFile}</code>
     * corresponding to it.<p>
     * <p/>
     * This method should be only called within write-action.
     * See {@link com.gome.maven.openapi.application.Application#runWriteAction}.
     *
     * @param url the URL
     * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
     * @see VirtualFileSystem#findFileByPath
     * @see VirtualFileSystem#refreshAndFindFileByPath
     */
    
    public abstract VirtualFile refreshAndFindFileByUrl( String url);

    /**
     * Adds listener to the file system.
     *
     * @param listener the listener
     * @see VirtualFileListener
     */
    public abstract void addVirtualFileListener( VirtualFileListener listener);

    public abstract void addVirtualFileListener( VirtualFileListener listener,  Disposable parentDisposable);

    /**
     * Removes listener form the file system.
     *
     * @param listener the listener
     */
    public abstract void removeVirtualFileListener( VirtualFileListener listener);

    /**
     * Constructs URL by specified protocol and path. URL is a string which uniquely identifies file in all
     * file systems.
     *
     * @param protocol the protocol
     * @param path     the path
     * @return URL
     */
    
    public static String constructUrl( String protocol,  String path) {
        return protocol + URLUtil.SCHEME_SEPARATOR + path;
    }

    /**
     * Extracts protocol from the given URL. Protocol is a substring from the beginning of the URL till "://".
     *
     * @param url the URL
     * @return protocol or <code>null</code> if there is no "://" in the URL
     * @see VirtualFileSystem#getProtocol
     */
    
    public static String extractProtocol( String url) {
        int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
        if (index < 0) return null;
        return url.substring(0, index);
    }

    /**
     * Extracts path from the given URL. Path is a substring from "://" till the end of URL. If there is no "://" URL
     * itself is returned.
     *
     * @param url the URL
     * @return path
     */
    
    public static String extractPath( String url) {
        int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
        if (index < 0) return url;
        return url.substring(index + URLUtil.SCHEME_SEPARATOR.length());
    }

    public abstract void addVirtualFileManagerListener( VirtualFileManagerListener listener);

    public abstract void addVirtualFileManagerListener( VirtualFileManagerListener listener,  Disposable parentDisposable);

    public abstract void removeVirtualFileManagerListener( VirtualFileManagerListener listener);

    public abstract void notifyPropertyChanged( VirtualFile virtualFile,  String property, Object oldValue, Object newValue);
}
