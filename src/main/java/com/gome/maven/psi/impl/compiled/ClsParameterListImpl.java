/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiParameter;
import com.gome.maven.psi.PsiParameterList;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiParameterListStub;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;

public class ClsParameterListImpl extends ClsRepositoryPsiElement<PsiParameterListStub> implements PsiParameterList {
    public ClsParameterListImpl( PsiParameterListStub stub) {
        super(stub);
    }

    @Override
    
    public PsiParameter[] getParameters() {
        return getStub().getChildrenByType(JavaStubElementTypes.PARAMETER, PsiParameter.ARRAY_FACTORY);
    }

    @Override
    public int getParameterIndex(PsiParameter parameter) {
        assert parameter.getParent() == this;
        return PsiImplUtil.getParameterIndex(parameter, this);
    }

    @Override
    public int getParametersCount() {
        return getParameters().length;
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        buffer.append('(');
        PsiParameter[] parameters = getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) buffer.append(", ");
            appendText(parameters[i], indentLevel, buffer);
        }
        buffer.append(')');
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, null);
        setMirrors(getParameters(), SourceTreeToPsiMap.<PsiParameterList>treeToPsiNotNull(element).getParameters());
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitParameterList(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public String toString() {
        return "PsiParameterList";
    }
}
