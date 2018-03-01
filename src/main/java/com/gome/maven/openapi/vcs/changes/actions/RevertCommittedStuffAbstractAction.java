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
package com.gome.maven.openapi.vcs.changes.actions;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.patch.BinaryFilePatch;
import com.gome.maven.openapi.diff.impl.patch.FilePatch;
import com.gome.maven.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.gome.maven.openapi.diff.impl.patch.formove.PatchApplier;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.changes.ui.ChangeListChooser;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.WaitForProgressToShow;
import com.gome.maven.util.containers.Convertor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class RevertCommittedStuffAbstractAction extends AnAction implements DumbAware {
    private final Convertor<AnActionEvent, Change[]> myForUpdateConvertor;
    private final Convertor<AnActionEvent, Change[]> myForPerformConvertor;
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.actions.RevertCommittedStuffAbstractAction");

    public RevertCommittedStuffAbstractAction(final Convertor<AnActionEvent, Change[]> forUpdateConvertor,
                                              final Convertor<AnActionEvent, Change[]> forPerformConvertor) {
        myForUpdateConvertor = forUpdateConvertor;
        myForPerformConvertor = forPerformConvertor;
    }

    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final VirtualFile baseDir = project.getBaseDir();
        assert baseDir != null;
        final Change[] changes = myForPerformConvertor.convert(e);
        if (changes == null || changes.length == 0) return;
        final List<Change> changesList = new ArrayList<Change>();
        Collections.addAll(changesList, changes);
        FileDocumentManager.getInstance().saveAllDocuments();

        String defaultName = null;
        final ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
        if (changeLists != null && changeLists.length > 0) {
            defaultName = VcsBundle.message("revert.changes.default.name", changeLists[0].getName());
        }

        final ChangeListChooser chooser = new ChangeListChooser(project, ChangeListManager.getInstance(project).getChangeListsCopy(), null,
                "Select Target Changelist", defaultName);
        if (!chooser.showAndGet()) {
            return;
        }

        final List<FilePatch> patches = new ArrayList<FilePatch>();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, VcsBundle.message("revert.changes.title"), true,
                BackgroundFromStartOption.getInstance()) {
            @Override
            public void run( ProgressIndicator indicator) {
                try {
                    final List<Change> preprocessed = ChangesPreprocess.preprocessChangesRemoveDeletedForDuplicateMoved(changesList);
                    patches.addAll(IdeaTextPatchBuilder.buildPatch(project, preprocessed, baseDir.getPresentableUrl(), true));
                }
                catch (final VcsException ex) {
                    WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                        @Override
                        public void run() {
                            Messages.showErrorDialog(project, "Failed to revert changes: " + ex.getMessage(), VcsBundle.message("revert.changes.title"));
                        }
                    }, null, myProject);
                    indicator.cancel();
                }
            }

            @Override
            public void onSuccess() {
                new PatchApplier<BinaryFilePatch>(project, baseDir, patches, chooser.getSelectedList(), null, null).execute();
            }
        });
    }

    public void update(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final Change[] changes = myForUpdateConvertor.convert(e);
        e.getPresentation().setEnabled(project != null && changes != null && changes.length > 0);
    }
}
