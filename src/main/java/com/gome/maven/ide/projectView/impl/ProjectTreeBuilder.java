/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.ide.projectView.impl;

import com.gome.maven.ProjectTopics;
import com.gome.maven.ide.CopyPasteUtil;
import com.gome.maven.ide.bookmarks.Bookmark;
import com.gome.maven.ide.bookmarks.BookmarksListener;
import com.gome.maven.ide.projectView.BaseProjectTreeBuilder;
import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ProjectViewPsiTreeChangeListener;
import com.gome.maven.ide.util.treeView.AbstractTreeStructure;
import com.gome.maven.ide.util.treeView.AbstractTreeUpdater;
import com.gome.maven.ide.util.treeView.NodeDescriptor;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootAdapter;
import com.gome.maven.openapi.roots.ModuleRootEvent;
import com.gome.maven.openapi.vcs.FileStatusListener;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.problems.WolfTheProblemSolver;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.messages.MessageBusConnection;
import gnu.trove.THashSet;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;

public class ProjectTreeBuilder extends BaseProjectTreeBuilder {
    public ProjectTreeBuilder( Project project,
                               JTree tree,
                               DefaultTreeModel treeModel,
                               Comparator<NodeDescriptor> comparator,
                               ProjectAbstractTreeStructureBase treeStructure) {
        super(project, tree, treeModel, treeStructure, comparator);

        final MessageBusConnection connection = project.getMessageBus().connect(this);

        connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                queueUpdate();
            }
        });

        connection.subscribe(BookmarksListener.TOPIC, new MyBookmarksListener());

        PsiManager.getInstance(project).addPsiTreeChangeListener(createPsiTreeChangeListener(project), this);
        FileStatusManager.getInstance(project).addFileStatusListener(new MyFileStatusListener(), this);
        CopyPasteManager.getInstance().addContentChangedListener(new CopyPasteUtil.DefaultCopyPasteListener(getUpdater()), this);

        WolfTheProblemSolver.getInstance(project).addProblemListener(new MyProblemListener(), this);

        setCanYieldUpdate(true);

        initRootNode();
    }

    /**
     * Creates psi tree changes listener. This method will be invoked in constructor of ProjectTreeBuilder
     * thus builder object will be not completely initialized
     * @param project Project
     * @return Listener
     */
    protected ProjectViewPsiTreeChangeListener createPsiTreeChangeListener(final Project project) {
        return new ProjectTreeBuilderPsiListener(project);
    }

    protected class ProjectTreeBuilderPsiListener extends ProjectViewPsiTreeChangeListener {
        public ProjectTreeBuilderPsiListener(final Project project) {
            super(project);
        }

        @Override
        protected DefaultMutableTreeNode getRootNode(){
            return ProjectTreeBuilder.this.getRootNode();
        }

        @Override
        protected AbstractTreeUpdater getUpdater() {
            return ProjectTreeBuilder.this.getUpdater();
        }

        @Override
        protected boolean isFlattenPackages(){
            AbstractTreeStructure structure = getTreeStructure();
            return structure instanceof AbstractProjectTreeStructure && ((AbstractProjectTreeStructure)structure).isFlattenPackages();
        }
    }

    private final class MyBookmarksListener implements BookmarksListener {
        @Override
        public void bookmarkAdded( Bookmark b) {
            updateForFile(b.getFile());
        }

        @Override
        public void bookmarkRemoved( Bookmark b) {
            updateForFile(b.getFile());
        }

        @Override
        public void bookmarkChanged( Bookmark b) {
            updateForFile(b.getFile());
        }

        private void updateForFile( VirtualFile file) {
            PsiElement element = findPsi(file);
            if (element != null) {
                queueUpdateFrom(element, false);
            }
        }
    }

    private final class MyFileStatusListener implements FileStatusListener {
        @Override
        public void fileStatusesChanged() {
            queueUpdate(false);
        }

        @Override
        public void fileStatusChanged( VirtualFile vFile) {
            queueUpdate(false);
        }
    }

    private PsiElement findPsi( VirtualFile vFile) {
        if (!vFile.isValid()) return null;
        PsiManager psiManager = PsiManager.getInstance(myProject);
        return vFile.isDirectory() ? psiManager.findDirectory(vFile) : psiManager.findFile(vFile);
    }

    private class MyProblemListener extends WolfTheProblemSolver.ProblemListener {
        private final Alarm myUpdateProblemAlarm = new Alarm();
        private final Collection<VirtualFile> myFilesToRefresh = new THashSet<VirtualFile>();

        @Override
        public void problemsAppeared( VirtualFile file) {
            queueUpdate(file);
        }

        @Override
        public void problemsDisappeared( VirtualFile file) {
            queueUpdate(file);
        }

        private void queueUpdate( VirtualFile fileToRefresh) {
            synchronized (myFilesToRefresh) {
                if (myFilesToRefresh.add(fileToRefresh)) {
                    myUpdateProblemAlarm.cancelAllRequests();
                    myUpdateProblemAlarm.addRequest(new Runnable() {
                        @Override
                        public void run() {
                            if (!myProject.isOpen()) return;
                            Set<VirtualFile> filesToRefresh;
                            synchronized (myFilesToRefresh) {
                                filesToRefresh = new THashSet<VirtualFile>(myFilesToRefresh);
                            }
                            final DefaultMutableTreeNode rootNode = getRootNode();
                            if (rootNode != null) {
                                updateNodesContaining(filesToRefresh, rootNode);
                            }
                            synchronized (myFilesToRefresh) {
                                myFilesToRefresh.removeAll(filesToRefresh);
                            }
                        }
                    }, 200, ModalityState.NON_MODAL);
                }
            }
        }
    }

    private void updateNodesContaining( Collection<VirtualFile> filesToRefresh,  DefaultMutableTreeNode rootNode) {
        if (!(rootNode.getUserObject() instanceof ProjectViewNode)) return;
        ProjectViewNode node = (ProjectViewNode)rootNode.getUserObject();
        Collection<VirtualFile> containingFiles = null;
        for (VirtualFile virtualFile : filesToRefresh) {
            if (!virtualFile.isValid()) {
                addSubtreeToUpdate(rootNode); // file must be deleted
                return;
            }
            if (node.contains(virtualFile)) {
                if (containingFiles == null) containingFiles = new SmartList<VirtualFile>();
                containingFiles.add(virtualFile);
            }
        }
        if (containingFiles != null) {
            updateNode(rootNode);
            Enumeration children = rootNode.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
                updateNodesContaining(containingFiles, child);
            }
        }
    }
}
