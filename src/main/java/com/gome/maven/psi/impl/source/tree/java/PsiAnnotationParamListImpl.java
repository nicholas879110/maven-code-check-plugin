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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiAnnotationParameterList;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiNameValuePair;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiAnnotationParameterListStub;
import com.gome.maven.psi.impl.source.JavaStubPsiElement;

/**
 * @author Dmitry Avdeev
 * @since 27.07.2012
 */
public class PsiAnnotationParamListImpl extends JavaStubPsiElement<PsiAnnotationParameterListStub> implements PsiAnnotationParameterList {
    public PsiAnnotationParamListImpl( PsiAnnotationParameterListStub stub) {
        super(stub, JavaStubElementTypes.ANNOTATION_PARAMETER_LIST);
    }

    public PsiAnnotationParamListImpl( ASTNode node) {
        super(node);
    }

    
    @Override
    public PsiNameValuePair[] getAttributes() {
        return getStubOrPsiChildren(JavaStubElementTypes.NAME_VALUE_PAIR, PsiNameValuePair.ARRAY_FACTORY);
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitAnnotationParameterList(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public String toString() {
        return "PsiAnnotationParameterList";
    }
}
