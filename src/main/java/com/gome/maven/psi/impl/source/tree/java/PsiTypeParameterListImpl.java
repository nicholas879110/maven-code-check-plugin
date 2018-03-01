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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiTypeParameterListStub;
import com.gome.maven.psi.impl.source.JavaStubPsiElement;
import com.gome.maven.psi.scope.PsiScopeProcessor;

/**
 *  @author dsl
 */
public class PsiTypeParameterListImpl extends JavaStubPsiElement<PsiTypeParameterListStub> implements PsiTypeParameterList {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiTypeParameterListImpl");

    public PsiTypeParameterListImpl(final PsiTypeParameterListStub stub) {
        super(stub, JavaStubElementTypes.TYPE_PARAMETER_LIST);
    }

    public PsiTypeParameterListImpl(final ASTNode node) {
        super(node);
    }

    @Override
    public PsiTypeParameter[] getTypeParameters() {
        return getStubOrPsiChildren(JavaStubElementTypes.TYPE_PARAMETER, PsiTypeParameter.ARRAY_FACTORY);
    }

    @Override
    public int getTypeParameterIndex(PsiTypeParameter typeParameter) {
        LOG.assertTrue(typeParameter.getParent() == this);
        return PsiImplUtil.getTypeParameterIndex(typeParameter, this);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,  ResolveState state, PsiElement lastParent,  PsiElement place) {
        final PsiTypeParameter[] parameters = getTypeParameters();
        for (final PsiTypeParameter parameter : parameters) {
            if (!processor.execute(parameter, state)) return false;
        }
        return true;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitTypeParameterList(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiTypeParameterList";
    }
}
