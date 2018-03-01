/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.ex.CheckboxAction;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.util.DiffUserDataKeysEx;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.changes.actions.diff.ShowDiffAction;
import com.gome.maven.openapi.vcs.changes.actions.diff.ShowDiffContext;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.containers.ContainerUtil;
//import org.gome.maven.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * @author max
 */
public class ChangesBrowser extends JPanel implements TypeSafeDataProvider {
    private static final Logger LOG = Logger.getInstance(ChangesBrowser.class);
    // for backgroundable rollback to mark
    private boolean myDataIsDirty;
    protected final ChangesTreeList<Change> myViewer;
    protected ChangeList mySelectedChangeList;
    protected Collection<Change> myChangesToDisplay;
    protected final Project myProject;
    private final boolean myCapableOfExcludingChanges;
    protected final JPanel myHeaderPanel;
    private JComponent myBottomPanel;
    private DefaultActionGroup myToolBarGroup;
    private String myToggleActionTitle = VcsBundle.message("commit.dialog.include.action.name");

    private List<AnAction> myAdditionalDiffActions;
    private JComponent myDiffBottomComponent;

    public static DataKey<ChangesBrowser> DATA_KEY = DataKey.create("com.gome.maven.openapi.vcs.changes.ui.ChangesBrowser");
    private ShowDiffAction myDiffAction;
    private final VirtualFile myToSelect;

    public void setChangesToDisplay(final List<Change> changes) {
        myChangesToDisplay = changes;
        myViewer.setChangesToDisplay(changes);
    }

    public void setDecorator(final ChangeNodeDecorator decorator) {
        myViewer.setChangeDecorator(decorator);
    }

    public ChangesBrowser(final Project project, List<? extends ChangeList> changeLists, final List<Change> changes,
                          ChangeList initialListSelection, final boolean capableOfExcludingChanges, final boolean highlightProblems,
                           final Runnable inclusionListener, final MyUseCase useCase,  VirtualFile toSelect) {
        super(new BorderLayout());
        setFocusable(false);

        myDataIsDirty = false;
        myProject = project;
        myCapableOfExcludingChanges = capableOfExcludingChanges;
        myToSelect = toSelect;

        final ChangeNodeDecorator decorator = MyUseCase.LOCAL_CHANGES.equals(useCase) ?
                RemoteRevisionsCache.getInstance(myProject).getChangesNodeDecorator() : null;

        myViewer = new ChangesTreeList<Change>(myProject, changes, capableOfExcludingChanges, highlightProblems, inclusionListener, decorator) {
            protected DefaultTreeModel buildTreeModel(final List<Change> changes, ChangeNodeDecorator changeNodeDecorator) {
                TreeModelBuilder builder = new TreeModelBuilder(myProject, false);
                return builder.buildModel(changes, changeNodeDecorator);
            }

            protected List<Change> getSelectedObjects(final ChangesBrowserNode<Change> node) {
                return node.getAllChangesUnder();
            }

            
            protected Change getLeadSelectedObject(final ChangesBrowserNode node) {
                final Object o = node.getUserObject();
                if (o instanceof Change) {
                    return (Change) o;
                }
                return null;
            }
        };

        myViewer.setDoubleClickHandler(getDoubleClickHandler());

        setInitialSelection(changeLists, changes, initialListSelection);
        rebuildList();

        add(myViewer, BorderLayout.CENTER);

        myHeaderPanel = new JPanel(new BorderLayout());
        myHeaderPanel.add(createToolbar(), BorderLayout.CENTER);
        add(myHeaderPanel, BorderLayout.NORTH);

        myBottomPanel = new JPanel(new BorderLayout());
        add(myBottomPanel, BorderLayout.SOUTH);

        myViewer.installPopupHandler(myToolBarGroup);
    }

    
    protected Runnable getDoubleClickHandler() {
        return new Runnable() {
            public void run() {
                showDiff();
            }
        };
    }

    protected void setInitialSelection(final List<? extends ChangeList> changeLists, final List<Change> changes, final ChangeList initialListSelection) {
        mySelectedChangeList = initialListSelection;
    }

    public void dispose() {
    }

    public void addToolbarAction(AnAction action) {
        myToolBarGroup.add(action);
    }

    public void addToolbarActions(ActionGroup group) {
        myToolBarGroup.addSeparator();
        myToolBarGroup.add(group);
    }

    public JComponent getDiffBottomComponent() {
        return myDiffBottomComponent;
    }

    public void setDiffBottomComponent(JComponent diffBottomComponent) {
        myDiffBottomComponent = diffBottomComponent;
    }

    public List<AnAction> getAdditionalDiffActions() {
        return myAdditionalDiffActions;
    }

    public void setAdditionalDiffActions(List<AnAction> additionalDiffActions) {
        myAdditionalDiffActions = additionalDiffActions;
    }

    public void setToggleActionTitle(final String toggleActionTitle) {
        myToggleActionTitle = toggleActionTitle;
    }

    public JPanel getHeaderPanel() {
        return myHeaderPanel;
    }

    public ChangesTreeList<Change> getViewer() {
        return myViewer;
    }

    public void calcData(DataKey key, DataSink sink) {
        if (key == VcsDataKeys.CHANGES) {
            List<Change> list = myViewer.getSelectedChanges();
            if (list.isEmpty()) list = myViewer.getChanges();
            sink.put(VcsDataKeys.CHANGES, list.toArray(new Change [list.size()]));
        }
        else if (key == VcsDataKeys.CHANGE_LISTS) {
            sink.put(VcsDataKeys.CHANGE_LISTS, getSelectedChangeLists());
        }
        else if (key == VcsDataKeys.CHANGE_LEAD_SELECTION) {
            final Change highestSelection = myViewer.getHighestLeadSelection();
            sink.put(VcsDataKeys.CHANGE_LEAD_SELECTION, (highestSelection == null) ? new Change[]{} : new Change[] {highestSelection});
        }    else if (key == CommonDataKeys.VIRTUAL_FILE_ARRAY) {
            sink.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, getSelectedFiles());
        }
        else if (key == CommonDataKeys.NAVIGATABLE_ARRAY) {
            sink.put(CommonDataKeys.NAVIGATABLE_ARRAY, ChangesUtil.getNavigatableArray(myProject, getSelectedFiles()));
        } else if (VcsDataKeys.IO_FILE_ARRAY.equals(key)) {
            sink.put(VcsDataKeys.IO_FILE_ARRAY, getSelectedIoFiles());
        }
        else if (key == DATA_KEY) {
            sink.put(DATA_KEY, this);
        } else if (VcsDataKeys.SELECTED_CHANGES_IN_DETAILS.equals(key)) {
            final List<Change> selectedChanges = getSelectedChanges();
            sink.put(VcsDataKeys.SELECTED_CHANGES_IN_DETAILS, selectedChanges.toArray(new Change[selectedChanges.size()]));
        }
    }

    public void select(List<Change> changes) {
        myViewer.select(changes);
    }

    public JComponent getBottomPanel() {
        return myBottomPanel;
    }

    private class ToggleChangeAction extends CheckboxAction {
        public ToggleChangeAction() {
            super(myToggleActionTitle);
        }

        public boolean isSelected(AnActionEvent e) {
            Change change = e.getData(VcsDataKeys.CURRENT_CHANGE);
            if (change == null) return false;

            return myViewer.isIncluded(change);
        }

        public void setSelected(AnActionEvent e, boolean state) {
            Change change = e.getData(VcsDataKeys.CURRENT_CHANGE);
            if (change == null) return;

            if (state) {
                myViewer.includeChange(change);
            }
            else {
                myViewer.excludeChange(change);
            }
        }
    }

    protected void showDiffForChanges(Change[] changesArray, final int indexInSelection) {
        final ShowDiffContext context = new ShowDiffContext(isInFrame() ? DiffDialogHints.FRAME : DiffDialogHints.MODAL);

        if (myAdditionalDiffActions == null) {
            context.addActions(createDiffActions());
        } else {
            context.addActions(myAdditionalDiffActions);
        }

        if (myDiffBottomComponent != null) {
            context.putChainContext(DiffUserDataKeysEx.BOTTOM_PANEL, myDiffBottomComponent);
        }

        updateDiffContext(context);

        ShowDiffAction.showDiffForChange(myProject, Arrays.asList(changesArray), indexInSelection, context);
    }

    protected void updateDiffContext( ShowDiffContext context) {
    }

    private void showDiff() {
        final Change leadSelection = myViewer.getLeadSelection();
        List<Change> changes = myViewer.getSelectedChanges();

        if (changes.size() < 2) {
            changes = myViewer.getChanges();
        }

        Change[] changesArray = changes.toArray(new Change[changes.size()]);

        if (leadSelection != null) {
            int indexInSelection = changes.indexOf(leadSelection);
            if (indexInSelection == -1) {
                showDiffForChanges(new Change[]{leadSelection}, 0);
            }
            else {
                showDiffForChanges(changesArray, indexInSelection);
            }
        }
        else {
            showDiffForChanges(changesArray, 0);
        }

        afterDiffRefresh();
    }

    protected void afterDiffRefresh() {
    }

    private static boolean isInFrame() {
        return ModalityState.current().equals(ModalityState.NON_MODAL);
    }

    protected List<AnAction> createDiffActions() {
        List<AnAction> actions = new ArrayList<AnAction>();
        if (myCapableOfExcludingChanges) {
            actions.add(new ToggleChangeAction());
        }
        return actions;
    }

    public void rebuildList() {
        myViewer.setChangesToDisplay(getCurrentDisplayedChanges(), myToSelect);
    }

    public void setAlwayExpandList(final boolean value) {
        myViewer.setAlwaysExpandList(value);
    }

    private JComponent createToolbar() {
        DefaultActionGroup toolbarGroups = new DefaultActionGroup();
        myToolBarGroup = new DefaultActionGroup();
        toolbarGroups.add(myToolBarGroup);
        buildToolBar(myToolBarGroup);

        toolbarGroups.addSeparator();
        DefaultActionGroup treeActionsGroup = new DefaultActionGroup();
        toolbarGroups.add(treeActionsGroup);
        for(AnAction action: myViewer.getTreeActions()) {
            treeActionsGroup.add(action);
        }

        return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarGroups, true).getComponent();
    }

    protected void buildToolBar(final DefaultActionGroup toolBarGroup) {
        myDiffAction = new ShowDiffAction() {
            public void update(AnActionEvent e) {
                Change[] changes = e.getData(VcsDataKeys.CHANGES);
                e.getPresentation().setEnabled(canShowDiff(myProject, changes));
            }

            public void actionPerformed(AnActionEvent e) {
                showDiff();                     // todo here
            }
        };
        myDiffAction.registerCustomShortcutSet(CommonShortcuts.getDiff(), myViewer);
        toolBarGroup.add(myDiffAction);
    }

    public List<Change> getCurrentDisplayedChanges() {
        final List<Change> list;
        if (myChangesToDisplay != null) {
            list = new ArrayList<Change>(myChangesToDisplay);
        }
        else if (mySelectedChangeList != null) {
            list = new ArrayList<Change>(mySelectedChangeList.getChanges());
        }
        else {
            list = Collections.emptyList();
        }
        return sortChanges(list);
    }

    protected List<Change> sortChanges(final List<Change> list) {
        List<Change> sortedList;
        try {
            sortedList = ContainerUtil.sorted(list, ChangesComparator.getInstance(myViewer.isShowFlatten()));
        }
        catch (IllegalArgumentException e) {
            sortedList = ContainerUtil.newArrayList(list);
            LOG.error("Couldn't sort these changes: " + list, e);
        }
        return sortedList;
    }

    public ChangeList getSelectedChangeList() {
        return mySelectedChangeList;
    }

    public JComponent getPreferredFocusedComponent() {
        return myViewer.getPreferredFocusedComponent();
    }

    private ChangeList[] getSelectedChangeLists() {
        if (mySelectedChangeList != null) {
            return new ChangeList[]{mySelectedChangeList};
        }
        return null;
    }

    private File[] getSelectedIoFiles() {
        final List<Change> changes = myViewer.getSelectedChanges();
        final List<File> files = new ArrayList<File>();
        for (Change change : changes) {
            final ContentRevision afterRevision = change.getAfterRevision();
            if (afterRevision != null) {
                final FilePath file = afterRevision.getFile();
                final File ioFile = file.getIOFile();
                files.add(ioFile);
            }
        }
        return files.toArray(new File[files.size()]);
    }

    public List<Change> getSelectedChanges() {
        return myViewer.getSelectedChanges();
    }

    private VirtualFile[] getSelectedFiles() {
        final List<Change> changes = myViewer.getSelectedChanges();
        return ChangesUtil.getFilesFromChanges(changes);
    }

    public ShowDiffAction getDiffAction() {
        return myDiffAction;
    }

    public enum MyUseCase {
        LOCAL_CHANGES,
        COMMITTED_CHANGES
    }

    public boolean isDataIsDirty() {
        return myDataIsDirty;
    }

    public void setDataIsDirty(boolean dataIsDirty) {
        myDataIsDirty = dataIsDirty;
    }

    public void setSelectionMode(/*@JdkConstants.ListSelectionMode*/ int mode) {
        myViewer.setSelectionMode(mode);
    }
}
