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
package com.gome.maven.psi.impl.java.stubs;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.LighterAST;
import com.gome.maven.lang.LighterASTNode;
import com.gome.maven.psi.PsiParameterList;
import com.gome.maven.psi.impl.java.stubs.impl.PsiParameterListStubImpl;
import com.gome.maven.psi.impl.source.PsiParameterListImpl;
import com.gome.maven.psi.impl.source.tree.java.ParameterListElement;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;

import java.io.IOException;

/**
 * @author max
 */
public class JavaParameterListElementType extends JavaStubElementType<PsiParameterListStub, PsiParameterList> {
    public JavaParameterListElementType() {
        super("PARAMETER_LIST");
    }

    
    @Override
    public ASTNode createCompositeNode() {
        return new ParameterListElement();
    }

    @Override
    public PsiParameterList createPsi( final PsiParameterListStub stub) {
        return getPsiFactory(stub).createParameterList(stub);
    }

    @Override
    public PsiParameterList createPsi( final ASTNode node) {
        return new PsiParameterListImpl(node);
    }

    @Override
    public PsiParameterListStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        return new PsiParameterListStubImpl(parentStub);
    }

    @Override
    public void serialize( final PsiParameterListStub stub,  final StubOutputStream dataStream) throws IOException {
    }

    
    @Override
    public PsiParameterListStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        return new PsiParameterListStubImpl(parentStub);
    }

    @Override
    public void indexStub( final PsiParameterListStub stub,  final IndexSink sink) {
    }
}
