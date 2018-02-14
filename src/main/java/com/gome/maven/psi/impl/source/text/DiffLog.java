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
package com.gome.maven.psi.impl.source.text;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.pom.tree.events.impl.TreeChangeEventImpl;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.DebugUtil;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.PsiManagerImpl;
import com.gome.maven.psi.impl.PsiTreeChangeEventImpl;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.tree.CompositeElement;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.impl.source.tree.TreeUtil;
import com.gome.maven.util.diff.DiffTreeChangeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: cdr
 */
public class DiffLog implements DiffTreeChangeBuilder<ASTNode,ASTNode> {
    public DiffLog() { }

    private abstract static class LogEntry {
        protected LogEntry() {
            ProgressIndicatorProvider.checkCanceled();
        }
        abstract void doActualPsiChange( PsiFile file,  ASTDiffBuilder astDiffBuilder);
    }

    private final List<LogEntry> myEntries = new ArrayList<LogEntry>();

    
    public TreeChangeEventImpl performActualPsiChange( PsiFile file) {
        final ASTDiffBuilder astDiffBuilder = new ASTDiffBuilder((PsiFileImpl) file);
        for (LogEntry entry : myEntries) {
            entry.doActualPsiChange(file, astDiffBuilder);
        }
        file.subtreeChanged();
        return astDiffBuilder.getEvent();
    }

    @Override
    public void nodeReplaced( ASTNode oldNode,  ASTNode newNode) {
        if (oldNode instanceof FileElement && newNode instanceof FileElement) {
            appendReplaceFileElement((FileElement)oldNode, (FileElement)newNode);
        }
        else {
            myEntries.add(new ReplaceEntry(oldNode, newNode));
        }
    }

    void appendReplaceElementWithEvents( CompositeElement oldRoot,  CompositeElement newRoot) {
        myEntries.add(new ReplaceElementWithEvents(oldRoot, newRoot));
    }

    void appendReplaceFileElement( FileElement oldNode,  FileElement newNode) {
        myEntries.add(new ReplaceFileElement(oldNode, newNode));
    }

    @Override
    public void nodeDeleted( ASTNode oldParent,  ASTNode oldNode) {
        myEntries.add(new DeleteEntry(oldParent, oldNode));
    }

    @Override
    public void nodeInserted( ASTNode oldParent,  ASTNode newNode, int pos) {
        myEntries.add(new InsertEntry(oldParent, newNode, pos));
    }

    private static class ReplaceEntry extends LogEntry {
        private final ASTNode myOldChild;
        private final ASTNode myNewChild;

        private ReplaceEntry( ASTNode oldNode,  ASTNode newNode) {
            myOldChild = oldNode;
            myNewChild = newNode;
            ASTNode parent = oldNode.getTreeParent();
            assert parent != null : "old:" + oldNode + " new:" + newNode;
        }

        @Override
        void doActualPsiChange( PsiFile file,  ASTDiffBuilder astDiffBuilder) {
            ASTNode oldNode = myOldChild;
            ASTNode newNode = myNewChild;
            ASTNode parent = oldNode.getTreeParent();
            assert parent != null : "old:" + oldNode + " new:" + newNode;

            final PsiElement psiParent = parent.getPsi();
            final PsiElement psiOldChild = file.isPhysical() ? oldNode.getPsi() : null;
            if (psiParent != null && psiOldChild != null) {
                final PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(file.getManager());
                event.setParent(psiParent);
                event.setFile(file);
                event.setOldChild(psiOldChild);
                PsiElement psiNewChild = getPsi(newNode, file);
                event.setNewChild(psiNewChild);
                ((PsiManagerEx)file.getManager()).beforeChildReplacement(event);
            }

            ((TreeElement)newNode).rawRemove();
            ((TreeElement)oldNode).rawReplaceWithList((TreeElement)newNode);

            astDiffBuilder.nodeReplaced(oldNode, newNode);

            ((TreeElement)newNode).clearCaches();
            if (!(newNode instanceof FileElement)) {
                ((CompositeElement)newNode.getTreeParent()).subtreeChanged();
            }

            DebugUtil.checkTreeStructure(parent);
        }
    }

    private static class DeleteEntry extends LogEntry {
         private final ASTNode myOldParent;
         private final ASTNode myOldNode;

        private DeleteEntry( ASTNode oldParent,  ASTNode oldNode) {
            myOldParent = oldParent;
            myOldNode = oldNode;
        }

        @Override
        void doActualPsiChange( PsiFile file,  ASTDiffBuilder astDiffBuilder) {
            ASTNode child = myOldNode;
            ASTNode parent = myOldParent;

            PsiElement psiParent = parent.getPsi();
            PsiElement psiChild = file.isPhysical() ? child.getPsi() : null;

            if (psiParent != null && psiChild != null) {
                PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(file.getManager());
                event.setParent(psiParent);
                event.setChild(psiChild);
                event.setFile(file);
                ((PsiManagerEx)file.getManager()).beforeChildRemoval(event);
            }

            astDiffBuilder.nodeDeleted(parent, child);

            ((TreeElement)child).rawRemove();
            ((CompositeElement)parent).subtreeChanged();

            DebugUtil.checkTreeStructure(parent);
        }
    }

    private static class InsertEntry extends LogEntry {
         private final ASTNode myOldParent;
         private final ASTNode myNewNode;
        private final int myPos;

        private InsertEntry( ASTNode oldParent,  ASTNode newNode, int pos) {
            assert oldParent instanceof CompositeElement : oldParent;
            assert pos>=0 : pos;
            assert pos<=oldParent.getChildren(null).length : pos + " "+ Arrays.toString(oldParent.getChildren(null));
            myOldParent = oldParent;
            myNewNode = newNode;
            myPos = pos;
        }

        @Override
        void doActualPsiChange( PsiFile file,  ASTDiffBuilder astDiffBuilder) {
            ASTNode anchor = null;
            ASTNode firstChildNode = myOldParent.getFirstChildNode();
            for (int i = 0; i < myPos; i++) {
                anchor = anchor == null ? firstChildNode : anchor.getTreeNext();
            }

            PsiElement psiParent = myOldParent.getPsi();
            PsiElement psiChild = getPsi(myNewNode, file);
            if (psiParent != null && psiChild != null) {
                PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(file.getManager());
                event.setParent(psiParent);
                event.setChild(psiChild);
                event.setFile(file);
                ((PsiManagerEx)file.getManager()).beforeChildAddition(event);
            }

            ((TreeElement)myNewNode).rawRemove();
            if (anchor != null) {
                ((TreeElement)anchor).rawInsertAfterMe((TreeElement)myNewNode);
            }
            else {
                if (firstChildNode != null) {
                    ((TreeElement)firstChildNode).rawInsertBeforeMe((TreeElement)myNewNode);
                }
                else {
                    ((CompositeElement)myOldParent).rawAddChildren((TreeElement)myNewNode);
                }
            }

            astDiffBuilder.nodeInserted(myOldParent, myNewNode, myPos);

            ((TreeElement)myNewNode).clearCaches();
            ((CompositeElement)myOldParent).subtreeChanged();

            DebugUtil.checkTreeStructure(myOldParent);
        }
    }

    private static PsiElement getPsi(ASTNode node, PsiFile file) {
        node.putUserData(TreeUtil.CONTAINING_FILE_KEY_AFTER_REPARSE, ((PsiFileImpl)file).getTreeElement());
        PsiElement psiChild = file.isPhysical() ? node.getPsi() : null;
        node.putUserData(TreeUtil.CONTAINING_FILE_KEY_AFTER_REPARSE, null);
        return psiChild;
    }

    private static class ReplaceFileElement extends LogEntry {
         private final FileElement myOldNode;
         private final FileElement myNewNode;

        private ReplaceFileElement( FileElement oldNode,  FileElement newNode) {
            myOldNode = oldNode;
            myNewNode = newNode;
        }

        @Override
        void doActualPsiChange( PsiFile file,  ASTDiffBuilder astDiffBuilder) {
            PsiFileImpl fileImpl = (PsiFileImpl)file;
            final int oldLength = myOldNode.getTextLength();
            PsiManagerImpl manager = (PsiManagerImpl)fileImpl.getManager();
            BlockSupportImpl.sendBeforeChildrenChangeEvent(manager, fileImpl, false);
            if (myOldNode.getFirstChildNode() != null) myOldNode.rawRemoveAllChildren();
            final ASTNode firstChildNode = myNewNode.getFirstChildNode();
            if (firstChildNode != null) myOldNode.rawAddChildren((TreeElement)firstChildNode);
            fileImpl.getTreeElement().setCharTable(myNewNode.getCharTable());
            myOldNode.subtreeChanged();
            BlockSupportImpl.sendAfterChildrenChangedEvent(manager,fileImpl, oldLength, false);
        }
    }

    private static class ReplaceElementWithEvents extends LogEntry {
         private final CompositeElement myOldRoot;
         private final CompositeElement myNewRoot;

        private ReplaceElementWithEvents( CompositeElement oldRoot,  CompositeElement newRoot) {
            myOldRoot = oldRoot;
            myNewRoot = newRoot;
        }

        @Override
        void doActualPsiChange( PsiFile file,  ASTDiffBuilder astDiffBuilder) {
            myOldRoot.replaceAllChildrenToChildrenOf(myNewRoot);
        }
    }
}
