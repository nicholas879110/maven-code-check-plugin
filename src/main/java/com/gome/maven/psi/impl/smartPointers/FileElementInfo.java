/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.smartPointers;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

/**
 * User: cdr
 */
class FileElementInfo implements SmartPointerElementInfo {
    protected final VirtualFile myVirtualFile;
    protected final Project myProject;
    protected final Language myLanguage;

    public FileElementInfo( PsiFile file) {
        this(file.getProject(), file.getVirtualFile(), file.getLanguage());
    }
    protected FileElementInfo( Project project, VirtualFile virtualFile) {
        this(project, virtualFile, null);
    }

    protected FileElementInfo( Project project, VirtualFile virtualFile, Language lang) {
        myVirtualFile = virtualFile;
        myProject = project;
        myLanguage = lang;
    }

    @Override
    public Document getDocumentToSynchronize() {
        return null;
    }

    @Override
    public void fastenBelt(int offset, RangeMarker[] cachedRangeMarker) {
    }

    @Override
    public void unfastenBelt(int offset) {
    }

    @Override
    public PsiElement restoreElement() {
        return SelfElementInfo.restoreFileFromVirtual(myVirtualFile, myProject, myLanguage);
    }

    @Override
    public PsiFile restoreFile() {
        PsiElement element = restoreElement();
        return element == null ? null : element.getContainingFile(); // can be directory
    }

    @Override
    public int elementHashCode() {
        return myVirtualFile.hashCode();
    }

    @Override
    public boolean pointsToTheSameElementAs( SmartPointerElementInfo other) {
        if (other instanceof FileElementInfo) {
            return Comparing.equal(myVirtualFile, ((FileElementInfo)other).myVirtualFile);
        }
        if (other instanceof SelfElementInfo || other instanceof ClsElementInfo) {
            // optimisation: SelfElementInfo need psi (parsing) for element restoration and apriori could not reference psi file
            return false;
        }
        return Comparing.equal(restoreElement(), other.restoreElement());
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myVirtualFile;
    }

    @Override
    public Segment getRange() {
        return new TextRange(0, (int)myVirtualFile.getLength());
    }

    
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void cleanup() {

    }
}
