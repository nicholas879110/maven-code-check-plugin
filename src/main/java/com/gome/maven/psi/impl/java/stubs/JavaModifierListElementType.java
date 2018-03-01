/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.gome.maven.psi.PsiModifierList;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.java.stubs.impl.PsiModifierListStubImpl;
import com.gome.maven.psi.impl.source.PsiModifierListImpl;
import com.gome.maven.psi.impl.source.tree.java.ModifierListElement;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;
import com.gome.maven.psi.tree.IElementType;

import java.io.IOException;

import static com.gome.maven.psi.impl.source.tree.JavaElementType.*;

/**
 * @author max
 */
public class JavaModifierListElementType extends JavaStubElementType<PsiModifierListStub, PsiModifierList> {
    public JavaModifierListElementType() {
        super("MODIFIER_LIST");
    }

    
    @Override
    public ASTNode createCompositeNode() {
        return new ModifierListElement();
    }

    @Override
    public PsiModifierList createPsi( final PsiModifierListStub stub) {
        return getPsiFactory(stub).createModifierList(stub);
    }

    @Override
    public PsiModifierList createPsi( final ASTNode node) {
        return new PsiModifierListImpl(node);
    }

    @Override
    public PsiModifierListStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        return new PsiModifierListStubImpl(parentStub, RecordUtil.packModifierList(tree, node, parentStub));
    }

    @Override
    public void serialize( final PsiModifierListStub stub,  final StubOutputStream dataStream) throws IOException {
        dataStream.writeVarInt(stub.getModifiersMask());
    }

    @Override
    public boolean shouldCreateStub(final ASTNode node) {
        final IElementType parentType = node.getTreeParent().getElementType();
        return shouldCreateStub(parentType);
    }

    @Override
    public boolean shouldCreateStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        final LighterASTNode parent = tree.getParent(node);
        final IElementType parentType = parent != null ? parent.getTokenType() : null;
        return shouldCreateStub(parentType);
    }

    private static boolean shouldCreateStub(IElementType parentType) {
        return parentType != null && parentType != LOCAL_VARIABLE && parentType != RESOURCE_VARIABLE && parentType != RESOURCE_LIST;
    }

    
    @Override
    public PsiModifierListStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        return new PsiModifierListStubImpl(parentStub, dataStream.readVarInt());
    }

    @Override
    public void indexStub( final PsiModifierListStub stub,  final IndexSink sink) { }
}
