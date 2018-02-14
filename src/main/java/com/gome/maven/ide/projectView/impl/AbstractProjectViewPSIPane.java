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

/**
 * @author cdr
 */
package com.gome.maven.ide.projectView.impl;

import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.PsiCopyPasteManager;
import com.gome.maven.ide.projectView.BaseProjectTreeBuilder;
import com.gome.maven.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.gome.maven.ide.ui.customization.CustomizationUtil;
import com.gome.maven.ide.util.treeView.AbstractTreeBuilder;
import com.gome.maven.ide.util.treeView.AbstractTreeUpdater;
import com.gome.maven.ide.util.treeView.TreeBuilderUtil;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.ScrollPaneFactory;
import com.gome.maven.ui.TreeSpeedSearch;
import com.gome.maven.util.EditSourceOnDoubleClickHandler;
import com.gome.maven.util.OpenSourceUtil;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;

public abstract class AbstractProjectViewPSIPane extends AbstractProjectViewPane {
    private JScrollPane myComponent;

    protected AbstractProjectViewPSIPane(Project project) {
        super(project);
    }

    @Override
    public JComponent createComponent() {
        if (myComponent != null) return myComponent;

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(null);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        myTree = createTree(treeModel);
        enableDnD();
        myComponent = ScrollPaneFactory.createScrollPane(myTree);
        myTreeStructure = createStructure();
        setTreeBuilder(createBuilder(treeModel));

        installComparator();
        initTree();

        return myComponent;
    }

    @Override
    public final void dispose() {
        myComponent = null;
        super.dispose();
    }

    private void initTree() {
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        UIUtil.setLineStyleAngled(myTree);
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
        myTree.expandPath(new TreePath(myTree.getModel().getRoot()));
        myTree.setSelectionPath(new TreePath(myTree.getModel().getRoot()));

        EditSourceOnDoubleClickHandler.install(myTree);

        ToolTipManager.sharedInstance().registerComponent(myTree);
        TreeUtil.installActions(myTree);

        myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                fireTreeChangeListener();
            }
        });
        myTree.getModel().addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                fireTreeChangeListener();
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                fireTreeChangeListener();
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                fireTreeChangeListener();
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                fireTreeChangeListener();
            }
        });

        new MySpeedSearch(myTree);

        myTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {

                    final DefaultMutableTreeNode selectedNode = ((ProjectViewTree)myTree).getSelectedNode();
                    if (selectedNode != null && !selectedNode.isLeaf()) {
                        return;
                    }

                    DataContext dataContext = DataManager.getInstance().getDataContext(myTree);
                    OpenSourceUtil.openSourcesFrom(dataContext, false);
                }
                else if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    if (e.isConsumed()) return;
                    PsiCopyPasteManager copyPasteManager = PsiCopyPasteManager.getInstance();
                    boolean[] isCopied = new boolean[1];
                    if (copyPasteManager.getElements(isCopied) != null && !isCopied[0]) {
                        copyPasteManager.clear();
                        e.consume();
                    }
                }
            }
        });
        CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_PROJECT_VIEW_POPUP, ActionPlaces.PROJECT_VIEW_POPUP);
    }

    
    @Override
    public final ActionCallback updateFromRoot(boolean restoreExpandedPaths) {
        final ArrayList<Object> pathsToExpand = new ArrayList<Object>();
        final ArrayList<Object> selectionPaths = new ArrayList<Object>();
        Runnable afterUpdate;
        final ActionCallback cb = new ActionCallback();
        if (restoreExpandedPaths) {
            TreeBuilderUtil.storePaths(getTreeBuilder(), (DefaultMutableTreeNode)myTree.getModel().getRoot(), pathsToExpand, selectionPaths, true);
            afterUpdate = new Runnable() {
                @Override
                public void run() {
                    if (myTree != null && getTreeBuilder() != null && !getTreeBuilder().isDisposed()) {
                        myTree.setSelectionPaths(new TreePath[0]);
                        TreeBuilderUtil.restorePaths(getTreeBuilder(), pathsToExpand, selectionPaths, true);
                    }
                    cb.setDone();
                }
            };
        }
        else {
            afterUpdate = cb.createSetDoneRunnable();
        }
        if (getTreeBuilder() != null) {
            getTreeBuilder().addSubtreeToUpdate(getTreeBuilder().getRootNode(), afterUpdate);
        }
        //myTreeBuilder.updateFromRoot();
        return cb;
    }

    @Override
    public void select(Object element, VirtualFile file, boolean requestFocus) {
        selectCB(element, file, requestFocus);
    }

    
    public ActionCallback selectCB(Object element, VirtualFile file, boolean requestFocus) {
        if (file != null) {
            return ((BaseProjectTreeBuilder)getTreeBuilder()).select(element, file, requestFocus);
        }
        return new ActionCallback.Done();
    }

    
    protected BaseProjectTreeBuilder createBuilder(DefaultTreeModel treeModel) {
        return new ProjectTreeBuilder(myProject, myTree, treeModel, null, (ProjectAbstractTreeStructureBase)myTreeStructure) {
            @Override
            protected AbstractTreeUpdater createUpdater() {
                return createTreeUpdater(this);
            }
        };
    }

    protected abstract ProjectAbstractTreeStructureBase createStructure();

    protected abstract ProjectViewTree createTree(DefaultTreeModel treeModel);

    protected abstract AbstractTreeUpdater createTreeUpdater(AbstractTreeBuilder treeBuilder);


    protected static final class MySpeedSearch extends TreeSpeedSearch {
        MySpeedSearch(JTree tree) {
            super(tree);
        }

        @Override
        protected boolean isMatchingElement(Object element, String pattern) {
            Object userObject = ((DefaultMutableTreeNode)((TreePath)element).getLastPathComponent()).getUserObject();
            if (userObject instanceof PsiDirectoryNode) {
                String str = getElementText(element);
                if (str == null) return false;
                str = str.toLowerCase();
                if (pattern.indexOf('.') >= 0) {
                    return compare(str, pattern);
                }
                StringTokenizer tokenizer = new StringTokenizer(str, ".");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (compare(token, pattern)) {
                        return true;
                    }
                }
                return false;
            }
            else {
                return super.isMatchingElement(element, pattern);
            }
        }
    }
}
