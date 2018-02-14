/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.ui.configuration;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.util.treeView.AbstractTreeBuilder;
import com.gome.maven.ide.util.treeView.AbstractTreeStructure;
import com.gome.maven.ide.util.treeView.NodeDescriptor;
import com.gome.maven.idea.ActionsBundle;
import com.gome.maven.openapi.actionSystem.CustomShortcutSet;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.actionSystem.ex.CustomComponentAction;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptorFactory;
import com.gome.maven.openapi.fileChooser.FileSystemTree;
import com.gome.maven.openapi.fileChooser.actions.NewFolderAction;
import com.gome.maven.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.gome.maven.openapi.fileChooser.impl.FileTreeBuilder;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.ContentEntry;
import com.gome.maven.openapi.roots.SourceFolder;
import com.gome.maven.openapi.roots.ui.configuration.actions.IconWithTextAction;
import com.gome.maven.openapi.roots.ui.configuration.actions.ToggleExcludedStateAction;
import com.gome.maven.openapi.roots.ui.configuration.actions.ToggleSourcesStateAction;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.ScrollPaneFactory;
import com.gome.maven.ui.TreeSpeedSearch;
import com.gome.maven.ui.treeStructure.Tree;
import com.gome.maven.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * Date: Oct 9, 2003
 * Time: 1:19:47 PM
 */
public class ContentEntryTreeEditor {
    private final Project myProject;
    private final List<ModuleSourceRootEditHandler<?>> myEditHandlers;
    protected final Tree myTree;
    private FileSystemTreeImpl myFileSystemTree;
    private final JPanel myTreePanel;
    private final DefaultMutableTreeNode EMPTY_TREE_ROOT = new DefaultMutableTreeNode(ProjectBundle.message("module.paths.empty.node"));
    protected final DefaultActionGroup myEditingActionsGroup;
    private ContentEntryEditor myContentEntryEditor;
    private final MyContentEntryEditorListener myContentEntryEditorListener = new MyContentEntryEditorListener();
    private final FileChooserDescriptor myDescriptor;

    public ContentEntryTreeEditor(Project project, List<ModuleSourceRootEditHandler<?>> editHandlers) {
        myProject = project;
        myEditHandlers = editHandlers;
        myTree = new Tree();
        myTree.setRootVisible(true);
        myTree.setShowsRootHandles(true);

        myEditingActionsGroup = new DefaultActionGroup();

        TreeUtil.installActions(myTree);
        new TreeSpeedSearch(myTree);

        myTreePanel = new MyPanel(new BorderLayout());
        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
        myTreePanel.add(scrollPane, BorderLayout.CENTER);

        myTreePanel.setVisible(false);
        myDescriptor = FileChooserDescriptorFactory.createMultipleFoldersDescriptor();
        myDescriptor.setShowFileSystemRoots(false);
    }

    protected void createEditingActions() {
        for (final ModuleSourceRootEditHandler<?> editor : myEditHandlers) {
            ToggleSourcesStateAction action = new ToggleSourcesStateAction(myTree, this, editor);
            CustomShortcutSet shortcutSet = editor.getMarkRootShortcutSet();
            if (shortcutSet != null) {
                action.registerCustomShortcutSet(shortcutSet, myTree);
            }
            myEditingActionsGroup.add(action);
        }

        setupExcludedAction();
    }

    protected List<ModuleSourceRootEditHandler<?>> getEditHandlers() {
        return myEditHandlers;
    }

    protected TreeCellRenderer getContentEntryCellRenderer() {
        return new ContentEntryTreeCellRenderer(this, myEditHandlers);
    }

    /**
     * @param contentEntryEditor : null means to clear the editor
     */
    public void setContentEntryEditor(final ContentEntryEditor contentEntryEditor) {
        if (myContentEntryEditor != null && myContentEntryEditor.equals(contentEntryEditor)) {
            return;
        }
        if (myFileSystemTree != null) {
            Disposer.dispose(myFileSystemTree);
            myFileSystemTree = null;
        }
        if (myContentEntryEditor != null) {
            myContentEntryEditor.removeContentEntryEditorListener(myContentEntryEditorListener);
            myContentEntryEditor = null;
        }
        if (contentEntryEditor == null) {
            ((DefaultTreeModel)myTree.getModel()).setRoot(EMPTY_TREE_ROOT);
            myTreePanel.setVisible(false);
            if (myFileSystemTree != null) {
                Disposer.dispose(myFileSystemTree);
            }
            return;
        }
        myTreePanel.setVisible(true);
        myContentEntryEditor = contentEntryEditor;
        myContentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);

        final ContentEntry entry = contentEntryEditor.getContentEntry();
        assert entry != null : contentEntryEditor;
        final VirtualFile file = entry.getFile();
        myDescriptor.setRoots(file);
        if (file == null) {
            final String path = VfsUtilCore.urlToPath(entry.getUrl());
            myDescriptor.setTitle(FileUtil.toSystemDependentName(path));
        }

        final Runnable init = new Runnable() {
            @Override
            public void run() {
                //noinspection ConstantConditions
                myFileSystemTree.updateTree();
                myFileSystemTree.select(file, null);
            }
        };

        myFileSystemTree = new FileSystemTreeImpl(myProject, myDescriptor, myTree, getContentEntryCellRenderer(), init, null) {
            @Override
            protected AbstractTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, AbstractTreeStructure treeStructure,
                                                            Comparator<NodeDescriptor> comparator, FileChooserDescriptor descriptor,
                                                            final Runnable onInitialized) {
                return new MyFileTreeBuilder(tree, treeModel, treeStructure, comparator, descriptor, onInitialized);
            }
        };
        myFileSystemTree.showHiddens(true);
        Disposer.register(myProject, myFileSystemTree);

        final NewFolderAction newFolderAction = new MyNewFolderAction();
        final DefaultActionGroup mousePopupGroup = new DefaultActionGroup();
        mousePopupGroup.add(myEditingActionsGroup);
        mousePopupGroup.addSeparator();
        mousePopupGroup.add(newFolderAction);
        myFileSystemTree.registerMouseListener(mousePopupGroup);
    }

    public ContentEntryEditor getContentEntryEditor() {
        return myContentEntryEditor;
    }

    public JComponent createComponent() {
        createEditingActions();
        return myTreePanel;
    }

    public void select(VirtualFile file) {
        if (myFileSystemTree != null) {
            myFileSystemTree.select(file, null);
        }
    }

    public void requestFocus() {
        myTree.requestFocus();
    }

    public void update() {
        if (myFileSystemTree != null) {
            myFileSystemTree.updateTree();
            final DefaultTreeModel model = (DefaultTreeModel)myTree.getModel();
            final int visibleRowCount = myTree.getVisibleRowCount();
            for (int row = 0; row < visibleRowCount; row++) {
                final TreePath pathForRow = myTree.getPathForRow(row);
                if (pathForRow != null) {
                    final TreeNode node = (TreeNode)pathForRow.getLastPathComponent();
                    if (node != null) {
                        model.nodeChanged(node);
                    }
                }
            }
        }
    }

    private static class MarkSourceToggleActionsGroup extends DefaultActionGroup {
        public MarkSourceToggleActionsGroup(String groupName, final Icon rootIcon) {
            super(groupName, true);
            getTemplatePresentation().setIcon(rootIcon);
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }
    }

    private class MyContentEntryEditorListener extends ContentEntryEditorListenerAdapter {
        @Override
        public void sourceFolderAdded( ContentEntryEditor editor, SourceFolder folder) {
            update();
        }

        @Override
        public void sourceFolderRemoved( ContentEntryEditor editor, VirtualFile file) {
            update();
        }

        @Override
        public void folderExcluded( ContentEntryEditor editor, VirtualFile file) {
            update();
        }

        @Override
        public void folderIncluded( ContentEntryEditor editor, String fileUrl) {
            update();
        }

        @Override
        public void sourceRootPropertiesChanged( ContentEntryEditor editor,  SourceFolder folder) {
            update();
        }
    }

    private static class MyNewFolderAction extends NewFolderAction implements CustomComponentAction {
        private MyNewFolderAction() {
            super(ActionsBundle.message("action.FileChooser.NewFolder.text"),
                    ActionsBundle.message("action.FileChooser.NewFolder.description"),
                    AllIcons.Actions.NewFolder);
        }

        @Override
        public JComponent createCustomComponent(Presentation presentation) {
            return IconWithTextAction.createCustomComponentImpl(this, presentation);
        }
    }

    private static class MyFileTreeBuilder extends FileTreeBuilder {
        public MyFileTreeBuilder(JTree tree,
                                 DefaultTreeModel treeModel,
                                 AbstractTreeStructure treeStructure,
                                 Comparator<NodeDescriptor> comparator,
                                 FileChooserDescriptor descriptor,
                                  Runnable onInitialized) {
            super(tree, treeModel, treeStructure, comparator, descriptor, onInitialized);
        }

        @Override
        protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
            return false; // need this in order to not show plus for empty directories
        }
    }

    private class MyPanel extends JPanel implements DataProvider {
        private MyPanel(final LayoutManager layout) {
            super(layout);
        }

        @Override
        
        public Object getData( final String dataId) {
            if (FileSystemTree.DATA_KEY.is(dataId)) {
                return myFileSystemTree;
            }
            return null;
        }
    }

    public DefaultActionGroup getEditingActionsGroup() {
        return myEditingActionsGroup;
    }

    protected void setupExcludedAction() {
        ToggleExcludedStateAction toggleExcludedAction = new ToggleExcludedStateAction(myTree, this);
        myEditingActionsGroup.add(toggleExcludedAction);
        toggleExcludedAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_MASK)), myTree);
    }

}
