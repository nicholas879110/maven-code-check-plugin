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
package com.gome.maven.psi.stubs;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.StubBasedPsiElement;
import com.gome.maven.psi.StubBuilder;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.util.containers.Stack;

/**
 * @author max
 */
public class DefaultStubBuilder implements StubBuilder {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.stubs.DefaultStubBuilder");

    @Override
    public StubElement buildStubTree( PsiFile file) {
        return buildStubTreeFor(file, createStubForFile(file));
    }

    
    protected StubElement createStubForFile( PsiFile file) {
        @SuppressWarnings("unchecked") PsiFileStubImpl stub = new PsiFileStubImpl(file);
        return stub;
    }

    
    private StubElement buildStubTreeFor( PsiElement root,  StubElement parentStub) {
        Stack<StubElement> parentStubs = new Stack<StubElement>();
        Stack<PsiElement> parentElements = new Stack<PsiElement>();
        parentElements.push(root);
        parentStubs.push(parentStub);

        while (!parentElements.isEmpty()) {
            StubElement stub = parentStubs.pop();
            PsiElement elt = parentElements.pop();

            if (elt instanceof StubBasedPsiElement) {
                final IStubElementType type = ((StubBasedPsiElement)elt).getElementType();

                if (type.shouldCreateStub(elt.getNode())) {
                    @SuppressWarnings("unchecked") StubElement s = type.createStub(elt, stub);
                    stub = s;
                }
            }
            else {
                final ASTNode node = elt.getNode();
                final IElementType type = node == null? null : node.getElementType();
                if (type instanceof IStubElementType && ((IStubElementType)type).shouldCreateStub(node)) {
                    LOG.error("Non-StubBasedPsiElement requests stub creation. Stub type: " + type + ", PSI: " + elt);
                }
            }

            for (PsiElement child = elt.getLastChild(); child != null; child = child.getPrevSibling()) {
                if (!skipChildProcessingWhenBuildingStubs(elt, child)) {
                    parentStubs.push(stub);
                    parentElements.push(child);
                }
            }
        }
        return parentStub;
    }

    /**
     * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(ASTNode, ASTNode)}.
     */
    protected boolean skipChildProcessingWhenBuildingStubs( PsiElement parent,  PsiElement element) {
        return false;
    }

    
    protected StubElement buildStubTreeFor( ASTNode root,  StubElement parentStub) {
        Stack<StubElement> parentStubs = new Stack<StubElement>();
        Stack<ASTNode> parentNodes = new Stack<ASTNode>();
        parentNodes.push(root);
        parentStubs.push(parentStub);

        while (!parentStubs.isEmpty()) {
            StubElement stub = parentStubs.pop();
            ASTNode node = parentNodes.pop();
            IElementType nodeType = node.getElementType();

            if (nodeType instanceof IStubElementType) {
                final IStubElementType type = (IStubElementType)nodeType;

                if (type.shouldCreateStub(node)) {
                    PsiElement element = node.getPsi();
                    if (!(element instanceof StubBasedPsiElement)) {
                        LOG.error("Non-StubBasedPsiElement requests stub creation. Stub type: " + type + ", PSI: " + element);
                    }
                    @SuppressWarnings("unchecked") StubElement s = type.createStub(element, stub);
                    stub = s;
                    LOG.assertTrue(stub != null, element);
                }
            }

            for (ASTNode childNode = node.getLastChildNode(); childNode != null; childNode = childNode.getTreePrev()) {
                if (!skipChildProcessingWhenBuildingStubs(node, childNode)) {
                    parentNodes.push(childNode);
                    parentStubs.push(stub);
                }
            }
        }

        return parentStub;
    }

    /**
     * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(PsiElement, PsiElement)}.
     */
    @Override
    public boolean skipChildProcessingWhenBuildingStubs( ASTNode parent,  ASTNode node) {
        return false;
    }
}
