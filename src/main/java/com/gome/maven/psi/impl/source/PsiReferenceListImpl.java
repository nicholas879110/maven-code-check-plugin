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
package com.gome.maven.psi.impl.source;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.java.stubs.PsiClassReferenceListStub;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.stubs.IStubElementType;
import com.gome.maven.psi.tree.IElementType;

/**
 * @author max
 */
public class PsiReferenceListImpl extends JavaStubPsiElement<PsiClassReferenceListStub> implements PsiReferenceList {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.PsiReferenceListImpl");

    public PsiReferenceListImpl(PsiClassReferenceListStub stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public PsiReferenceListImpl(ASTNode node) {
        super(node);
    }

    @Override
    
    public PsiJavaCodeReferenceElement[] getReferenceElements() {
        return calcTreeElement().getChildrenAsPsiElements(JavaElementType.JAVA_CODE_REFERENCE, PsiJavaCodeReferenceElement.ARRAY_FACTORY);
    }

    @Override
    
    public PsiClassType[] getReferencedTypes() {
        PsiClassReferenceListStub stub = getStub();
        if (stub != null) {
            return stub.getReferencedTypes();
        }

        PsiJavaCodeReferenceElement[] refs = getReferenceElements();
        PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
        PsiClassType[] types = new PsiClassType[refs.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = factory.createType(refs[i]);
        }

        return types;
    }

    @Override
    public Role getRole() {
        IElementType type = getElementType();
        if (type == JavaElementType.EXTENDS_LIST) {
            return Role.EXTENDS_LIST;
        }
        else if (type == JavaElementType.IMPLEMENTS_LIST) {
            return Role.IMPLEMENTS_LIST;
        }
        else if (type == JavaElementType.THROWS_LIST) {
            return Role.THROWS_LIST;
        }
        else if (type == JavaElementType.EXTENDS_BOUND_LIST) {
            return Role.EXTENDS_BOUNDS_LIST;
        }

        LOG.error("Unknown element type:" + type);
        return null;
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

    public String toString() {
        return getElementType() == JavaElementType.EXTENDS_BOUND_LIST ? "PsiElement(EXTENDS_BOUND_LIST)"
                : "PsiReferenceList"; // todo[r.sh] fix test data
    }
}
