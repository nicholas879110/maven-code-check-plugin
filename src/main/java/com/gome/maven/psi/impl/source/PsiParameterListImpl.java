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
package com.gome.maven.psi.impl.source;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiParameter;
import com.gome.maven.psi.PsiParameterList;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiParameterListStub;
import com.gome.maven.psi.impl.source.tree.CompositeElement;

public class PsiParameterListImpl extends JavaStubPsiElement<PsiParameterListStub> implements PsiParameterList {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.PsiParameterListImpl");

    public PsiParameterListImpl( PsiParameterListStub stub) {
        super(stub, JavaStubElementTypes.PARAMETER_LIST);
    }

    public PsiParameterListImpl( ASTNode node) {
        super(node);
    }

    @Override
    
    public PsiParameter[] getParameters() {
        return getStubOrPsiChildren(JavaStubElementTypes.PARAMETER, PsiParameter.ARRAY_FACTORY);
    }

    @Override
    public int getParameterIndex(PsiParameter parameter) {
        LOG.assertTrue(parameter.getParent() == this);
        return PsiImplUtil.getParameterIndex(parameter, this);
    }

    @Override
    
    public CompositeElement getNode() {
        return (CompositeElement)super.getNode();
    }

    @Override
    public int getParametersCount() {
        final PsiParameterListStub stub = getStub();
        if (stub != null) {
            return stub.getChildrenStubs().size();
        }

        return getNode().countChildren(Constants.PARAMETER_BIT_SET);
    }

    @Override
    public void accept( PsiElementVisitor visitor){
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitParameterList(this);
        }
        else {
            visitor.visitElement(this);
        }
    }


    public String toString(){
        return "PsiParameterList:" + getText();
    }
}