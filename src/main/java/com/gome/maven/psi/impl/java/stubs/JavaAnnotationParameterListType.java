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
package com.gome.maven.psi.impl.java.stubs;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.LighterAST;
import com.gome.maven.lang.LighterASTNode;
import com.gome.maven.psi.PsiAnnotationParameterList;
import com.gome.maven.psi.impl.java.stubs.impl.PsiAnnotationParameterListStubImpl;
import com.gome.maven.psi.impl.source.tree.java.AnnotationParamListElement;
import com.gome.maven.psi.impl.source.tree.java.PsiAnnotationParamListImpl;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;

import java.io.IOException;

/**
 * @author Dmitry Avdeev
 *         Date: 7/27/12
 */
public class JavaAnnotationParameterListType extends JavaStubElementType<PsiAnnotationParameterListStub, PsiAnnotationParameterList> {

    protected JavaAnnotationParameterListType() {
        super("ANNOTATION_PARAMETER_LIST", true);
    }

    @Override
    public PsiAnnotationParameterList createPsi( ASTNode node) {
        return new PsiAnnotationParamListImpl(node);
    }

    
    @Override
    public ASTNode createCompositeNode() {
        return new AnnotationParamListElement();
    }

    @Override
    public PsiAnnotationParameterListStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        return new PsiAnnotationParameterListStubImpl(parentStub);
    }

    @Override
    public PsiAnnotationParameterList createPsi( PsiAnnotationParameterListStub stub) {
        return getPsiFactory(stub).createAnnotationParameterList(stub);
    }

    @Override
    public void serialize( PsiAnnotationParameterListStub stub,  StubOutputStream dataStream) throws IOException {
    }

    
    @Override
    public PsiAnnotationParameterListStub deserialize( StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new PsiAnnotationParameterListStubImpl(parentStub);
    }

    @Override
    public void indexStub( PsiAnnotationParameterListStub stub,  IndexSink sink) {
    }
}
