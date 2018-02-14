/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.compiler.impl;

import com.gome.maven.openapi.compiler.CompileScope;
import com.gome.maven.openapi.compiler.ExportableUserDataHolderBase;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.roots.FileIndex;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 18, 2003
 */
public abstract class FileIndexCompileScope extends ExportableUserDataHolderBase implements CompileScope {

    protected abstract FileIndex[] getFileIndices();


    public VirtualFile[] getFiles(final FileType fileType, final boolean inSourceOnly) {
        final List<VirtualFile> files = new ArrayList<VirtualFile>();
        final FileIndex[] fileIndices = getFileIndices();
        for (final FileIndex fileIndex : fileIndices) {
            fileIndex.iterateContent(new CompilerContentIterator(fileType, fileIndex, inSourceOnly, files));
        }
        return VfsUtil.toVirtualFileArray(files);
    }
}
