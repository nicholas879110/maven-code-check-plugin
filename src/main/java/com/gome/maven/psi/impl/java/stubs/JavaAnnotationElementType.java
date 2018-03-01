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
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiAnnotation;
import com.gome.maven.psi.PsiNameHelper;
import com.gome.maven.psi.impl.java.stubs.impl.PsiAnnotationStubImpl;
import com.gome.maven.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.gome.maven.psi.impl.source.tree.LightTreeUtil;
import com.gome.maven.psi.impl.source.tree.java.AnnotationElement;
import com.gome.maven.psi.impl.source.tree.java.PsiAnnotationImpl;
import com.gome.maven.psi.stubs.IndexSink;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubInputStream;
import com.gome.maven.psi.stubs.StubOutputStream;

import java.io.IOException;

/**
 * @author max
 */
public class JavaAnnotationElementType extends JavaStubElementType<PsiAnnotationStub, PsiAnnotation> {
    public JavaAnnotationElementType() {
        super("ANNOTATION");
    }

    
    @Override
    public ASTNode createCompositeNode() {
        return new AnnotationElement();
    }

    @Override
    public PsiAnnotation createPsi( PsiAnnotationStub stub) {
        return getPsiFactory(stub).createAnnotation(stub);
    }

    @Override
    public PsiAnnotation createPsi( ASTNode node) {
        return new PsiAnnotationImpl(node);
    }

    @Override
    public PsiAnnotationStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        String text = LightTreeUtil.toFilteredString(tree, node, null);
        return new PsiAnnotationStubImpl(parentStub, text);
    }

    @Override
    public void serialize( PsiAnnotationStub stub,  StubOutputStream dataStream) throws IOException {
        dataStream.writeUTFFast(stub.getText());
    }

    
    @Override
    public PsiAnnotationStub deserialize( StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new PsiAnnotationStubImpl(parentStub, dataStream.readUTFFast());
    }

    @Override
    public void indexStub( PsiAnnotationStub stub,  IndexSink sink) {
        String shortName = getReferenceShortName(stub.getText());
        if (!StringUtil.isEmptyOrSpaces(shortName)) {
            sink.occurrence(JavaStubIndexKeys.ANNOTATIONS, shortName);
        }
    }

    private static String getReferenceShortName(String annotationText) {
        int index = annotationText.indexOf('(');
        if (index >= 0) annotationText = annotationText.substring(0, index);
        return PsiNameHelper.getShortClassName(annotationText);
    }
}
