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
package com.gome.maven.psi.impl.java.stubs;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.LighterAST;
import com.gome.maven.lang.LighterASTNode;
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.PsiParameter;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.cache.TypeInfo;
import com.gome.maven.psi.impl.java.stubs.impl.PsiParameterStubImpl;
import com.gome.maven.psi.impl.source.PsiParameterImpl;
import com.gome.maven.psi.impl.source.PsiReceiverParameterImpl;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.LightTreeUtil;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.io.StringRef;

import java.io.IOException;

/**
 * @author max
 */
public abstract class JavaParameterElementType extends JavaStubElementType<PsiParameterStub, PsiParameter> {
    public static final TokenSet ID_TYPES = TokenSet.create(JavaTokenType.IDENTIFIER, JavaTokenType.THIS_KEYWORD);

    public JavaParameterElementType( String id) {
        super(id);
    }

    @Override
    public PsiParameter createPsi( PsiParameterStub stub) {
        return getPsiFactory(stub).createParameter(stub);
    }

    @Override
    public PsiParameter createPsi( ASTNode node) {
        boolean receiver = node.getElementType() == JavaElementType.RECEIVER_PARAMETER;
        return receiver ? new PsiReceiverParameterImpl(node) : new PsiParameterImpl(node);
    }

    @Override
    public PsiParameterStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        TypeInfo typeInfo = TypeInfo.create(tree, node, parentStub);
        LighterASTNode id = LightTreeUtil.requiredChildOfType(tree, node, ID_TYPES);
        String name = RecordUtil.intern(tree.getCharTable(), id);
        return new PsiParameterStubImpl(parentStub, name, typeInfo, typeInfo.isEllipsis);
    }

    @Override
    public void serialize( PsiParameterStub stub,  StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        TypeInfo.writeTYPE(dataStream, stub.getType(false));
        dataStream.writeBoolean(stub.isParameterTypeEllipsis());
    }

    
    @Override
    public PsiParameterStub deserialize( StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        TypeInfo type = TypeInfo.readTYPE(dataStream);
        boolean isEllipsis = dataStream.readBoolean();
        return new PsiParameterStubImpl(parentStub, name, type, isEllipsis);
    }

    @Override
    public void indexStub( PsiParameterStub stub,  IndexSink sink) {
    }
}
