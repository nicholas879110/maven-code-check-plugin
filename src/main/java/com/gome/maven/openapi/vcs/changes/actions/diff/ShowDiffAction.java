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
package com.gome.maven.openapi.vcs.changes.actions.diff;

import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.DiffManager;
import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.diff.util.DiffUserDataKeys;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.idea.ActionsBundle;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.changes.actions.ShowDiffUIContext;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShowDiffAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(ShowDiffAction.class);

    private static final String ourText = ActionsBundle.actionText("ChangesView.Diff");

    public ShowDiffAction() {
        super(ourText,
                ActionsBundle.actionDescription("ChangesView.Diff"),
                AllIcons.Actions.Diff);
    }

    public void update( AnActionEvent e) {
        if (ActionPlaces.MAIN_MENU.equals(e.getPlace())) {
            e.getPresentation().setEnabled(true);
            return;
        }

        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        Project project = e.getData(CommonDataKeys.PROJECT);
        e.getPresentation().setEnabled(project != null && canShowDiff(project, changes));
    }

    protected static boolean canShowDiff( Project project,  Change[] changes) {
        if (changes == null || changes.length == 0) return false;
        for (Change change : changes) {
            if (ChangeDiffRequestProducer.canCreate(project, change)) return true;
        }
        return false;
    }

    public void actionPerformed( final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (project == null || !canShowDiff(project, changes)) return;
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;

        final boolean needsConversion = checkIfThereAreFakeRevisions(project, changes);
        final List<Change> changesInList = e.getData(VcsDataKeys.CHANGES_IN_LIST_KEY);

        // this trick is essential since we are under some conditions to refresh changes;
        // but we can only rely on callback after refresh
        final Runnable performer = new Runnable() {
            public void run() {
                Change[] convertedChanges;
                if (needsConversion) {
                    convertedChanges = loadFakeRevisions(project, changes);
                }
                else {
                    convertedChanges = changes;
                }

                if (convertedChanges == null || convertedChanges.length == 0) {
                    return;
                }

                Change selectedChane = null;
                List<Change> result = null;

                if (convertedChanges.length == 1) {
                    selectedChane = convertedChanges[0];
                    ChangeList changeList = ((ChangeListManagerImpl)ChangeListManager.getInstance(project)).getIdentityChangeList(selectedChane);
                    if (changeList != null) {
                        result = changesInList != null ? changesInList : new ArrayList<Change>(changeList.getChanges());
                    }
                }
                if (result == null) result = ContainerUtil.newArrayList(convertedChanges);

                //ContainerUtil.sort(result, ChangesComparator.getInstance(false));
                int index = selectedChane == null ? 0 : Math.max(0, ContainerUtil.indexOfIdentity(result, selectedChane));

                showDiffForChange(project, result, index);
            }
        };

        if (needsConversion) {
            ChangeListManager.getInstance(project)
                    .invokeAfterUpdate(performer, InvokeAfterUpdateMode.BACKGROUND_CANCELLABLE, ourText, ModalityState.current());
        }
        else {
            performer.run();
        }
    }

    private static boolean checkIfThereAreFakeRevisions( Project project,  Change[] changes) {
        boolean needsConversion = false;
        for (Change change : changes) {
            final ContentRevision beforeRevision = change.getBeforeRevision();
            final ContentRevision afterRevision = change.getAfterRevision();
            if (beforeRevision instanceof FakeRevision) {
                VcsDirtyScopeManager.getInstance(project).fileDirty(beforeRevision.getFile());
                needsConversion = true;
            }
            if (afterRevision instanceof FakeRevision) {
                VcsDirtyScopeManager.getInstance(project).fileDirty(afterRevision.getFile());
                needsConversion = true;
            }
        }
        return needsConversion;
    }

    
    private static Change[] loadFakeRevisions( Project project,  Change[] changes) {
        List<Change> matchingChanges = new ArrayList<Change>();
        for (Change change : changes) {
            matchingChanges.addAll(ChangeListManager.getInstance(project).getChangesIn(ChangesUtil.getFilePath(change)));
        }
        return matchingChanges.toArray(new Change[matchingChanges.size()]);
    }

    //
    // Impl
    //

    public static void showDiffForChange( Project project,  Iterable<Change> changes) {
        showDiffForChange(project, changes, 0);
    }

    public static void showDiffForChange( Project project,  Iterable<Change> changes, int index) {
        showDiffForChange(project, changes, index, new ShowDiffContext());
    }

    public static void showDiffForChange( Project project,
                                          Iterable<Change> changes,
                                          Condition<Change> condition,
                                          ShowDiffContext context) {
        int index = 0;
        List<ChangeDiffRequestProducer> presentables = new ArrayList<ChangeDiffRequestProducer>();
        for (Change change : changes) {
            if (condition.value(change)) index = presentables.size();
            ChangeDiffRequestProducer presentable = ChangeDiffRequestProducer.create(project, change, context.getChangeContext(change));
            if (presentable != null) presentables.add(presentable);
        }

        showDiffForChange(project, presentables, index, context);
    }

    public static void showDiffForChange( Project project,
                                          Iterable<Change> changes,
                                         int index,
                                          ShowDiffContext context) {
        int i = 0;
        int newIndex = 0;
        List<ChangeDiffRequestProducer> presentables = new ArrayList<ChangeDiffRequestProducer>();
        for (Change change : changes) {
            if (i == index) newIndex = presentables.size();
            ChangeDiffRequestProducer presentable = ChangeDiffRequestProducer.create(project, change, context.getChangeContext(change));
            if (presentable != null) {
                presentables.add(presentable);
            }
            i++;
        }

        showDiffForChange(project, presentables, newIndex, context);
    }

    private static void showDiffForChange( Project project,
                                           List<ChangeDiffRequestProducer> presentables,
                                          int index,
                                           ShowDiffContext context) {
        if (presentables.isEmpty()) return;
        if (index < 0 || index >= presentables.size()) index = 0;

        DiffRequestChain chain = new ChangeDiffRequestChain(presentables);
        chain.setIndex(index);

        for (Map.Entry<Key, Object> entry : context.getChainContext().entrySet()) {
            chain.putUserData(entry.getKey(), entry.getValue());
        }
        chain.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, context.getActions());

        DiffManager.getInstance().showDiff(project, chain, context.getDialogHints());
    }

    //
    // Compatibility
    //

    
    public static ShowDiffContext convertContext( ShowDiffUIContext uiContext) {
        if (uiContext.getActionsFactory() != null) LOG.warn("DiffExtendUIFactory ignored");
        if (uiContext.getDiffNavigationContext() != null) LOG.warn("DiffNavigationContext ignored");

        return new ShowDiffContext(uiContext.isShowFrame() ? DiffDialogHints.FRAME : DiffDialogHints.MODAL);
    }
}
