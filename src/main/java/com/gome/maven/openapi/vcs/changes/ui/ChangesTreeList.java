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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.CopyProvider;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.ide.util.treeView.TreeState;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diff.DiffBundle;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.keymap.KeymapManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangesUtil;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.*;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.ui.components.panels.NonOpaquePanel;
import com.gome.maven.ui.treeStructure.Tree;
import com.gome.maven.ui.treeStructure.actions.CollapseAllAction;
import com.gome.maven.ui.treeStructure.actions.ExpandAllAction;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.Convertor;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.tree.TreeUtil;
import com.gome.maven.util.ui.tree.WideSelectionTreeUI;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
//import org.gome.maven.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * @author max
 */
public abstract class ChangesTreeList<T> extends JPanel implements TypeSafeDataProvider {
    private final Tree myTree;
    private final JBList myList;
    private final JScrollPane myTreeScrollPane;
    private final JScrollPane myListScrollPane;
    protected final Project myProject;
    private final boolean myShowCheckboxes;
    private final boolean myHighlightProblems;
    private boolean myShowFlatten;

    private final Collection<T> myIncludedChanges;
    private Runnable myDoubleClickHandler = EmptyRunnable.getInstance();
    private boolean myAlwaysExpandList;

     private static final String TREE_CARD = "Tree";
     private static final String LIST_CARD = "List";
     private static final String ROOT = "root";
    private final CardLayout myCards;

     private final static String FLATTEN_OPTION_KEY = "ChangesBrowser.SHOW_FLATTEN";

    private final Runnable myInclusionListener;
     private ChangeNodeDecorator myChangeDecorator;
    private Runnable myGenericSelectionListener;
     private final CopyProvider myTreeCopyProvider;
     private final ChangesBrowserNodeListCopyProvider myListCopyProvider;

    public ChangesTreeList(final Project project, Collection<T> initiallyIncluded, final boolean showCheckboxes,
                           final boolean highlightProblems,  final Runnable inclusionListener,  final ChangeNodeDecorator decorator) {
        myProject = project;
        myShowCheckboxes = showCheckboxes;
        myHighlightProblems = highlightProblems;
        myInclusionListener = inclusionListener;
        myChangeDecorator = decorator;
        myIncludedChanges = new HashSet<T>(initiallyIncluded);
        myAlwaysExpandList = true;

        myCards = new CardLayout();

        setLayout(myCards);

        final int checkboxWidth = new JCheckBox().getPreferredSize().width;
        myTree = new MyTree(project, checkboxWidth);
        myTree.setHorizontalAutoScrollingEnabled(false);
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
        myTree.setOpaque(false);
        myTree.setCellRenderer(new MyTreeCellRenderer());
        new TreeSpeedSearch(myTree, new Convertor<TreePath, String>() {
            @Override
            public String convert(TreePath o) {
                ChangesBrowserNode node = (ChangesBrowserNode) o.getLastPathComponent();
                return node.getTextPresentation();
            }
        });

        myList = new JBList(new DefaultListModel());
        myList.setVisibleRowCount(10);

        add(myListScrollPane = ScrollPaneFactory.createScrollPane(myList), LIST_CARD);
        add(myTreeScrollPane = ScrollPaneFactory.createScrollPane(myTree), TREE_CARD);

        new ListSpeedSearch(myList) {
            @Override
            protected String getElementText(Object element) {
                if (element instanceof Change) {
                    return ChangesUtil.getFilePath((Change)element).getName();
                }
                return super.getElementText(element);
            }
        };

        myList.setCellRenderer(new MyListCellRenderer());

        new MyToggleSelectionAction().registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)), this);
        if (myShowCheckboxes) {
            registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    includeSelection();
                }

            }, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

            registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    excludeSelection();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        }

        registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myDoubleClickHandler.run();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        myTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode() && e.getModifiers() == 0) {
                    if (myTree.getSelectionCount() <= 1) {
                        Object lastPathComponent = myTree.getLastSelectedPathComponent();
                        if (!(lastPathComponent instanceof DefaultMutableTreeNode)) {
                            return;
                        }
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode)lastPathComponent;
                        if (!node.isLeaf()) {
                            return;
                        }
                    }
                    myDoubleClickHandler.run();
                    e.consume();
                }
            }
        });

        new ClickListener() {
            @Override
            public boolean onClick( MouseEvent e, int clickCount) {
                final int idx = myList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    final Rectangle baseRect = myList.getCellBounds(idx, idx);
                    baseRect.setSize(checkboxWidth, baseRect.height);
                    if (baseRect.contains(e.getPoint())) {
                        toggleSelection();
                        return true;
                    }
                    else if (clickCount == 2) {
                        myDoubleClickHandler.run();
                        return true;
                    }
                }
                return false;
            }
        }.installOn(myList);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                final TreePath clickPath = myTree.getUI() instanceof WideSelectionTreeUI
                        ? myTree.getClosestPathForLocation(e.getX(), e.getY())
                        : myTree.getPathForLocation(e.getX(), e.getY());
                if (clickPath == null) return false;

                myDoubleClickHandler.run();
                return true;
            }
        }.installOn(myTree);

        setShowFlatten(PropertiesComponent.getInstance(myProject).isTrueValue(FLATTEN_OPTION_KEY));

        String emptyText = StringUtil.capitalize(DiffBundle.message("diff.count.differences.status.text", 0));
        setEmptyText(emptyText);

        myTreeCopyProvider = new ChangesBrowserNodeCopyProvider(myTree);
        myListCopyProvider = new ChangesBrowserNodeListCopyProvider(myProject, myList);
    }

    public void setEmptyText( String emptyText) {
        myTree.getEmptyText().setText(emptyText);
        myList.getEmptyText().setText(emptyText);
    }

    // generic, both for tree and list
    public void addSelectionListener(final Runnable runnable) {
        myGenericSelectionListener = runnable;
        myList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                myGenericSelectionListener.run();
            }
        });
        myTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                myGenericSelectionListener.run();
            }
        });
    }

    public void setChangeDecorator( ChangeNodeDecorator changeDecorator) {
        myChangeDecorator = changeDecorator;
    }

    public void setDoubleClickHandler(final Runnable doubleClickHandler) {
        myDoubleClickHandler = doubleClickHandler;
    }

    public void installPopupHandler(ActionGroup group) {
        PopupHandler.installUnknownPopupHandler(myList, group, ActionManager.getInstance());
        PopupHandler.installUnknownPopupHandler(myTree, group, ActionManager.getInstance());
    }

    public JComponent getPreferredFocusedComponent() {
        return myShowFlatten ? myList : myTree;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }

    public boolean isShowFlatten() {
        return myShowFlatten;
    }

    public void setScrollPaneBorder(Border border) {
        myListScrollPane.setBorder(border);
        myTreeScrollPane.setBorder(border);
    }

    public void setShowFlatten(final boolean showFlatten) {
        final List<T> wasSelected = getSelectedChanges();
        myShowFlatten = showFlatten;
        myCards.show(this, myShowFlatten ? LIST_CARD : TREE_CARD);
        select(wasSelected);
        if (myList.hasFocus() || myTree.hasFocus()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    requestFocus();
                }
            });
        }
    }


    @Override
    public void requestFocus() {
        if (myShowFlatten) {
            myList.requestFocus();
        }
        else {
            myTree.requestFocus();
        }
    }

    public void setChangesToDisplay(final List<T> changes) {
        setChangesToDisplay(changes, null);
    }

    public void setChangesToDisplay(final List<T> changes,  final VirtualFile toSelect) {
        final boolean wasEmpty = myList.isEmpty();
        final List<T> sortedChanges = new ArrayList<T>(changes);
        Collections.sort(sortedChanges, new Comparator<T>() {
            @Override
            public int compare(final T o1, final T o2) {
                return TreeModelBuilder.getPathForObject(o1).getName().compareToIgnoreCase(TreeModelBuilder.getPathForObject(o2).getName());
            }
        });

        @SuppressWarnings("deprecation")
        final Set<Object> wasSelected = new THashSet<Object>(Arrays.asList(myList.getSelectedValues()));
        //noinspection unchecked
        myList.setModel(new AbstractListModel() {
            @Override
            public int getSize() {
                return sortedChanges.size();
            }

            @Override
            public Object getElementAt(int index) {
                return sortedChanges.get(index);
            }
        });

        final DefaultTreeModel model = buildTreeModel(changes, myChangeDecorator);
        TreeState state = null;
        if (! myAlwaysExpandList && ! wasEmpty) {
            state = TreeState.createOn(myTree, (DefaultMutableTreeNode) myTree.getModel().getRoot());
        }
        myTree.setModel(model);
        if (! myAlwaysExpandList && ! wasEmpty) {
            //noinspection ConstantConditions
            state.applyTo(myTree, (DefaultMutableTreeNode) myTree.getModel().getRoot());

            final TIntArrayList indices = new TIntArrayList();
            for (int i = 0; i < sortedChanges.size(); i++) {
                T t = sortedChanges.get(i);
                if (wasSelected.contains(t)) {
                    indices.add(i);
                }
            }
            myList.setSelectedIndices(indices.toNativeArray());
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (myProject.isDisposed()) return;
                TreeUtil.expandAll(myTree);

                int selectedListRow = 0;
                int selectedTreeRow = -1;

                if (myShowCheckboxes) {
                    if (myIncludedChanges.size() > 0) {
                        for (int i = 0; i < sortedChanges.size(); i++) {
                            T t = sortedChanges.get(i);
                            if (myIncludedChanges.contains(t)) {
                                selectedListRow = i;
                                break;
                            }
                        }

                        ChangesBrowserNode root = (ChangesBrowserNode)model.getRoot();
                        Enumeration enumeration = root.depthFirstEnumeration();

                        while (enumeration.hasMoreElements()) {
                            ChangesBrowserNode node = (ChangesBrowserNode)enumeration.nextElement();
                            @SuppressWarnings("unchecked")
                            final CheckboxTree.NodeState state = getNodeStatus(node);
                            if (node != root && state == CheckboxTree.NodeState.CLEAR) {
                                myTree.collapsePath(new TreePath(node.getPath()));
                            }
                        }

                        enumeration = root.depthFirstEnumeration();
                        while (enumeration.hasMoreElements()) {
                            ChangesBrowserNode node = (ChangesBrowserNode)enumeration.nextElement();
                            @SuppressWarnings("unchecked")
                            final CheckboxTree.NodeState state = getNodeStatus(node);
                            if (state == CheckboxTree.NodeState.FULL && node.isLeaf()) {
                                selectedTreeRow = myTree.getRowForPath(new TreePath(node.getPath()));
                                break;
                            }
                        }
                    }
                } else {
                    if (toSelect != null) {
                        int rowInTree = findRowContainingFile((TreeNode)model.getRoot(), toSelect);
                        if (rowInTree > -1) {
                            selectedTreeRow = rowInTree;
                        }
                        int rowInList = findRowContainingFile(myList.getModel(), toSelect);
                        if (rowInList > -1) {
                            selectedListRow = rowInList;
                        }
                    }
                }

                if (changes.size() > 0) {
                    myList.setSelectedIndex(selectedListRow);
                    myList.ensureIndexIsVisible(selectedListRow);

                    if (selectedTreeRow >= 0) {
                        myTree.setSelectionRow(selectedTreeRow);
                    }
                    TreeUtil.showRowCentered(myTree, selectedTreeRow, false);
                }
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static int findRowContainingFile( ListModel listModel,  final VirtualFile toSelect) {
        for (int i = 0; i < listModel.getSize(); i++) {
            Object item = listModel.getElementAt(i);
            if (item instanceof Change && matches((Change)item, toSelect)) {
                return i;
            }
        }
        return -1;
    }

    private int findRowContainingFile( TreeNode root,  final VirtualFile toSelect) {
        final Ref<Integer> row = Ref.create(-1);
        TreeUtil.traverse(root, new TreeUtil.Traverse() {
            @Override
            public boolean accept(Object node) {
                if (node instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
                    if (userObject instanceof Change) {
                        if (matches((Change)userObject, toSelect)) {
                            TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
                            row.set(myTree.getRowForPath(new TreePath(path)));
                        }
                    }
                }

                return row.get() == -1;
            }
        });
        return row.get();
    }

    private static boolean matches( Change change,  VirtualFile file) {
        VirtualFile virtualFile = change.getVirtualFile();
        return virtualFile != null && virtualFile.equals(file) || seemsToBeMoved(change, file);
    }

    private static boolean seemsToBeMoved(Change change, VirtualFile toSelect) {
        ContentRevision afterRevision = change.getAfterRevision();
        if (afterRevision == null) return false;
        FilePath file = afterRevision.getFile();
        return FileUtil.pathsEqual(file.getPath(), toSelect.getPath());
    }

    protected abstract DefaultTreeModel buildTreeModel(final List<T> changes, final ChangeNodeDecorator changeNodeDecorator);

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void toggleSelection() {
        boolean hasExcluded = false;
        for (T value : getSelectedChanges()) {
            if (!myIncludedChanges.contains(value)) {
                hasExcluded = true;
            }
        }

        if (hasExcluded) {
            includeSelection();
        }
        else {
            excludeSelection();
        }

        repaint();
    }

    private void includeSelection() {
        for (T change : getSelectedChanges()) {
            myIncludedChanges.add(change);
        }
        notifyInclusionListener();
        repaint();
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void excludeSelection() {
        for (T change : getSelectedChanges()) {
            myIncludedChanges.remove(change);
        }
        notifyInclusionListener();
        repaint();
    }

    public List<T> getChanges() {
        if (myShowFlatten) {
            ListModel m = myList.getModel();
            int size = m.getSize();
            List<T> result = new ArrayList<T>(size);
            for (int i = 0; i < size; i++) {
                //noinspection unchecked
                result.add((T)m.getElementAt(i));
            }
            return result;
        }
        else {
            final LinkedHashSet<T> result = new LinkedHashSet<T>();
            TreeUtil.traverseDepth((ChangesBrowserNode)myTree.getModel().getRoot(), new TreeUtil.Traverse() {
                @Override
                public boolean accept(Object node) {
                    ChangesBrowserNode changeNode = (ChangesBrowserNode)node;
                    if (changeNode.isLeaf()) {
                        //noinspection unchecked
                        result.addAll(changeNode.getAllChangesUnder());
                    }
                    return true;
                }
            });
            return new ArrayList<T>(result);
        }
    }

    public int getSelectionCount() {
        if (myShowFlatten) {
            return myList.getSelectedIndices().length;
        } else {
            return myTree.getSelectionCount();
        }
    }

    
    public List<T> getSelectedChanges() {
        if (myShowFlatten) {
            final List<T> changes = new ArrayList<T>();
            //noinspection deprecation
            for (Object anO : myList.getSelectedValues()) {
                //noinspection unchecked
                changes.add((T)anO);
            }
            return changes;
        }
        else {
            final TreePath[] paths = myTree.getSelectionPaths();
            if (paths == null) {
                return Collections.emptyList();
            }
            else {
                LinkedHashSet<T> changes = ContainerUtil.newLinkedHashSet();
                for (TreePath path : paths) {
                    //noinspection unchecked
                    changes.addAll(getSelectedObjects((ChangesBrowserNode)path.getLastPathComponent()));
                }
                return ContainerUtil.newArrayList(changes);
            }
        }
    }

    protected abstract List<T> getSelectedObjects(final ChangesBrowserNode<T> node);

    
    protected abstract T getLeadSelectedObject(final ChangesBrowserNode node);

    
    public T getHighestLeadSelection() {
        if (myShowFlatten) {
            final int index = myList.getLeadSelectionIndex();
            ListModel listModel = myList.getModel();
            if (index < 0 || index >= listModel.getSize()) return null;
            //noinspection unchecked
            return (T)listModel.getElementAt(index);
        }
        else {
            final TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return null;
            }
            //noinspection unchecked
            return getLeadSelectedObject((ChangesBrowserNode<T>)path.getLastPathComponent());
        }
    }

    
    public T getLeadSelection() {
        if (myShowFlatten) {
            final int index = myList.getLeadSelectionIndex();
            ListModel listModel = myList.getModel();
            //noinspection unchecked
            return index < 0 || index >= listModel.getSize() ? null : (T)listModel.getElementAt(index);
        }
        else {
            final TreePath path = myTree.getSelectionPath();
            //noinspection unchecked
            return path == null ? null : ContainerUtil.getFirstItem(getSelectedObjects(((ChangesBrowserNode<T>)path.getLastPathComponent())));
        }
    }

    private void notifyInclusionListener() {
        if (myInclusionListener != null) {
            myInclusionListener.run();
        }
    }

    // no listener supposed to be called
    public void setIncludedChanges(final Collection<T> changes) {
        myIncludedChanges.clear();
        myIncludedChanges.addAll(changes);
        myTree.repaint();
        myList.repaint();
    }

    public void includeChange(final T change) {
        myIncludedChanges.add(change);
        notifyInclusionListener();
        myTree.repaint();
        myList.repaint();
    }

    public void includeChanges(final Collection<T> changes) {
        myIncludedChanges.addAll(changes);
        notifyInclusionListener();
        myTree.repaint();
        myList.repaint();
    }

    public void excludeChange(final T change) {
        myIncludedChanges.remove(change);
        notifyInclusionListener();
        myTree.repaint();
        myList.repaint();
    }

    public void excludeChanges(final Collection<T> changes) {
        myIncludedChanges.removeAll(changes);
        notifyInclusionListener();
        myTree.repaint();
        myList.repaint();
    }

    public boolean isIncluded(final T change) {
        return myIncludedChanges.contains(change);
    }

    public Collection<T> getIncludedChanges() {
        return myIncludedChanges;
    }

    public void expandAll() {
        TreeUtil.expandAll(myTree);
    }

    public AnAction[] getTreeActions() {
        final ToggleShowDirectoriesAction directoriesAction = new ToggleShowDirectoriesAction();
        final ExpandAllAction expandAllAction = new ExpandAllAction(myTree) {
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setVisible(!myShowFlatten);
            }
        };
        final CollapseAllAction collapseAllAction = new CollapseAllAction(myTree) {
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setVisible(!myShowFlatten);
            }
        };
        final AnAction[] actions = new AnAction[]{directoriesAction, expandAllAction, collapseAllAction};
        directoriesAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK)),
                this);
        expandAllAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_EXPAND_ALL)),
                myTree);
        collapseAllAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_COLLAPSE_ALL)),
                myTree);
        return actions;
    }

    public void setSelectionMode(/*@JdkConstants.ListSelectionMode*/ int mode) {
        myList.setSelectionMode(mode);
        myTree.getSelectionModel().setSelectionMode(getTreeSelectionModeFromListSelectionMode(mode));
    }

//    @JdkConstants.TreeSelectionMode
    private static int getTreeSelectionModeFromListSelectionMode(/*@JdkConstants.ListSelectionMode*/ int mode) {
        switch (mode) {
            case ListSelectionModel.SINGLE_SELECTION: return TreeSelectionModel.SINGLE_TREE_SELECTION;
            case ListSelectionModel.SINGLE_INTERVAL_SELECTION: return TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
            case ListSelectionModel.MULTIPLE_INTERVAL_SELECTION: return TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
        }
        throw new IllegalArgumentException("Illegal selection mode: " + mode);
    }

    private class MyTreeCellRenderer extends JPanel implements TreeCellRenderer {
        private final ChangesBrowserNodeRenderer myTextRenderer;
        private final JCheckBox myCheckBox;


        public MyTreeCellRenderer() {
            super(new BorderLayout());
            myCheckBox = new JCheckBox();
            myTextRenderer = new ChangesBrowserNodeRenderer(myProject, false, myHighlightProblems);

            if (myShowCheckboxes) {
                add(myCheckBox, BorderLayout.WEST);
            }

            add(myTextRenderer, BorderLayout.CENTER);
            setOpaque(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {

            if (UIUtil.isUnderGTKLookAndFeel() || UIUtil.isUnderNimbusLookAndFeel()) {
                NonOpaquePanel.setTransparent(this);
                NonOpaquePanel.setTransparent(myCheckBox);
            } else {
                setBackground(null);
                myCheckBox.setBackground(null);
                myCheckBox.setOpaque(false);
            }

            myTextRenderer.setOpaque(false);
            myTextRenderer.setTransparentIconBackground(true);
            myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (myShowCheckboxes) {
                @SuppressWarnings("unchecked")
                CheckboxTree.NodeState state = getNodeStatus((ChangesBrowserNode)value);
                myCheckBox.setSelected(state != CheckboxTree.NodeState.CLEAR);
                myCheckBox.setEnabled(state != CheckboxTree.NodeState.PARTIAL);
                revalidate();

                return this;
            }
            else {
                return myTextRenderer;
            }
        }
    }


    private CheckboxTree.NodeState getNodeStatus(ChangesBrowserNode<T> node) {
        boolean hasIncluded = false;
        boolean hasExcluded = false;

        for (T change : getSelectedObjects(node)) {
            if (myIncludedChanges.contains(change)) {
                hasIncluded = true;
            }
            else {
                hasExcluded = true;
            }
        }

        if (hasIncluded && hasExcluded) return CheckboxTree.NodeState.PARTIAL;
        if (hasIncluded) return CheckboxTree.NodeState.FULL;
        return CheckboxTree.NodeState.CLEAR;
    }

    private class MyListCellRenderer extends JPanel implements ListCellRenderer {
        private final ColoredListCellRenderer myTextRenderer;
        public final JCheckBox myCheckbox;

        public MyListCellRenderer() {
            super(new BorderLayout());
            myCheckbox = new JCheckBox();
            myTextRenderer = new VirtualFileListCellRenderer(myProject) {
                @Override
                protected void putParentPath(Object value, FilePath path, FilePath self) {
                    super.putParentPath(value, path, self);
                    final boolean applyChangeDecorator = (value instanceof Change) && myChangeDecorator != null;
                    if (applyChangeDecorator) {
                        myChangeDecorator.decorate((Change) value, this, isShowFlatten());
                    }
                }

                @Override
                protected void putParentPathImpl(Object value, String parentPath, FilePath self) {
                    final boolean applyChangeDecorator = (value instanceof Change) && myChangeDecorator != null;
                    List<Pair<String,ChangeNodeDecorator.Stress>> parts = null;
                    if (applyChangeDecorator) {
                        parts = myChangeDecorator.stressPartsOfFileName((Change)value, parentPath);
                    }
                    if (parts == null) {
                        super.putParentPathImpl(value, parentPath, self);
                        return;
                    }

                    for (Pair<String, ChangeNodeDecorator.Stress> part : parts) {
                        append(part.getFirst(), part.getSecond().derive(SimpleTextAttributes.GRAYED_ATTRIBUTES));
                    }
                }

                @Override
                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean selected,
                                                              boolean hasFocus) {
                    final Component component = super.getListCellRendererComponent(list, value, index, selected, hasFocus);
                    final FileColorManager colorManager = FileColorManager.getInstance(myProject);
                    if (!selected) {
                        if (Registry.is("file.colors.in.commit.dialog") && colorManager.isEnabled() && colorManager.isEnabledForProjectView()) {
                            if (value instanceof Change) {
                                final VirtualFile file = ((Change)value).getVirtualFile();
                                if (file != null) {
                                    final Color color = colorManager.getFileColor(file);
                                    if (color != null) {
                                        component.setBackground(color);
                                    }
                                }
                            }
                        }
                    }
                    return component;
                }
            };

            myCheckbox.setBackground(null);
            setBackground(null);

            if (myShowCheckboxes) {
                add(myCheckbox, BorderLayout.WEST);
            }
            add(myTextRenderer, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            myTextRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (myShowCheckboxes) {
                //noinspection SuspiciousMethodCalls
                myCheckbox.setSelected(myIncludedChanges.contains(value));
                return this;
            }
            else {
                return myTextRenderer;
            }
        }
    }

    private class MyToggleSelectionAction extends AnAction implements DumbAware {
        @Override
        public void actionPerformed(AnActionEvent e) {
            toggleSelection();
        }
    }

    public class ToggleShowDirectoriesAction extends ToggleAction implements DumbAware {
        public ToggleShowDirectoriesAction() {
            super(VcsBundle.message("changes.action.show.directories.text"),
                    VcsBundle.message("changes.action.show.directories.description"),
                    AllIcons.Actions.GroupByPackage);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return (! myProject.isDisposed()) && !PropertiesComponent.getInstance(myProject).isTrueValue(FLATTEN_OPTION_KEY);
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            PropertiesComponent.getInstance(myProject).setValue(FLATTEN_OPTION_KEY, String.valueOf(!state));
            setShowFlatten(!state);
        }
    }

    private class SelectAllAction extends AnAction {
        private SelectAllAction() {
            super("Select All", "Select all items", AllIcons.Actions.Selectall);
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            if (myShowFlatten) {
                final int count = myList.getModel().getSize();
                if (count > 0) {
                    myList.setSelectionInterval(0, count-1);
                }
            }
            else {
                final int countTree = myTree.getRowCount();
                if (countTree > 0) {
                    myTree.setSelectionInterval(0, countTree-1);
                }
            }
        }
    }

    public void select(final List<T> changes) {
        final DefaultTreeModel treeModel = (DefaultTreeModel) myTree.getModel();
        final TreeNode root = (TreeNode) treeModel.getRoot();
        final List<TreePath> treeSelection = new ArrayList<TreePath>(changes.size());
        TreeUtil.traverse(root, new TreeUtil.Traverse() {
            @Override
            public boolean accept(Object node) {
                @SuppressWarnings("unchecked")
                final T change = (T) ((DefaultMutableTreeNode) node).getUserObject();
                if (changes.contains(change)) {
                    treeSelection.add(new TreePath(((DefaultMutableTreeNode) node).getPath()));
                }
                return true;
            }
        });
        myTree.setSelectionPaths(treeSelection.toArray(new TreePath[treeSelection.size()]));

        // list
        final ListModel model = myList.getModel();
        final int size = model.getSize();
        final List<Integer> listSelection = new ArrayList<Integer>(changes.size());
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            final T el = (T) model.getElementAt(i);
            if (changes.contains(el)) {
                listSelection.add(i);
            }
        }
        myList.setSelectedIndices(int2int(listSelection));
    }

    private static int[] int2int(List<Integer> treeSelection) {
        final int[] toPass = new int[treeSelection.size()];
        int i = 0;
        for (Integer integer : treeSelection) {
            toPass[i] = integer;
            ++ i;
        }
        return toPass;
    }

    public void enableSelection(final boolean value) {
        myTree.setEnabled(value);
    }

    public void setAlwaysExpandList(boolean alwaysExpandList) {
        myAlwaysExpandList = alwaysExpandList;
    }

    public void setPaintBusy(final boolean value) {
        myTree.setPaintBusy(value);
        myList.setPaintBusy(value);
    }

    @Override
    public void calcData(DataKey key, DataSink sink) {
        if (PlatformDataKeys.COPY_PROVIDER == key) {
            sink.put(PlatformDataKeys.COPY_PROVIDER, myShowFlatten ? myListCopyProvider : myTreeCopyProvider);
        }
    }

    private class MyTree extends Tree implements TypeSafeDataProvider {

        private final Project myProject;
        private final int myCheckboxWidth;

        public MyTree(Project project, int checkboxWidth) {
            super(ChangesBrowserNode.create(ChangesTreeList.this.myProject, ROOT));
            myProject = project;
            myCheckboxWidth = checkboxWidth;
        }

        @Override
        public boolean isFileColorsEnabled() {
            final boolean enabled = Registry.is("file.colors.in.commit.dialog")
                    && FileColorManager.getInstance(myProject).isEnabled()
                    && FileColorManager.getInstance(myProject).isEnabledForProjectView();
            final boolean opaque = isOpaque();
            if (enabled && opaque) {
                setOpaque(false);
            } else if (!enabled && !opaque) {
                setOpaque(true);
            }
            return enabled;
        }

        @Override
        public Color getFileColorFor(Object object) {
            VirtualFile file = null;
            if (object instanceof FilePathImpl) {
                file = LocalFileSystem.getInstance().findFileByPath(((FilePathImpl)object).getPath());
            } else if (object instanceof Change) {
                file = ((Change)object).getVirtualFile();
            }

            if (file != null) {
                return FileColorManager.getInstance(myProject).getFileColor(file);
            }
            return super.getFileColorFor(object);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension size = super.getPreferredScrollableViewportSize();
            size = new Dimension(size.width + 10, size.height);
            return size;
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                if (! myTree.isEnabled()) return;
                int row = myTree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    final Rectangle baseRect = myTree.getRowBounds(row);
                    baseRect.setSize(myCheckboxWidth, baseRect.height);
                    if (baseRect.contains(e.getPoint())) {
                        myTree.setSelectionRow(row);
                        toggleSelection();
                    }
                }
            }
            super.processMouseEvent(e);
        }

        @Override
        public int getToggleClickCount() {
            return -1;
        }

        @Override
        public void calcData(DataKey key, DataSink sink) {
            // just delegate to the change list
            ChangesTreeList.this.calcData(key, sink);
        }
    }

    private static class ChangesBrowserNodeListCopyProvider implements CopyProvider {

         private final Project myProject;
         private final JList myList;

        ChangesBrowserNodeListCopyProvider( Project project,  JList list) {
            myProject = project;
            myList = list;
        }

        @Override
        public void performCopy( DataContext dataContext) {
            CopyPasteManager.getInstance().setContents(new StringSelection(StringUtil.join(myList.getSelectedValues(),
                    new Function<Object, String>() {
                        @Override
                        public String fun(Object object) {
                            return ChangesBrowserNode.create(myProject, object).getTextPresentation();
                        }
                    }, "\n")));
        }

        @Override
        public boolean isCopyEnabled( DataContext dataContext) {
            return !myList.isSelectionEmpty();
        }

        @Override
        public boolean isCopyVisible( DataContext dataContext) {
            return true;
        }
    }
}
