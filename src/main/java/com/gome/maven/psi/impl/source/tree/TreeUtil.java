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

package com.gome.maven.psi.impl.source.tree;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.psi.StubBuilder;
import com.gome.maven.psi.impl.DebugUtil;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.stubs.IStubElementType;
import com.gome.maven.psi.stubs.StubBase;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.stubs.StubTree;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.IStrongWhitespaceHolderElementType;
import com.gome.maven.psi.tree.IStubFileElementType;
import com.gome.maven.psi.tree.TokenSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class TreeUtil {
    public static final Key<String> UNCLOSED_ELEMENT_PROPERTY = Key.create("UNCLOSED_ELEMENT_PROPERTY");

    private TreeUtil() {
    }

    public static void ensureParsed(ASTNode node) {
        if (node != null) {
            node.getFirstChildNode();
        }
    }

    public static void ensureParsedRecursively( ASTNode node) {
        ((TreeElement)node).acceptTree(new RecursiveTreeElementWalkingVisitor() { });
    }
    public static void ensureParsedRecursivelyCheckingProgress( ASTNode node,  final ProgressIndicator indicator) {
        ((TreeElement)node).acceptTree(new RecursiveTreeElementWalkingVisitor() {
            @Override
            public void visitLeaf(LeafElement leaf) {
                indicator.checkCanceled();
            }
        });
    }

    public static boolean isCollapsedChameleon(ASTNode node) {
        return node instanceof LazyParseableElement && !((LazyParseableElement)node).isParsed();
    }

    
    public static ASTNode findChildBackward(ASTNode parent, IElementType type) {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            ApplicationManager.getApplication().assertReadAccessAllowed();
        }
        for (ASTNode element = parent.getLastChildNode(); element != null; element = element.getTreePrev()) {
            if (element.getElementType() == type) return element;
        }
        return null;
    }

    
    public static ASTNode skipElements(ASTNode element, TokenSet types) {
        while (true) {
            if (element == null) return null;
            if (!types.contains(element.getElementType())) break;
            element = element.getTreeNext();
        }
        return element;
    }

    
    public static ASTNode skipElementsBack( ASTNode element, TokenSet types) {
        if (element == null) return null;
        if (!types.contains(element.getElementType())) return element;

        ASTNode parent = element.getTreeParent();
        ASTNode prev = element;
        while (prev instanceof CompositeElement) {
            if (!types.contains(prev.getElementType())) return prev;
            prev = prev.getTreePrev();
        }
        if (prev == null) return null;
        ASTNode firstChildNode = parent.getFirstChildNode();
        ASTNode lastRelevant = null;
        while (firstChildNode != prev) {
            if (!types.contains(firstChildNode.getElementType())) lastRelevant = firstChildNode;
            firstChildNode = firstChildNode.getTreeNext();
        }
        return lastRelevant;
    }

    
    public static ASTNode findParent(ASTNode element, IElementType type) {
        for (ASTNode parent = element.getTreeParent(); parent != null; parent = parent.getTreeParent()) {
            if (parent.getElementType() == type) return parent;
        }
        return null;
    }

    
    public static ASTNode findParent(ASTNode element, TokenSet types) {
        for (ASTNode parent = element.getTreeParent(); parent != null; parent = parent.getTreeParent()) {
            if (types.contains(parent.getElementType())) return parent;
        }
        return null;
    }

    
    public static LeafElement findFirstLeaf(ASTNode element) {
        return (LeafElement)findFirstLeaf(element, true);
    }

    public static ASTNode findFirstLeaf(ASTNode element, boolean expandChameleons) {
        if (element instanceof LeafElement || !expandChameleons && isCollapsedChameleon(element)) {
            return element;
        }
        else {
            for (ASTNode child = element.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                ASTNode leaf = findFirstLeaf(child, expandChameleons);
                if (leaf != null) return leaf;
            }
            return null;
        }
    }

    public static boolean isLeafOrCollapsedChameleon(ASTNode node) {
        return node instanceof LeafElement || isCollapsedChameleon(node);
    }

    
    public static TreeElement findFirstLeafOrChameleon(final TreeElement element) {
        if (element == null) return null;

        final Ref<TreeElement> result = Ref.create(null);
        element.acceptTree(new RecursiveTreeElementWalkingVisitor(false) {
            @Override
            protected void visitNode(final TreeElement element) {
                if (isLeafOrCollapsedChameleon(element)) {
                    result.set(element);
                    stopWalking();
                    return;
                }
                super.visitNode(element);
            }
        });

        return result.get();
    }

    
    public static ASTNode findLastLeaf(ASTNode element) {
        return findLastLeaf(element, true);
    }

    public static ASTNode findLastLeaf(ASTNode element, boolean expandChameleons) {
        if (element instanceof LeafElement || !expandChameleons && isCollapsedChameleon(element)) {
            return element;
        }
        for (ASTNode child = element.getLastChildNode(); child != null; child = child.getTreePrev()) {
            ASTNode leaf = findLastLeaf(child);
            if (leaf != null) return leaf;
        }
        return null;
    }

    
    public static ASTNode findSibling(ASTNode start, IElementType elementType) {
        ASTNode child = start;
        while (true) {
            if (child == null) return null;
            if (child.getElementType() == elementType) return child;
            child = child.getTreeNext();
        }
    }

    
    public static ASTNode findSibling(ASTNode start, TokenSet types) {
        ASTNode child = start;
        while (true) {
            if (child == null) return null;
            if (types.contains(child.getElementType())) return child;
            child = child.getTreeNext();
        }
    }

    
    public static ASTNode findSiblingBackward(ASTNode start, IElementType elementType) {
        ASTNode child = start;
        while (true) {
            if (child == null) return null;
            if (child.getElementType() == elementType) return child;
            child = child.getTreePrev();
        }
    }


    
    public static ASTNode findSiblingBackward(ASTNode start, TokenSet types) {
        ASTNode child = start;
        while (true) {
            if (child == null) return null;
            if (types.contains(child.getElementType())) return child;
            child = child.getTreePrev();
        }
    }

    
    public static ASTNode findCommonParent(ASTNode one, ASTNode two) {
        // optimization
        if (one == two) return one;
        final Set<ASTNode> parents = new HashSet<ASTNode>(20);
        while (one != null) {
            parents.add(one);
            one = one.getTreeParent();
        }
        while (two != null) {
            if (parents.contains(two)) return two;
            two = two.getTreeParent();
        }
        return null;
    }

    public static Couple<ASTNode> findTopmostSiblingParents(ASTNode one, ASTNode two) {
        if (one == two) return Couple.of(null, null);

        LinkedList<ASTNode> oneParents = new LinkedList<ASTNode>();
        LinkedList<ASTNode> twoParents = new LinkedList<ASTNode>();
        while (one != null) {
            oneParents.add(one);
            one = one.getTreeParent();
        }
        while (two != null) {
            twoParents.add(two);
            two = two.getTreeParent();
        }

        do {
            one = oneParents.pollLast();
            two = twoParents.pollLast();
        }
        while (one == two && one != null);

        return Couple.of(one, two);
    }

    public static void clearCaches( final TreeElement tree) {
        tree.acceptTree(new RecursiveTreeElementWalkingVisitor(false) {
            @Override
            protected void visitNode(final TreeElement element) {
                element.clearCaches();
                super.visitNode(element);
            }
        });
    }

    
    public static ASTNode nextLeaf( final ASTNode node) {
        return nextLeaf((TreeElement)node, null);
    }

    public static Key<FileElement> CONTAINING_FILE_KEY_AFTER_REPARSE = Key.create("CONTAINING_FILE_KEY_AFTER_REPARSE");
    public static FileElement getFileElement(TreeElement element) {
        TreeElement parent = element;
        while (parent != null && !(parent instanceof FileElement)) {
            parent = parent.getTreeParent();
        }
        if (parent == null) {
            parent = element.getUserData(CONTAINING_FILE_KEY_AFTER_REPARSE);
        }
        return (FileElement)parent;
    }

    
    public static ASTNode prevLeaf(final ASTNode node) {
        return prevLeaf((TreeElement)node, null);
    }

    public static boolean isStrongWhitespaceHolder(IElementType type) {
        return type instanceof IStrongWhitespaceHolderElementType;
    }

    public static String getTokenText(Lexer lexer) {
        return lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
    }

    
    public static LeafElement nextLeaf( TreeElement start, CommonParentState commonParent) {
        return (LeafElement)nextLeaf(start, commonParent, null, true);
    }

    
    public static TreeElement nextLeaf( TreeElement start,
                                       CommonParentState commonParent,
                                       IElementType searchedType,
                                       boolean expandChameleons) {
        TreeElement element = start;
        while (element != null) {
            if (commonParent != null) {
                commonParent.startLeafBranchStart = element;
                initStrongWhitespaceHolder(commonParent, element, true);
            }
            TreeElement nextTree = element;
            TreeElement next = null;
            while (next == null && (nextTree = nextTree.getTreeNext()) != null) {
                if (nextTree.getElementType() == searchedType) {
                    return nextTree;
                }
                next = findFirstLeafOrType(nextTree, searchedType, commonParent, expandChameleons);
            }
            if (next != null) {
                if (commonParent != null) commonParent.nextLeafBranchStart = nextTree;
                return next;
            }
            element = element.getTreeParent();
        }
        return element;
    }

    private static void initStrongWhitespaceHolder(CommonParentState commonParent, ASTNode start, boolean slopeSide) {
        if (start instanceof CompositeElement &&
                (isStrongWhitespaceHolder(start.getElementType()) || slopeSide && start.getUserData(UNCLOSED_ELEMENT_PROPERTY) != null)) {
            commonParent.strongWhiteSpaceHolder = (CompositeElement)start;
            commonParent.isStrongElementOnRisingSlope = slopeSide;
        }
    }

    
    private static TreeElement findFirstLeafOrType( TreeElement element,
                                                   final IElementType searchedType,
                                                   final CommonParentState commonParent,
                                                   final boolean expandChameleons) {
        class MyVisitor extends RecursiveTreeElementWalkingVisitor {
            TreeElement result;

            MyVisitor(boolean doTransform) {
                super(doTransform);
            }

            @Override
            protected void visitNode(TreeElement node) {
                if (result != null) return;

                if (commonParent != null) {
                    initStrongWhitespaceHolder(commonParent, node, false);
                }
                if (!expandChameleons && isCollapsedChameleon(node) || node instanceof LeafElement || node.getElementType() == searchedType) {
                    result = node;
                    return;
                }

                super.visitNode(node);
            }
        }

        MyVisitor visitor = new MyVisitor(expandChameleons);
        element.acceptTree(visitor);
        return visitor.result;
    }

    
    public static ASTNode prevLeaf(TreeElement start,  CommonParentState commonParent) {
        while (true) {
            if (start == null) return null;
            if (commonParent != null) {
                if (commonParent.strongWhiteSpaceHolder != null && start.getUserData(UNCLOSED_ELEMENT_PROPERTY) != null) {
                    commonParent.strongWhiteSpaceHolder = (CompositeElement)start;
                }
                commonParent.nextLeafBranchStart = start;
            }
            ASTNode prevTree = start;
            ASTNode prev = null;
            while (prev == null && (prevTree = prevTree.getTreePrev()) != null) {
                prev = findLastLeaf(prevTree);
            }
            if (prev != null) {
                if (commonParent != null) commonParent.startLeafBranchStart = (TreeElement)prevTree;
                return prev;
            }
            start = start.getTreeParent();
        }
    }

    
    public static ASTNode nextLeaf( ASTNode start, boolean expandChameleons) {
        while (start != null) {
            for (ASTNode each = start.getTreeNext(); each != null; each = each.getTreeNext()) {
                ASTNode leaf = findFirstLeaf(each, expandChameleons);
                if (leaf != null) return leaf;
            }
            start = start.getTreeParent();
        }
        return null;
    }

    
    public static ASTNode prevLeaf( ASTNode start, boolean expandChameleons) {
        while (start != null) {
            for (ASTNode each = start.getTreePrev(); each != null; each = each.getTreePrev()) {
                ASTNode leaf = findLastLeaf(each, expandChameleons);
                if (leaf != null) return leaf;
            }
            start = start.getTreeParent();
        }
        return null;
    }

    
    public static ASTNode getLastChild(ASTNode element) {
        ASTNode child = element;
        while (child != null) {
            element = child;
            child = element.getLastChildNode();
        }
        return element;
    }

    public static final class CommonParentState {
        public TreeElement startLeafBranchStart = null;
        public ASTNode nextLeafBranchStart = null;
        public CompositeElement strongWhiteSpaceHolder = null;
        public boolean isStrongElementOnRisingSlope = true;
    }

    public static class StubBindingException extends RuntimeException {
        public StubBindingException(String message) {
            super(message);
        }
    }

    public static void bindStubsToTree( PsiFileImpl file,  StubTree stubTree) throws StubBindingException {
        final Iterator<StubElement<?>> stubs = stubTree.getPlainList().iterator();
        stubs.next();  // skip file root stub

        FileElement tree = file.getTreeElement();
        assert tree != null : file;

        final IStubFileElementType type = file.getElementTypeForStubBuilder();
        assert type != null;
        final StubBuilder builder = type.getBuilder();
        tree.acceptTree(new RecursiveTreeElementWalkingVisitor() {
            @Override
            protected void visitNode(TreeElement node) {
                CompositeElement parent = node.getTreeParent();
                if (parent != null && builder.skipChildProcessingWhenBuildingStubs(parent, node)) {
                    return;
                }

                IElementType type = node.getElementType();
                if (type instanceof IStubElementType && ((IStubElementType)type).shouldCreateStub(node)) {
                    final StubElement stub = stubs.hasNext() ? stubs.next() : null;
                    if (stub == null || stub.getStubType() != type) {
                        throw new StubBindingException("stub:" + stub + ", AST:" + type);
                    }

                    //noinspection unchecked
                    ((StubBase)stub).setPsi(node.getPsi());
                }

                super.visitNode(node);
            }
        });
    }
}
