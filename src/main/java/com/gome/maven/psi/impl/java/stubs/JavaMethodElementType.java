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
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.cache.TypeInfo;
import com.gome.maven.psi.impl.java.stubs.impl.PsiMethodStubImpl;
import com.gome.maven.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.gome.maven.psi.impl.source.PsiAnnotationMethodImpl;
import com.gome.maven.psi.impl.source.PsiMethodImpl;
import com.gome.maven.psi.impl.source.tree.ElementType;
import com.gome.maven.psi.impl.source.tree.JavaDocElementType;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.LightTreeUtil;
import com.gome.maven.psi.impl.source.tree.java.AnnotationMethodElement;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.io.StringRef;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public abstract class JavaMethodElementType extends JavaStubElementType<PsiMethodStub, PsiMethod> {
    public JavaMethodElementType( final String name) {
        super(name);
    }

    @Override
    public PsiMethod createPsi( final PsiMethodStub stub) {
        return getPsiFactory(stub).createMethod(stub);
    }

    @Override
    public PsiMethod createPsi( final ASTNode node) {
        if (node instanceof AnnotationMethodElement) {
            return new PsiAnnotationMethodImpl(node);
        }
        else {
            return new PsiMethodImpl(node);
        }
    }

    @Override
    public PsiMethodStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        String name = null;
        boolean isConstructor = true;
        boolean isVarArgs = false;
        boolean isDeprecatedByComment = false;
        boolean hasDeprecatedAnnotation = false;
        String defValueText = null;

        boolean expectingDef = false;
        for (final LighterASTNode child : tree.getChildren(node)) {
            final IElementType type = child.getTokenType();
            if (type == JavaDocElementType.DOC_COMMENT) {
                isDeprecatedByComment = RecordUtil.isDeprecatedByDocComment(tree, child);
            }
            else if (type == JavaElementType.MODIFIER_LIST) {
                hasDeprecatedAnnotation = RecordUtil.isDeprecatedByAnnotation(tree, child);
            }
            else if (type == JavaElementType.TYPE) {
                isConstructor = false;
            }
            else if (type == JavaTokenType.IDENTIFIER) {
                name = RecordUtil.intern(tree.getCharTable(), child);
            }
            else if (type == JavaElementType.PARAMETER_LIST) {
                final List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, child, JavaElementType.PARAMETER);
                if (!params.isEmpty()) {
                    final LighterASTNode pType = LightTreeUtil.firstChildOfType(tree, params.get(params.size() - 1), JavaElementType.TYPE);
                    if (pType != null) {
                        isVarArgs = (LightTreeUtil.firstChildOfType(tree, pType, JavaTokenType.ELLIPSIS) != null);
                    }
                }
            }
            else if (type == JavaTokenType.DEFAULT_KEYWORD) {
                expectingDef = true;
            }
            else if (expectingDef && !ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(type) &&
                    type != JavaTokenType.SEMICOLON && type != JavaElementType.CODE_BLOCK) {
                defValueText = LightTreeUtil.toFilteredString(tree, child, null);
                break;
            }
        }

        TypeInfo typeInfo = isConstructor ? TypeInfo.createConstructorType() : TypeInfo.create(tree, node, parentStub);
        boolean isAnno = (node.getTokenType() == JavaElementType.ANNOTATION_METHOD);
        byte flags = PsiMethodStubImpl.packFlags(isConstructor, isAnno, isVarArgs, isDeprecatedByComment, hasDeprecatedAnnotation);

        return new PsiMethodStubImpl(parentStub, StringRef.fromString(name), typeInfo, flags, StringRef.fromString(defValueText));
    }

    @Override
    public void serialize( final PsiMethodStub stub,  final StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        TypeInfo.writeTYPE(dataStream, stub.getReturnTypeText(false));
        dataStream.writeByte(((PsiMethodStubImpl)stub).getFlags());
        if (stub.isAnnotationMethod()) {
            dataStream.writeName(stub.getDefaultValueText());
        }
    }

    
    @Override
    public PsiMethodStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        final TypeInfo type = TypeInfo.readTYPE(dataStream);
        byte flags = dataStream.readByte();
        final StringRef defaultMethodValue = PsiMethodStubImpl.isAnnotationMethod(flags) ? dataStream.readName() : null;
        return new PsiMethodStubImpl(parentStub, name, type, flags, defaultMethodValue);
    }

    @Override
    public void indexStub( final PsiMethodStub stub,  final IndexSink sink) {
        final String name = stub.getName();
        if (name != null) {
            sink.occurrence(JavaStubIndexKeys.METHODS, name);
            if (RecordUtil.isStaticNonPrivateMember(stub)) {
                sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_NAMES, name);
                sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_TYPES, stub.getReturnTypeText(false).getShortTypeText());
            }
        }
    }
}
