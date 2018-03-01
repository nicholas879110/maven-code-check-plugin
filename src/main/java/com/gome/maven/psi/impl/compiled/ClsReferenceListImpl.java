/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.util.AtomicNotNullLazyValue;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.java.stubs.PsiClassReferenceListStub;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;

public class ClsReferenceListImpl extends ClsRepositoryPsiElement<PsiClassReferenceListStub> implements PsiReferenceList {
    private static final ClsJavaCodeReferenceElementImpl[] EMPTY_REFS_ARRAY = new ClsJavaCodeReferenceElementImpl[0];

    private final NotNullLazyValue<ClsJavaCodeReferenceElementImpl[]> myRefs;

    public ClsReferenceListImpl( PsiClassReferenceListStub stub) {
        super(stub);
        myRefs = new AtomicNotNullLazyValue<ClsJavaCodeReferenceElementImpl[]>() {
            
            @Override
            protected ClsJavaCodeReferenceElementImpl[] compute() {
                String[] strings = getStub().getReferencedNames();
                if (strings.length > 0) {
                    ClsJavaCodeReferenceElementImpl[] refs = new ClsJavaCodeReferenceElementImpl[strings.length];
                    for (int i = 0; i < strings.length; i++) {
                        refs[i] = new ClsJavaCodeReferenceElementImpl(ClsReferenceListImpl.this, strings[i]);
                    }
                    return refs;
                }
                else {
                    return EMPTY_REFS_ARRAY;
                }
            }
        };
    }

    @Override
    
    public PsiJavaCodeReferenceElement[] getReferenceElements() {
        return myRefs.getValue();
    }

    @Override
    
    public PsiElement[] getChildren() {
        return getReferenceElements();
    }

    @Override
    
    public PsiClassType[] getReferencedTypes() {
        return getStub().getReferencedTypes();
    }

    @Override
    public Role getRole() {
        return getStub().getRole();
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        final String[] names = getStub().getReferencedNames();
        if (names.length != 0) {
            final Role role = getStub().getRole();
            switch (role) {
                case EXTENDS_BOUNDS_LIST:
                case EXTENDS_LIST:
                    buffer.append(PsiKeyword.EXTENDS).append(' ');
                    break;
                case IMPLEMENTS_LIST:
                    buffer.append(PsiKeyword.IMPLEMENTS).append(' ');
                    break;
                case THROWS_LIST:
                    buffer.append(PsiKeyword.THROWS).append(' ');
                    break;
            }
            for (int i = 0; i < names.length; i++) {
                if (i > 0) buffer.append(", ");
                buffer.append(names[i]);
            }
        }
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, null);
        setMirrors(getReferenceElements(), SourceTreeToPsiMap.<PsiReferenceList>treeToPsiNotNull(element).getReferenceElements());
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitReferenceList(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public String toString() {
        return "PsiReferenceList";
    }
}
