/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.gome.maven.lang.LighterAST;
import com.gome.maven.lang.LighterASTNode;
import com.gome.maven.lang.LighterLazyParseableNode;
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiJavaFile;
import com.gome.maven.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.stubs.LightStubBuilder;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.io.StringRef;

public class JavaLightStubBuilder extends LightStubBuilder {
    
    @Override
    protected StubElement createStubForFile( PsiFile file,  LighterAST tree) {
        if (!(file instanceof PsiJavaFile)) {
            return super.createStubForFile(file, tree);
        }

        String refText = "";
        LighterASTNode pkg = LightTreeUtil.firstChildOfType(tree, tree.getRoot(), JavaElementType.PACKAGE_STATEMENT);
        if (pkg != null) {
            LighterASTNode ref = LightTreeUtil.firstChildOfType(tree, pkg, JavaElementType.JAVA_CODE_REFERENCE);
            if (ref != null) {
                refText = JavaSourceUtil.getReferenceText(tree, ref);
            }
        }
        return new PsiJavaFileStubImpl((PsiJavaFile)file, StringRef.fromString(refText), false);
    }

    @Override
    public boolean skipChildProcessingWhenBuildingStubs( ASTNode parent,  ASTNode node) {
        IElementType parentType = parent.getElementType();
        IElementType nodeType = node.getElementType();

        if (checkByTypes(parentType, nodeType)) return true;

        if (nodeType == JavaElementType.CODE_BLOCK) {
            CodeBlockVisitor visitor = new CodeBlockVisitor();
            ((TreeElement)node).acceptTree(visitor);
            return visitor.result;
        }

        return false;
    }

    @Override
    protected boolean skipChildProcessingWhenBuildingStubs( LighterAST tree,  LighterASTNode parent,  LighterASTNode node) {
        IElementType parentType = parent.getTokenType();
        IElementType nodeType = node.getTokenType();

        if (checkByTypes(parentType, nodeType)) return true;

        if (nodeType == JavaElementType.CODE_BLOCK) {
            CodeBlockVisitor visitor = new CodeBlockVisitor();
            ((LighterLazyParseableNode)node).accept(visitor);
            return visitor.result;
        }

        return false;
    }

    private static boolean checkByTypes(IElementType parentType, IElementType nodeType) {
        if (ElementType.IMPORT_STATEMENT_BASE_BIT_SET.contains(parentType)) {
            return true;
        }
        if (nodeType == JavaElementType.PARAMETER && parentType != JavaElementType.PARAMETER_LIST) {
            return true;
        }
        if (nodeType == JavaElementType.PARAMETER_LIST && parentType == JavaElementType.LAMBDA_EXPRESSION) {
            return true;
        }

        return false;
    }

    private static class CodeBlockVisitor extends RecursiveTreeElementWalkingVisitor implements LighterLazyParseableNode.Visitor {
        private static final TokenSet BLOCK_ELEMENTS = TokenSet.create(
                JavaElementType.ANNOTATION, JavaElementType.CLASS, JavaElementType.ANONYMOUS_CLASS);

        private boolean result = true;

        @Override
        protected void visitNode(TreeElement element) {
            if (BLOCK_ELEMENTS.contains(element.getElementType())) {
                result = false;
                stopWalking();
                return;
            }
            super.visitNode(element);
        }

        private IElementType last = null;
        private boolean seenNew = false;

        @Override
        public boolean visit(IElementType type) {
            if (ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(type)) {
                return true;
            }

            // annotations
            if (type == JavaTokenType.AT) {
                return (result = false);
            }
            // anonymous classes
            else if (type == JavaTokenType.NEW_KEYWORD) {
                seenNew = true;
            }
            else if (seenNew && type == JavaTokenType.SEMICOLON) {
                seenNew = false;
            }
            else if (seenNew && type == JavaTokenType.LBRACE && last != JavaTokenType.RBRACKET) {
                return (result = false);
            }
            // local classes
            else if (type == JavaTokenType.CLASS_KEYWORD && last != JavaTokenType.DOT) {
                return (result = false);
            }

            last = type;
            return true;
        }
    }
}
