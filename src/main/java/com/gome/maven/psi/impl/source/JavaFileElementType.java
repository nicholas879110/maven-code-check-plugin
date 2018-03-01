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
package com.gome.maven.psi.impl.source;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.LighterASTNode;
import com.gome.maven.lang.PsiBuilder;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.lang.java.parser.JavaParser;
import com.gome.maven.lang.java.parser.JavaParserUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.impl.java.stubs.PsiJavaFileStub;
import com.gome.maven.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.gome.maven.psi.impl.source.tree.java.JavaFileElement;
import com.gome.maven.psi.stubs.*;
import com.gome.maven.psi.tree.ILightStubFileElementType;
import com.gome.maven.util.diff.FlyweightCapableTreeStructure;
import com.gome.maven.util.io.StringRef;

import java.io.IOException;

/**
 * @author max
 */
public class JavaFileElementType extends ILightStubFileElementType<PsiJavaFileStub> {
    public static final int STUB_VERSION = 19;

    public JavaFileElementType() {
        super("java.FILE", JavaLanguage.INSTANCE);
    }

    @Override
    public LightStubBuilder getBuilder() {
        return new JavaLightStubBuilder();
    }

    @Override
    public int getStubVersion() {
        return STUB_VERSION;
    }

    @Override
    public boolean shouldBuildStubFor(final VirtualFile file) {
        final VirtualFile dir = file.getParent();
        return dir == null || dir.getUserData(LanguageLevel.KEY) != null;
    }

    @Override
    public ASTNode createNode(final CharSequence text) {
        return new JavaFileElement(text);
    }

    @Override
    public FlyweightCapableTreeStructure<LighterASTNode> parseContentsLight(final ASTNode chameleon) {
        final PsiBuilder builder = JavaParserUtil.createBuilder(chameleon);
        doParse(builder);
        return builder.getLightTree();
    }

    @Override
    public ASTNode parseContents(final ASTNode chameleon) {
        final PsiBuilder builder = JavaParserUtil.createBuilder(chameleon);
        doParse(builder);
        return builder.getTreeBuilt().getFirstChildNode();
    }

    private void doParse(final PsiBuilder builder) {
        final PsiBuilder.Marker root = builder.mark();
        JavaParser.INSTANCE.getFileParser().parse(builder);
        root.done(this);
    }

    
    @Override
    public String getExternalId() {
        return "java.FILE";
    }

    @Override
    public void serialize( final PsiJavaFileStub stub,  final StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isCompiled());
        dataStream.writeName(stub.getPackageName());
    }

    
    @Override
    public PsiJavaFileStub deserialize( final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        boolean compiled = dataStream.readBoolean();
        StringRef packName = dataStream.readName();
        return new PsiJavaFileStubImpl(null, packName, compiled);
    }

    @Override
    public void indexStub( final PsiJavaFileStub stub,  final IndexSink sink) { }
}
