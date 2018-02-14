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
package com.gome.maven.compiler.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.compiler.CompileScope;
import com.gome.maven.openapi.compiler.ExportableUserDataHolderBase;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileVisitor;

import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @since Jan 20, 2003
 */
public class FileSetCompileScope extends ExportableUserDataHolderBase implements CompileScope {
    private final Set<VirtualFile> myRootFiles = new HashSet<VirtualFile>();
    private final Set<String> myDirectoryUrls = new HashSet<String>();
    private Set<String> myUrls = null; // urls caching
    private final Module[] myAffectedModules;

    public FileSetCompileScope(final Collection<VirtualFile> files, Module[] modules) {
        myAffectedModules = modules;
        ApplicationManager.getApplication().runReadAction(
                new Runnable() {
                    public void run() {
                        for (VirtualFile file : files) {
                            assert file != null;
                            addFile(file);
                        }
                    }
                }
        );
    }

    
    public Module[] getAffectedModules() {
        return myAffectedModules;
    }

    public Collection<VirtualFile> getRootFiles() {
        return Collections.unmodifiableCollection(myRootFiles);
    }

    
    public VirtualFile[] getFiles(final FileType fileType, boolean inSourceOnly) {
        final List<VirtualFile> files = new ArrayList<VirtualFile>();
        for (Iterator<VirtualFile> it = myRootFiles.iterator(); it.hasNext();) {
            VirtualFile file = it.next();
            if (!file.isValid()) {
                it.remove();
                continue;
            }
            if (file.isDirectory()) {
                addRecursively(files, file, fileType);
            }
            else {
                if (fileType == null || fileType.equals(file.getFileType())) {
                    files.add(file);
                }
            }
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }

    public boolean belongs(String url) {
        //url = CompilerUtil.normalizePath(url, '/');
        if (getUrls().contains(url)) {
            return true;
        }
        for (String directoryUrl : myDirectoryUrls) {
            if (FileUtil.startsWith(url, directoryUrl)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getUrls() {
        if (myUrls == null) {
            myUrls = new HashSet<String>();
            for (VirtualFile file : myRootFiles) {
                String url = file.getUrl();
                myUrls.add(url);
            }
        }
        return myUrls;
    }

    private void addFile(VirtualFile file) {
        if (file.isDirectory()) {
            myDirectoryUrls.add(file.getUrl() + "/");
        }
        myRootFiles.add(file);
        myUrls = null;
    }

    private static void addRecursively(final Collection<VirtualFile> container, VirtualFile fromDirectory, final FileType fileType) {
        VfsUtilCore.visitChildrenRecursively(fromDirectory, new VirtualFileVisitor(VirtualFileVisitor.SKIP_ROOT) {
            @Override
            public boolean visitFile( VirtualFile child) {
                if (!child.isDirectory() && (fileType == null || fileType.equals(child.getFileType()))) {
                    container.add(child);
                }
                return true;
            }
        });
    }
}
