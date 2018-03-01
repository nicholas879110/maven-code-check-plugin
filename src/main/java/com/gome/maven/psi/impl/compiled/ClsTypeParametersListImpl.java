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

import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiTypeParameterListStub;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.scope.PsiScopeProcessor;

/**
 * @author max
 */
public class ClsTypeParametersListImpl extends ClsRepositoryPsiElement<PsiTypeParameterListStub> implements PsiTypeParameterList {
    public ClsTypeParametersListImpl( PsiTypeParameterListStub stub) {
        super(stub);
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        final PsiTypeParameter[] params = getTypeParameters();
        if (params.length != 0) {
            buffer.append('<');
            for (int i = 0; i < params.length; i++) {
                if (i > 0) buffer.append(", ");
                appendText(params[i], indentLevel, buffer);
            }
            buffer.append(">");
        }
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, null);
        setMirrors(getTypeParameters(), SourceTreeToPsiMap.<PsiTypeParameterList>treeToPsiNotNull(element).getTypeParameters());
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

    @Override
    public PsiTypeParameter[] getTypeParameters() {
        return getStub().getChildrenByType(JavaStubElementTypes.TYPE_PARAMETER, PsiTypeParameter.ARRAY_FACTORY);
    }

    @Override
    public int getTypeParameterIndex(PsiTypeParameter typeParameter) {
        assert typeParameter.getParent() == this;
        return PsiImplUtil.getTypeParameterIndex(typeParameter, this);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        final PsiTypeParameter[] typeParameters = getTypeParameters();
        for (PsiTypeParameter parameter : typeParameters) {
            if (!processor.execute(parameter, state)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PsiTypeParameterList";
    }
}
