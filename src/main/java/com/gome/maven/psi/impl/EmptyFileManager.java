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
package com.gome.maven.psi.impl;

//import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.SingleRootFileViewProvider;
import com.gome.maven.psi.impl.file.impl.FileManager;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author max
 */
class EmptyFileManager implements FileManager {
    private final PsiManagerImpl myManager;
    private final ConcurrentMap<VirtualFile, FileViewProvider> myVFileToViewProviderMap = ContainerUtil.createConcurrentWeakValueMap();

    EmptyFileManager(final PsiManagerImpl manager) {
        myManager = manager;
    }

    @Override
    public void dispose() {
    }

    @Override
    public PsiFile findFile( VirtualFile vFile) {
        return null;
    }

    @Override
    public PsiDirectory findDirectory( VirtualFile vFile) {
        return null;
    }

    @Override
    public void reloadFromDisk( PsiFile file) {
    }

    @Override
    public PsiFile getCachedPsiFile( VirtualFile vFile) {
        return null;
    }

    @Override
    public void cleanupForNextTest() {
        myVFileToViewProviderMap.clear();
    }

    @Override
    public FileViewProvider findViewProvider( VirtualFile file) {
        return myVFileToViewProviderMap.get(file);
    }

    @Override
    public FileViewProvider findCachedViewProvider( VirtualFile file) {
        return myVFileToViewProviderMap.get(file);
    }

    @Override
    
    public FileViewProvider createFileViewProvider( final VirtualFile file, final boolean eventSystemEnabled) {
        return new SingleRootFileViewProvider(myManager, file, eventSystemEnabled);
    }

    @Override
    public void setViewProvider( final VirtualFile virtualFile, final FileViewProvider singleRootFileViewProvider) {
//        if (!(virtualFile instanceof VirtualFileWindow)) {
            if (singleRootFileViewProvider == null) {
                myVFileToViewProviderMap.remove(virtualFile);
            }
            else {
                myVFileToViewProviderMap.put(virtualFile, singleRootFileViewProvider);
            }
//        }
    }

    
    @Override
    public List<PsiFile> getAllCachedFiles() {
        return Collections.emptyList();
    }
}
