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
package com.gome.maven.openapi.vfs.newvfs.events;

import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileSystem;

/**
 * @author max
 */
public abstract class VFileEvent {
    private final boolean myIsFromRefresh;
    private final Object myRequestor;

    public VFileEvent(Object requestor, final boolean isFromRefresh) {
        myRequestor = requestor;
        myIsFromRefresh = isFromRefresh;
    }

    public boolean isFromRefresh() {
        return myIsFromRefresh;
    }

    public Object getRequestor() {
        return myRequestor;
    }

    
    public abstract String getPath();

    /**
     * Returns the VirtualFile which this event belongs to.
     * In some cases it may be null - it is not guaranteed that there is such file.
     * <p/>
     * NB: Use this method with caution, because {@link VFileCreateEvent#getFile()} needs
     * {@link VirtualFile#findChild(String)} which may be a performance leak.
     */
    
    public abstract VirtualFile getFile();

    
    public abstract VirtualFileSystem getFileSystem();

    public abstract boolean isValid();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);
}