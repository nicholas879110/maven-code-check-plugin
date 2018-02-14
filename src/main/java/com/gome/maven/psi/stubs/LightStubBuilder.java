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
package com.gome.maven.psi.stubs;

import com.gome.maven.lang.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diagnostic.LogUtil;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.StubBuilder;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.IFileElementType;
import com.gome.maven.psi.tree.ILightStubFileElementType;
import com.gome.maven.psi.tree.IStubFileElementType;
import com.gome.maven.util.containers.Stack;
import gnu.trove.TIntStack;

import java.util.List;

public class LightStubBuilder implements StubBuilder {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.stubs.LightStubBuilder");
    public static final ThreadLocal<LighterAST> FORCED_AST = new ThreadLocal<LighterAST>();

    @Override
    public StubElement buildStubTree( PsiFile file) {
        LighterAST tree = FORCED_AST.get();
        if (tree == null) {
            FileType fileType = file.getFileType();
            if (!(fileType instanceof LanguageFileType)) {
                LOG.error("File is not of LanguageFileType: " + fileType + ", " + file);
                return null;
            }
            Language language = ((LanguageFileType)fileType).getLanguage();
            final IFileElementType contentType = LanguageParserDefinitions.INSTANCE.forLanguage(language).getFileNodeType();
            if (!(contentType instanceof IStubFileElementType)) {
                LOG.error("File is not of IStubFileElementType: " + contentType + ", " + file);
                return null;
            }

            final FileASTNode node = file.getNode();
            if (contentType instanceof ILightStubFileElementType) {
                tree = node.getLighterAST();
            }
            else {
                tree = new TreeBackedLighterAST(node);
            }
        } else {
            FORCED_AST.set(null);
        }
        if (tree == null) return null;

        final StubElement rootStub = createStubForFile(file, tree);
        buildStubTree(tree, tree.getRoot(), rootStub);
        return rootStub;
    }

    
    @SuppressWarnings("unchecked")
    protected StubElement createStubForFile( PsiFile file,  LighterAST tree) {
        return new PsiFileStubImpl(file);
    }

    protected void buildStubTree( LighterAST tree,  LighterASTNode root,  StubElement rootStub) {
        final Stack<LighterASTNode> parents = new Stack<LighterASTNode>();
        final TIntStack childNumbers = new TIntStack();
        final Stack<List<LighterASTNode>> kinderGarden = new Stack<List<LighterASTNode>>();
        final Stack<StubElement> parentStubs = new Stack<StubElement>();

        LighterASTNode parent = null;
        LighterASTNode element = root;
        List<LighterASTNode> children = null;
        int childNumber = 0;
        StubElement parentStub = rootStub;

        nextElement:
        while (element != null) {
            final StubElement stub = createStub(tree, element, parentStub);

            if (parent == null || !skipNode(tree, parent, element)) {
                final List<LighterASTNode> kids = tree.getChildren(element);
                if (!kids.isEmpty()) {
                    if (parent != null) {
                        parents.push(parent);
                        childNumbers.push(childNumber);
                        kinderGarden.push(children);
                        parentStubs.push(parentStub);
                    }
                    parent = element;
                    element = (children = kids).get(childNumber = 0);
                    parentStub = stub;
                    if (!skipNode(tree, parent, element)) continue nextElement;
                }
            }

            while (children != null && ++childNumber < children.size()) {
                element = children.get(childNumber);
                if (!skipNode(tree, parent, element)) continue nextElement;
            }

            element = null;
            while (!parents.isEmpty()) {
                parent = parents.pop();
                childNumber = childNumbers.pop();
                children = kinderGarden.pop();
                parentStub = parentStubs.pop();
                while (++childNumber < children.size()) {
                    element = children.get(childNumber);
                    if (!skipNode(tree, parent, element)) continue nextElement;
                }
                element = null;
            }
        }
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    protected StubElement createStub(final LighterAST tree, final LighterASTNode element, final StubElement parentStub) {
        final IElementType elementType = element.getTokenType();
        if (elementType instanceof IStubElementType) {
            if (elementType instanceof ILightStubElementType) {
                final ILightStubElementType lightElementType = (ILightStubElementType)elementType;
                if (lightElementType.shouldCreateStub(tree, element, parentStub)) {
                    return lightElementType.createStub(tree, element, parentStub);
                }
            }
            else {
                LOG.error("Element is not of ILightStubElementType: " + LogUtil.objectAndClass(elementType) + ", " + element);
            }
        }

        return parentStub;
    }

    private boolean skipNode( LighterAST tree,  LighterASTNode parent,  LighterASTNode node) {
        if (tree instanceof TreeBackedLighterAST) {
            return skipChildProcessingWhenBuildingStubs(((TreeBackedLighterAST)tree).unwrap(parent), ((TreeBackedLighterAST)tree).unwrap(node));
        }
        else {
            return skipChildProcessingWhenBuildingStubs(tree, parent, node);
        }
    }

    /**
     * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(LighterAST, LighterASTNode, LighterASTNode)}.
     */
    @Override
    public boolean skipChildProcessingWhenBuildingStubs( ASTNode parent,  ASTNode node) {
        return false;
    }

    /**
     * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(ASTNode, ASTNode)}.
     */
    protected boolean skipChildProcessingWhenBuildingStubs( LighterAST tree,  LighterASTNode parent,  LighterASTNode node) {
        return false;
    }
}
