/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;

/**
 * User: cdr
 */
class HardElementInfo implements SmartPointerElementInfo {
    
    private final PsiElement myElement;
    
    private final Project myProject;

    public HardElementInfo( Project project,  PsiElement element) {
        myElement = element;
        myProject = project;
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
        return myElement;
    }

    @Override
    public PsiFile restoreFile() {
        return myElement.getContainingFile();
    }

    @Override
    public int elementHashCode() {
        return myElement.hashCode();
    }

    @Override
    public boolean pointsToTheSameElementAs( SmartPointerElementInfo other) {
        return Comparing.equal(myElement, other.restoreElement());
    }

    @Override
    public VirtualFile getVirtualFile() {
        return PsiUtilCore.getVirtualFile(myElement);
    }

    @Override
    public Segment getRange() {
        return myElement.getTextRange();
    }

    
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void cleanup() {

    }
}
