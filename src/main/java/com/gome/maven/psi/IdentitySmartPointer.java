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

/*
 * @author max
 */
package com.gome.maven.psi;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.vfs.VirtualFile;

public class IdentitySmartPointer<T extends PsiElement> implements SmartPsiElementPointer<T> {
    private final T myElement;
    private final PsiFile myFile;

    public IdentitySmartPointer( T element,  PsiFile file) {
        myElement = element;
        myFile = file;
    }

    public IdentitySmartPointer( T element) {
        this(element, element.getContainingFile());
    }

    @Override
    
    public Project getProject() {
        return myFile.getProject();
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myFile.getVirtualFile();
    }

    @Override
    public T getElement() {
        T element = myElement;
        return element.isValid() ? element : null;
    }

    public int hashCode() {
        final T elt = getElement();
        return elt == null ? 0 : elt.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof SmartPsiElementPointer
                && SmartPointerManager.getInstance(getProject()).pointToTheSameElement(this, (SmartPsiElementPointer)obj);
    }

    @Override
    public PsiFile getContainingFile() {
        return myFile;
    }

    @Override
    public Segment getRange() {
        return myElement.getTextRange();
    }
}
