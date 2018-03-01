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
package com.gome.maven.openapi.vfs.ex.http;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.vfs.DeprecatedVirtualFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.io.URLUtil;

public abstract class HttpFileSystem extends DeprecatedVirtualFileSystem {
    public static HttpFileSystem getInstance() {
        return (HttpFileSystem)VirtualFileManager.getInstance().getFileSystem(URLUtil.HTTP_PROTOCOL);
    }

    public abstract boolean isFileDownloaded( VirtualFile file);

    public abstract void addFileListener( HttpVirtualFileListener listener);

    public abstract void addFileListener( HttpVirtualFileListener listener,  Disposable parentDisposable);

    public abstract void removeFileListener( HttpVirtualFileListener listener);

    public abstract VirtualFile createChild( VirtualFile parent,  String name, boolean isDirectory);
}
