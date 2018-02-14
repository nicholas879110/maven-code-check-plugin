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

import com.gome.maven.openapi.util.io.FileAttributes;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileListener;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.VirtualFileSystem;
import com.gome.maven.util.containers.ContainerUtil;

import java.io.IOException;
import java.util.Map;

/**
 * @author max
 */
public abstract class NewVirtualFileSystem extends VirtualFileSystem implements FileSystemInterface, CachingVirtualFileSystem {
    private final Map<VirtualFileListener, VirtualFileListener> myListenerWrappers =
            ContainerUtil.newConcurrentMap();

    
    public abstract VirtualFile findFileByPathIfCached(  final String path);

    
    protected String normalize( String path) {
        return path;
    }

    @Override
    public void refreshWithoutFileWatcher(final boolean asynchronous) {
        refresh(asynchronous);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isSymLink( final VirtualFile file) {
        return false;
    }

    @Override
    public String resolveSymLink( VirtualFile file) {
        return null;
    }

    @Override
    public boolean isSpecialFile( final VirtualFile file) {
        return false;
    }

    
    protected abstract String extractRootPath( String path);

    @Override
    public void addVirtualFileListener( final VirtualFileListener listener) {
        VirtualFileListener wrapper = new VirtualFileFilteringListener(listener, this);
        VirtualFileManager.getInstance().addVirtualFileListener(wrapper);
        myListenerWrappers.put(listener, wrapper);
    }

    @Override
    public void removeVirtualFileListener( final VirtualFileListener listener) {
        final VirtualFileListener wrapper = myListenerWrappers.remove(listener);
        if (wrapper != null) {
            VirtualFileManager.getInstance().removeVirtualFileListener(wrapper);
        }
    }

    public abstract int getRank();

    
    @Override
    public abstract VirtualFile copyFile(Object requestor,  VirtualFile file,  VirtualFile newParent,  String copyName) throws IOException;

    @Override
    
    public abstract VirtualFile createChildDirectory(Object requestor,  VirtualFile parent,  String dir) throws IOException;

    
    @Override
    public abstract VirtualFile createChildFile(Object requestor,  VirtualFile parent,  String file) throws IOException;

    @Override
    public abstract void deleteFile(Object requestor,  VirtualFile file) throws IOException;

    @Override
    public abstract void moveFile(Object requestor,  VirtualFile file,  VirtualFile newParent) throws IOException;

    @Override
    public abstract void renameFile(final Object requestor,  VirtualFile file,  String newName) throws IOException;

    public boolean markNewFilesAsDirty() {
        return false;
    }

    
    public String getCanonicallyCasedName( VirtualFile file) {
        return file.getName();
    }

    /**
     * Reads various file attributes in one shot (to reduce the number of native I/O calls).
     *
     * @param file file to get attributes of.
     * @return attributes of a given file, or <code>null</code> if the file doesn't exist.
     * @since 11.1
     */
    
    public abstract FileAttributes getAttributes( VirtualFile file);
}
