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
package com.gome.maven.openapi.vcs.changes.actions;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.idea.ActionsBundle;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.changes.ui.ChangeListChooser;
import com.gome.maven.openapi.vcs.changes.ui.ChangesListView;
import com.gome.maven.openapi.vcs.changes.ui.ChangesViewContentManager;
import com.gome.maven.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowManager;
import gnu.trove.THashSet;

import java.util.*;

/**
 * @author max
 */
public class MoveChangesToAnotherListAction extends AnAction implements DumbAware {

    public MoveChangesToAnotherListAction() {
        super(ActionsBundle.actionText(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST),
                ActionsBundle.actionDescription(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST),
                AllIcons.Actions.MoveToAnotherChangelist);
    }

    public void update(AnActionEvent e) {
        final boolean isEnabled = isEnabled(e);
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(isEnabled);
        }
        else {
            e.getPresentation().setEnabled(isEnabled);
        }
    }

    protected boolean isEnabled(final AnActionEvent e) {
        return checkEnabled(e);
    }

    private static boolean checkEnabled(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return false;
        if (! ProjectLevelVcsManager.getInstance(project).hasActiveVcss()) return false;

        final List<VirtualFile> unversionedFiles = e.getData(ChangesListView.UNVERSIONED_FILES_DATA_KEY);
        if (unversionedFiles != null && (! unversionedFiles.isEmpty())) return true;

        final boolean hasChangedOrUnversionedFiles = SelectedFilesHelper.hasChangedOrUnversionedFiles(project, e);
        if (hasChangedOrUnversionedFiles) return true;
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (changes != null && changes.length > 0) {
            return true;
        }
        final VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        return files != null && files.length > 0;
    }

    
    private static List<Change> getChangesForSelectedFiles(final Project project, final List<VirtualFile> unversionedFiles,
                                                            final List<VirtualFile> changedFiles, final AnActionEvent e) {
        if (ProjectLevelVcsManager.getInstance(project).getAllActiveVcss().length == 0) {
            return null;
        }

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles != null) {
            List<Change> changesInFiles = new ArrayList<Change>();
            for(VirtualFile vFile: virtualFiles) {
                Change change = changeListManager.getChange(vFile);
                if (change == null) {
                    final FileStatus status = changeListManager.getStatus(vFile);
                    if (FileStatus.UNKNOWN.equals(status)) {
                        unversionedFiles.add(vFile);
                        if (changedFiles != null) changedFiles.add(vFile);
                    } else if (FileStatus.NOT_CHANGED.equals(status) && vFile.isDirectory()) {
                        addAllChangesUnderPath(changedFiles, changeListManager, changesInFiles, new FilePathImpl(vFile));
                    }
                    continue;
                }
                if (change.getAfterRevision() != null && change.getAfterRevision().getFile() != null && change.getAfterRevision().getFile().isDirectory()) {
                    final FilePath file = change.getAfterRevision().getFile();
                    addAllChangesUnderPath(changedFiles, changeListManager, changesInFiles, file);
                } else {
                    changesInFiles.add(change);
                    if (changedFiles != null) {
                        changedFiles.add(vFile);
                    }
                }
            }
            return changesInFiles;
        }
        return Collections.emptyList();
    }

    private static void addAllChangesUnderPath(List<VirtualFile> changedFiles,
                                               ChangeListManager changeListManager,
                                               List<Change> changesInFiles, FilePath file) {
        final Collection<Change> in = changeListManager.getChangesIn(file);
        changesInFiles.addAll(in);
        if (changedFiles != null) {
            for (Change innerChange : in) {
                final FilePath path = ChangesUtil.getAfterPath(innerChange);
                if (path != null && path.getVirtualFile() != null) {
                    changedFiles.add(path.getVirtualFile());
                }
            }
        }
    }

    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;
        if (! ProjectLevelVcsManager.getInstance(project).hasActiveVcss()) return;
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        List<VirtualFile> unversionedFiles = e.getData(ChangesListView.UNVERSIONED_FILES_DATA_KEY);

        final List<VirtualFile> changedFiles = new ArrayList<VirtualFile>();
        boolean activateChangesView = false;
        unversionedFiles = new ArrayList<VirtualFile>();
        final List<Change> changesList = new ArrayList<Change>();
        if (changes != null) {
            changesList.addAll(Arrays.asList(changes));
        } else {
            changes = new Change[0];
        }
        changesList.addAll(getChangesForSelectedFiles(project, unversionedFiles, changedFiles, e));
        activateChangesView = true;

        if (changesList.isEmpty() && unversionedFiles.isEmpty()) {
            VcsBalloonProblemNotifier.showOverChangesView(project, "Nothing is selected that can be moved", MessageType.INFO);
            return;
        }

        if (!askAndMove(project, changesList, unversionedFiles)) return;
        if (activateChangesView) {
            ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID);
            if (!window.isVisible()) {
                window.activate(new Runnable() {
                    public void run() {
                        if (changedFiles.size() > 0) {
                            ChangesViewManager.getInstance(project).selectFile(changedFiles.get(0));
                        }
                    }
                });
            }
        }
    }

    public static boolean askAndMove(final Project project, final Collection<Change> changes, final List<VirtualFile> unversionedFiles) {
        if (changes.isEmpty() && unversionedFiles.isEmpty()) return false;
        final ChangeListManagerImpl listManager = ChangeListManagerImpl.getInstanceImpl(project);
        final List<LocalChangeList> lists = listManager.getChangeLists();
        final List<LocalChangeList> preferredLists = getPreferredLists(lists, changes, true);
        ChangeListChooser chooser = new ChangeListChooser(project, preferredLists, guessPreferredList(preferredLists),
                ActionsBundle.message("action.ChangesView.Move.text"), null);
        chooser.show();
        LocalChangeList resultList = chooser.getSelectedList();
        if (resultList != null) {
            listManager.moveChangesTo(resultList, changes.toArray(new Change[changes.size()]));
            if ((unversionedFiles != null) && (! unversionedFiles.isEmpty())) {
                listManager.addUnversionedFiles(resultList, unversionedFiles);
            }
            return true;
        }
        return false;
    }

    
    private static ChangeList guessPreferredList(final List<LocalChangeList> preferredLists) {
        for (ChangeList preferredList : preferredLists) {
            if (preferredList.getChanges().isEmpty()) {
                return preferredList;
            }
        }

        if (preferredLists.size() > 0) {
            return preferredLists.get(0);
        }

        return null;
    }

    private static List<LocalChangeList> getPreferredLists(final List<LocalChangeList> lists,
                                                           final Collection<Change> changes,
                                                           final boolean includeDefaultIfEmpty) {
        List<LocalChangeList> preferredLists = new ArrayList<LocalChangeList>(lists);
        Set<Change> changesAsSet = new THashSet<Change>(changes);
        for (LocalChangeList list : lists) {
            for (Change change : list.getChanges()) {
                if (changesAsSet.contains(change)) {
                    preferredLists.remove(list);
                    break;
                }
            }
        }

        if (preferredLists.isEmpty() && includeDefaultIfEmpty) {
            for (LocalChangeList list : lists) {
                if (list.isDefault()) {
                    preferredLists.add(list);
                }
            }
        }

        return preferredLists;
    }
}
