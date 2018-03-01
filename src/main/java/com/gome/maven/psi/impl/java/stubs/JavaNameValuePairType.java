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
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.PsiNameValuePair;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.java.stubs.impl.PsiNameValuePairStubImpl;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.java.NameValuePairElement;
import com.gome.maven.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;
import com.gome.maven.util.io.StringRef;

import java.io.IOException;
import java.util.List;

/**
 * @author Dmitry Avdeev
 *         Date: 7/27/12
 */
public class JavaNameValuePairType extends JavaStubElementType<PsiNameValuePairStub, PsiNameValuePair> {

    protected JavaNameValuePairType() {
        super("NAME_VALUE_PAIR", true);
    }

    @Override
    public PsiNameValuePair createPsi( ASTNode node) {
        return new PsiNameValuePairImpl(node);
    }

    
    @Override
    public ASTNode createCompositeNode() {
        return new NameValuePairElement();
    }

    @Override
    public PsiNameValuePairStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        String name = null;
        String value = null;
        List<LighterASTNode> children = tree.getChildren(node);
        for (LighterASTNode child : children) {
            if (child.getTokenType() == JavaTokenType.IDENTIFIER) {
                name = RecordUtil.intern(tree.getCharTable(), child);
            }
            else if (child.getTokenType() == JavaElementType.LITERAL_EXPRESSION) {
                value = RecordUtil.intern(tree.getCharTable(), tree.getChildren(child).get(0));
                value = StringUtil.unquoteString(value);
            }
        }
        return new PsiNameValuePairStubImpl(parentStub, StringRef.fromString(name), StringRef.fromString(value));
    }

    @Override
    public PsiNameValuePair createPsi( PsiNameValuePairStub stub) {
        return getPsiFactory(stub).createNameValuePair(stub);
    }

    @Override
    public void serialize( PsiNameValuePairStub stub,  StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeName(stub.getValue());
    }

    
    @Override
    public PsiNameValuePairStub deserialize( StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        return new PsiNameValuePairStubImpl(parentStub, name, dataStream.readName());
    }

    @Override
    public void indexStub( PsiNameValuePairStub stub,  IndexSink sink) {
    }
}
