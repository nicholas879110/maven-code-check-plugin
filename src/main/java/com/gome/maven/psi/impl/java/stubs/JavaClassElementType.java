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
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiNameHelper;
import com.gome.maven.psi.impl.cache.RecordUtil;
import com.gome.maven.psi.impl.java.stubs.impl.PsiClassStubImpl;
import com.gome.maven.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.gome.maven.psi.impl.source.PsiAnonymousClassImpl;
import com.gome.maven.psi.impl.source.PsiClassImpl;
import com.gome.maven.psi.impl.source.PsiEnumConstantInitializerImpl;
import com.gome.maven.psi.impl.source.tree.JavaDocElementType;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.LightTreeUtil;
import com.gome.maven.psi.impl.source.tree.java.AnonymousClassElement;
import com.gome.maven.psi.impl.source.tree.java.EnumConstantInitializerElement;
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
public abstract class JavaClassElementType extends JavaStubElementType<PsiClassStub, PsiClass> {
    public JavaClassElementType(  final String id) {
        super(id);
    }

    @Override
    public PsiClass createPsi( final PsiClassStub stub) {
        return getPsiFactory(stub).createClass(stub);
    }

    @Override
    public PsiClass createPsi( final ASTNode node) {
        if (node instanceof EnumConstantInitializerElement) {
            return new PsiEnumConstantInitializerImpl(node);
        }
        else if (node instanceof AnonymousClassElement) {
            return new PsiAnonymousClassImpl(node);
        }

        return new PsiClassImpl(node);
    }

    @Override
    public PsiClassStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
        boolean isDeprecatedByComment = false;
        boolean isInterface = false;
        boolean isEnum = false;
        boolean isEnumConst = false;
        boolean isAnonymous = false;
        boolean isAnnotation = false;
        boolean isInQualifiedNew = false;
        boolean hasDeprecatedAnnotation = false;

        String qualifiedName = null;
        String name = null;
        String baseRef = null;

        if (node.getTokenType() == JavaElementType.ANONYMOUS_CLASS) {
            isAnonymous = true;
        }
        else if (node.getTokenType() == JavaElementType.ENUM_CONSTANT_INITIALIZER) {
            isAnonymous = isEnumConst = true;
            baseRef = ((PsiClassStub)parentStub.getParentStub()).getName();
        }

        for (final LighterASTNode child : tree.getChildren(node)) {
            final IElementType type = child.getTokenType();
            if (type == JavaDocElementType.DOC_COMMENT) {
                isDeprecatedByComment = RecordUtil.isDeprecatedByDocComment(tree, child);
            }
            else if (type == JavaElementType.MODIFIER_LIST) {
                hasDeprecatedAnnotation = RecordUtil.isDeprecatedByAnnotation(tree, child);
            }
            else if (type == JavaTokenType.AT) {
                isAnnotation = true;
            }
            else if (type == JavaTokenType.INTERFACE_KEYWORD) {
                isInterface = true;
            }
            else if (type == JavaTokenType.ENUM_KEYWORD) {
                isEnum = true;
            }
            else if (!isAnonymous && type == JavaTokenType.IDENTIFIER) {
                name = RecordUtil.intern(tree.getCharTable(), child);
            }
            else if (isAnonymous && !isEnumConst && type == JavaElementType.JAVA_CODE_REFERENCE) {
                baseRef = LightTreeUtil.toFilteredString(tree, child, null);
            }
        }

        if (name != null) {
            if (parentStub instanceof PsiJavaFileStub) {
                final String pkg = ((PsiJavaFileStub)parentStub).getPackageName();
                if (!pkg.isEmpty()) qualifiedName = pkg + '.' + name; else qualifiedName = name;
            }
            else if (parentStub instanceof PsiClassStub) {
                final String parentFqn = ((PsiClassStub)parentStub).getQualifiedName();
                qualifiedName = parentFqn != null ? parentFqn + '.' + name : null;
            }
        }

        if (isAnonymous) {
            final LighterASTNode parent = tree.getParent(node);
            if (parent != null && parent.getTokenType() == JavaElementType.NEW_EXPRESSION) {
                isInQualifiedNew = (LightTreeUtil.firstChildOfType(tree, parent, JavaTokenType.DOT) != null);
            }
        }

        final byte flags = PsiClassStubImpl.packFlags(isDeprecatedByComment, isInterface, isEnum, isEnumConst, isAnonymous, isAnnotation,
                isInQualifiedNew, hasDeprecatedAnnotation);
        final JavaClassElementType type = typeForClass(isAnonymous, isEnumConst);
        return new PsiClassStubImpl(type, parentStub, qualifiedName, name, baseRef, flags);
    }

    public static JavaClassElementType typeForClass(final boolean anonymous, final boolean enumConst) {
        return enumConst
                ? JavaStubElementTypes.ENUM_CONSTANT_INITIALIZER
                : anonymous ? JavaStubElementTypes.ANONYMOUS_CLASS : JavaStubElementTypes.CLASS;
    }

    @Override
    public void serialize( final PsiClassStub stub,  final StubOutputStream dataStream) throws IOException {
        dataStream.writeByte(((PsiClassStubImpl)stub).getFlags());
        if (!stub.isAnonymous()) {
            dataStream.writeName(stub.getName());
            dataStream.writeName(stub.getQualifiedName());
            dataStream.writeByte(stub.getLanguageLevel().ordinal());
            dataStream.writeName(stub.getSourceFileName());
        }
        else {
            dataStream.writeName(stub.getBaseClassReferenceText());
        }
    }

    
    @Override
    public PsiClassStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        byte flags = dataStream.readByte();
        boolean isAnonymous = PsiClassStubImpl.isAnonymous(flags);
        boolean isEnumConst = PsiClassStubImpl.isEnumConstInitializer(flags);
        JavaClassElementType type = typeForClass(isAnonymous, isEnumConst);

        if (!isAnonymous) {
            StringRef name = dataStream.readName();
            StringRef qname = dataStream.readName();
            int languageLevelId = dataStream.readByte();
            StringRef sourceFileName = dataStream.readName();
            PsiClassStubImpl classStub = new PsiClassStubImpl(type, parentStub, qname, name, null, flags);
            classStub.setLanguageLevel(LanguageLevel.values()[languageLevelId]);
            classStub.setSourceFileName(sourceFileName);
            return classStub;
        }
        else {
            StringRef baseRef = dataStream.readName();
            return new PsiClassStubImpl(type, parentStub, null, null, baseRef, flags);
        }
    }

    @Override
    public void indexStub( final PsiClassStub stub,  final IndexSink sink) {
        boolean isAnonymous = stub.isAnonymous();
        if (isAnonymous) {
            String baseRef = stub.getBaseClassReferenceText();
            if (baseRef != null) {
                sink.occurrence(JavaStubIndexKeys.ANONYMOUS_BASEREF, PsiNameHelper.getShortClassName(baseRef));
            }
        }
        else {
            final String shortName = stub.getName();
            if (shortName != null) {
                sink.occurrence(JavaStubIndexKeys.CLASS_SHORT_NAMES, shortName);
            }

            final String fqn = stub.getQualifiedName();
            if (fqn != null) {
                sink.occurrence(JavaStubIndexKeys.CLASS_FQN, fqn.hashCode());
            }
        }
    }

    @Override
    public String getId(final PsiClassStub stub) {
        final String name = stub.getName();
        return name != null ? name : super.getId(stub);
    }
}
