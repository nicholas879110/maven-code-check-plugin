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
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.PsiTypeParameter;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.java.stubs.impl.PsiTypeParameterStubImpl;
import com.gome.maven.psi.impl.source.tree.LightTreeUtil;
import com.gome.maven.psi.impl.source.tree.java.PsiTypeParameterImpl;
import com.gome.maven.psi.impl.source.tree.java.TypeParameterElement;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;
import com.gome.maven.util.io.StringRef;

import java.io.IOException;

/**
 * @author max
 */
public class JavaTypeParameterElementType extends JavaStubElementType<PsiTypeParameterStub, PsiTypeParameter> {
    public JavaTypeParameterElementType() {
        super("TYPE_PARAMETER");
    }

    
    @Override
    public ASTNode createCompositeNode() {
        return new TypeParameterElement();
    }

    @Override
    public PsiTypeParameter createPsi( final PsiTypeParameterStub stub) {
        return getPsiFactory(stub).createTypeParameter(stub);
    }

    @Override
    public PsiTypeParameter createPsi( final ASTNode node) {
        return new PsiTypeParameterImpl(node);
    }

    @Override
    public PsiTypeParameterStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        final LighterASTNode id = LightTreeUtil.requiredChildOfType(tree, node, JavaTokenType.IDENTIFIER);
        final String name = RecordUtil.intern(tree.getCharTable(), id);
        return new PsiTypeParameterStubImpl(parentStub, StringRef.fromString(name));
    }

    @Override
    public void serialize( final PsiTypeParameterStub stub,  final StubOutputStream dataStream) throws IOException {
        String name = stub.getName();
        dataStream.writeName(name);
    }

    
    @Override
    public PsiTypeParameterStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        return new PsiTypeParameterStubImpl(parentStub, name);
    }

    @Override
    public void indexStub( final PsiTypeParameterStub stub,  final IndexSink sink) {
    }
}
