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
import com.gome.maven.psi.PsiAnchor;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

/**
 * User: cdr
 */
public class ClsElementInfo implements SmartPointerElementInfo {
    private final PsiAnchor.StubIndexReference myStubIndexReference;

    public ClsElementInfo( PsiAnchor.StubIndexReference stubReference) {
        myStubIndexReference = stubReference;
    }

    @Override
    public Document getDocumentToSynchronize() {
        return null;
    }

    // before change
    @Override
    public void fastenBelt(int offset, RangeMarker[] cachedRangeMarker) {
    }

    // after change
    @Override
    public void unfastenBelt(int offset) {
    }

    @Override
    public PsiElement restoreElement() {
        return myStubIndexReference.retrieve();
    }

    @Override
    public int elementHashCode() {
        return myStubIndexReference.hashCode();
    }

    @Override
    public boolean pointsToTheSameElementAs( SmartPointerElementInfo other) {
        if (other instanceof ClsElementInfo) {
            return myStubIndexReference.equals(((ClsElementInfo)other).myStubIndexReference);
        }
        return Comparing.equal(restoreElement(), other.restoreElement());
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myStubIndexReference.getVirtualFile();
    }

    @Override
    public Segment getRange() {
        return null;
    }

    
    @Override
    public Project getProject() {
        return myStubIndexReference.getProject();
    }

    @Override
    public void cleanup() {

    }

    @Override
    public PsiFile restoreFile() {
        return myStubIndexReference.getFile();
    }
}
