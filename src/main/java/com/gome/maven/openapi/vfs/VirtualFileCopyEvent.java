/*
 * Copyright 2000-2012 JetBrains s.r.o.
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



/**
 * Provides data for event which is fired when a virtual file is copied.
 *
 * @see com.gome.maven.openapi.vfs.VirtualFileListener#fileCopied(com.gome.maven.openapi.vfs.VirtualFileCopyEvent)
 */
public class VirtualFileCopyEvent extends VirtualFileEvent {
    private final VirtualFile myOriginalFile;

    public VirtualFileCopyEvent( Object requestor, VirtualFile original, VirtualFile created) {
        super(requestor, created, created.getName(), created.getParent());
        myOriginalFile = original;
    }

    /**
     * Returns original file.
     *
     * @return original file.
     */
    public VirtualFile getOriginalFile() {
        return myOriginalFile;
    }
}
