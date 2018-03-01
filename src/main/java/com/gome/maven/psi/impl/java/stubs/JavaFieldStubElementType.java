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
import com.gome.maven.psi.PsiField;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.cache.TypeInfo;
import com.gome.maven.psi.impl.java.stubs.impl.PsiFieldStubImpl;
import com.gome.maven.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.gome.maven.psi.impl.source.PsiEnumConstantImpl;
import com.gome.maven.psi.impl.source.PsiFieldImpl;
import com.gome.maven.psi.impl.source.tree.ElementType;
import com.gome.maven.psi.impl.source.tree.JavaDocElementType;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.LightTreeUtil;
import com.gome.maven.psi.impl.source.tree.java.EnumConstantElement;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.util.io.StringRef;

import java.io.IOException;

/**
 * @author max
 */
public abstract class JavaFieldStubElementType extends JavaStubElementType<PsiFieldStub, PsiField> {
    private static final int INITIALIZER_LENGTH_LIMIT = 1000;

    public JavaFieldStubElementType(  final String id) {
        super(id);
    }

    @Override
    public PsiField createPsi( final PsiFieldStub stub) {
        return getPsiFactory(stub).createField(stub);
    }

    @Override
    public PsiField createPsi( final ASTNode node) {
        if (node instanceof EnumConstantElement) {
            return new PsiEnumConstantImpl(node);
        }
        else {
            return new PsiFieldImpl(node);
        }
    }

    @Override
    public PsiFieldStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        final TypeInfo typeInfo = TypeInfo.create(tree, node, parentStub);

        boolean isDeprecatedByComment = false;
        boolean hasDeprecatedAnnotation = false;
        String name = null;
        String initializer = null;

        boolean expectingInit = false;
        for (final LighterASTNode child : tree.getChildren(node)) {
            final IElementType type = child.getTokenType();
            if (type == JavaDocElementType.DOC_COMMENT) {
                isDeprecatedByComment = RecordUtil.isDeprecatedByDocComment(tree, child);
            }
            else if (type == JavaElementType.MODIFIER_LIST) {
                hasDeprecatedAnnotation = RecordUtil.isDeprecatedByAnnotation(tree, child);
            }
            else if (type == JavaTokenType.IDENTIFIER) {
                name = RecordUtil.intern(tree.getCharTable(), child);
            }
            else if (type == JavaTokenType.EQ) {
                expectingInit = true;
            }
            else if (expectingInit && !ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(type) && type != JavaTokenType.SEMICOLON) {
                initializer = encodeInitializer(tree, child);
                break;
            }
        }

        final boolean isEnumConst = node.getTokenType() == JavaElementType.ENUM_CONSTANT;
        final byte flags = PsiFieldStubImpl.packFlags(isEnumConst, isDeprecatedByComment, hasDeprecatedAnnotation);

        return new PsiFieldStubImpl(parentStub, name, typeInfo, initializer, flags);
    }

    private static String encodeInitializer(final LighterAST tree, final LighterASTNode initializer) {
        final IElementType type = initializer.getTokenType();
        if (type == JavaElementType.NEW_EXPRESSION || type == JavaElementType.METHOD_CALL_EXPRESSION) {
            return PsiFieldStub.INITIALIZER_NOT_STORED;
        }

        if (initializer.getEndOffset() - initializer.getStartOffset() > INITIALIZER_LENGTH_LIMIT) {
            return PsiFieldStub.INITIALIZER_TOO_LONG;
        }

        return LightTreeUtil.toFilteredString(tree, initializer, null);
    }

    @Override
    public void serialize( final PsiFieldStub stub,  final StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        TypeInfo.writeTYPE(dataStream, stub.getType(false));
        dataStream.writeName(stub.getInitializerText());
        dataStream.writeByte(((PsiFieldStubImpl)stub).getFlags());
    }

    
    @Override
    public PsiFieldStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        final StringRef name = dataStream.readName();
        final TypeInfo type = TypeInfo.readTYPE(dataStream);
        final StringRef initializerText = dataStream.readName();
        final byte flags = dataStream.readByte();
        return new PsiFieldStubImpl(parentStub, name, type, initializerText, flags);
    }

    @Override
    public void indexStub( final PsiFieldStub stub,  final IndexSink sink) {
        final String name = stub.getName();
        if (name != null) {
            sink.occurrence(JavaStubIndexKeys.FIELDS, name);
            if (RecordUtil.isStaticNonPrivateMember(stub)) {
                sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_NAMES, name);
                sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_TYPES, stub.getType(false).getShortTypeText());
            }
        }
    }

    @Override
    public String getId(final PsiFieldStub stub) {
        final String name = stub.getName();
        if (name != null) return name;

        return super.getId(stub);
    }
}
