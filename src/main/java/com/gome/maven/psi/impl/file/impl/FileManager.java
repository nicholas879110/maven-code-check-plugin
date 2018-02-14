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

package com.gome.maven.psi.impl.file.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;

import java.util.List;

public interface FileManager extends Disposable {
    
    PsiFile findFile( VirtualFile vFile);

    
    PsiDirectory findDirectory( VirtualFile vFile);

    void reloadFromDisk( PsiFile file); //Q: move to PsiFile(Impl)?

    
    PsiFile getCachedPsiFile( VirtualFile vFile);

    
    void cleanupForNextTest();

    FileViewProvider findViewProvider( VirtualFile file);
    FileViewProvider findCachedViewProvider( VirtualFile file);
    void setViewProvider( VirtualFile virtualFile,  FileViewProvider fileViewProvider);

    
    List<PsiFile> getAllCachedFiles();

    
    FileViewProvider createFileViewProvider( VirtualFile file, boolean eventSystemEnabled);
}
