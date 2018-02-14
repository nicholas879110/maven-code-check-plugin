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
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ContentIterator;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

public class OneProjectItemCompileScope extends ExportableUserDataHolderBase implements CompileScope{
    private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.OneProjectItemCompileScope");
    private final Project myProject;
    private final VirtualFile myFile;
    private final String myUrl;

    public OneProjectItemCompileScope(Project project, VirtualFile file) {
        myProject = project;
        myFile = file;
        final String url = file.getUrl();
        myUrl = file.isDirectory()? url + "/" : url;
    }

    
    public VirtualFile[] getFiles(final FileType fileType, final boolean inSourceOnly) {
        final List<VirtualFile> files = new ArrayList<VirtualFile>(1);
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        final ContentIterator iterator = new CompilerContentIterator(fileType, projectFileIndex, inSourceOnly, files);
        if (myFile.isDirectory()){
            projectFileIndex.iterateContentUnderDirectory(myFile, iterator);
        }
        else{
            iterator.processFile(myFile);
        }
        return VfsUtil.toVirtualFileArray(files);
    }

    public boolean belongs(String url) {
        if (myFile.isDirectory()){
            return FileUtil.startsWith(url, myUrl);
        }
        return FileUtil.pathsEqual(url, myUrl);
    }

    
    public Module[] getAffectedModules() {
        final Module module = ModuleUtil.findModuleForFile(myFile, myProject);
        if (module == null) {
            LOG.error("Module is null for file " + myFile.getPresentableUrl());
            return Module.EMPTY_ARRAY;
        }
        return new Module[] {module};
    }

}
