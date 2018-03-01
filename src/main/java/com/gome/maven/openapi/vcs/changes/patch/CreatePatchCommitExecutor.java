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

package com.gome.maven.openapi.vcs.changes.patch;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.actions.ShowFilePathAction;
import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.components.ProjectComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.patch.BaseRevisionTextPatchEP;
import com.gome.maven.openapi.diff.impl.patch.FilePatch;
import com.gome.maven.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.DefaultJDOMExternalizer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.changes.shelf.ShelveChangesManager;
import com.gome.maven.util.WaitForProgressToShow;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class CreatePatchCommitExecutor extends LocalCommitExecutor implements ProjectComponent, JDOMExternalizable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.patch.CreatePatchCommitExecutor");

    private final Project myProject;
    private final ChangeListManager myChangeListManager;

    public String PATCH_PATH = "";

    public static CreatePatchCommitExecutor getInstance(Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetComponent(project, CreatePatchCommitExecutor.class);
    }

    public CreatePatchCommitExecutor(final Project project, final ChangeListManager changeListManager) {
        myProject = project;
        myChangeListManager = changeListManager;
    }

    
    public String getActionText() {
        return "Create Patch...";
    }

    @Override
    public String getHelpId() {
        return "reference.dialogs.vcs.patch.create";
    }

    
    public CommitSession createCommitSession() {
        return new CreatePatchCommitSession();
    }

    public void projectOpened() {
        myChangeListManager.registerCommitExecutor(this);
    }

    public void projectClosed() {
    }

    
    
    public String getComponentName() {
        return "CreatePatchCommitExecutor";
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
    }

    private class CreatePatchCommitSession implements CommitSession, CommitSessionContextAware {
        private final CreatePatchConfigurationPanel myPanel = new CreatePatchConfigurationPanel(myProject);
        private CommitContext myCommitContext;

        public CreatePatchCommitSession() {
        }

        @Override
        public void setContext(CommitContext context) {
            myCommitContext = context;
        }

        
        public JComponent getAdditionalConfigurationUI() {
            return myPanel.getPanel();
        }

        public JComponent getAdditionalConfigurationUI(final Collection<Change> changes, final String commitMessage) {
            if (PATCH_PATH.length() == 0) {
                VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
                PATCH_PATH = settings.PATCH_STORAGE_LOCATION;
                if (PATCH_PATH == null) {
                    PATCH_PATH = myProject.getBaseDir() == null ? PathManager.getHomePath() : myProject.getBaseDir().getPresentableUrl();
                }
            }
            myPanel.setFileName(ShelveChangesManager.suggestPatchName(myProject, commitMessage, new File(PATCH_PATH), null));
            myPanel.setReversePatch(false);

            boolean dvcsIsUsed = false;

            if (ProjectLevelVcsManager.getInstance(myProject).dvcsUsedInProject()) {
                for (Change change : changes) {
                    final AbstractVcs vcs = ChangesUtil.getVcsForChange(change, myProject);
                    if (vcs != null && VcsType.distributed.equals(vcs.getType())) {
                        dvcsIsUsed = true;
                        break;
                    }
                }
            }
            final List<Change> modified = new ArrayList<Change>();
            for (Change change : changes) {
                if (change.getBeforeRevision() == null || change.getAfterRevision() == null) continue;
                modified.add(change);
            }
            myPanel.setChanges(modified);
            myPanel.showTextStoreOption(dvcsIsUsed);
            return myPanel.getPanel();
        }

        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            return myPanel.isOkToExecute();
        }

        public void execute(Collection<Change> changes, String commitMessage) {
            if (! myPanel.isOkToExecute()) {
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        Messages
                                .showErrorDialog(myProject, VcsBundle.message("create.patch.error.title", myPanel.getError()), CommonBundle.getErrorTitle());
                    }
                }, ModalityState.NON_MODAL, myProject);
                return;
            }
            final String fileName = myPanel.getFileName();
            final File file = new File(fileName).getAbsoluteFile();
            if (file.exists()) {
                final int[] result = new int[1];
                WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        result[0] = Messages.showYesNoDialog(myProject, "File " + file.getName() + " (" + file.getParent() + ")" +
                                        " already exists.\nDo you want to overwrite it?",
                                CommonBundle.getWarningTitle(), Messages.getWarningIcon());
                    }
                });
                if (Messages.NO == result[0]) return;
            }
            if (file.getParentFile() == null) {
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showErrorDialog(myProject, VcsBundle.message("create.patch.error.title", "Can not write patch to specified file: " +
                                file.getPath()), CommonBundle.getErrorTitle());
                    }
                }, ModalityState.NON_MODAL, myProject);
                return;
            }
            myPanel.onOk();
            myCommitContext.putUserData(BaseRevisionTextPatchEP.ourPutBaseRevisionTextKey, myPanel.isStoreTexts());
            final List<FilePath> list = new ArrayList<FilePath>();
            for (Change change : myPanel.getIncludedChanges()) {
                list.add(ChangesUtil.getFilePath(change));
            }
            myCommitContext.putUserData(BaseRevisionTextPatchEP.ourBaseRevisionPaths, list);

            int binaryCount = 0;
            for(Change change: changes) {
                if (ChangesUtil.isBinaryChange(change)) {
                    binaryCount++;
                }
            }
            if (binaryCount == changes.size()) {
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    public void run() {
                        Messages.showInfoMessage(myProject, VcsBundle.message("create.patch.all.binary"),
                                VcsBundle.message("create.patch.commit.action.title"));
                    }
                }, null, myProject);
                return;
            }
            try {
                file.getParentFile().mkdirs();
                VcsConfiguration.getInstance(myProject).acceptLastCreatedPatchName(file.getName());
                PATCH_PATH = file.getParent();
                VcsApplicationSettings.getInstance().PATCH_STORAGE_LOCATION = PATCH_PATH;
                final boolean reversePatch = myPanel.isReversePatch();

                List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(myProject, changes, myProject.getBaseDir().getPresentableUrl(), reversePatch);
                PatchWriter.writePatches(myProject, fileName, patches, myCommitContext, myPanel.getEncoding());
                final String message;
                if (binaryCount == 0) {
                    message = VcsBundle.message("create.patch.success.confirmation", file.getPath());
                }
                else {
                    message = VcsBundle.message("create.patch.partial.success.confirmation", file.getPath(),
                            binaryCount);
                }
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    public void run() {
                        final VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
                        if (Boolean.TRUE.equals(configuration.SHOW_PATCH_IN_EXPLORER)) {
                            ShowFilePathAction.openFile(file);
                        } else if (Boolean.FALSE.equals(configuration.SHOW_PATCH_IN_EXPLORER)) {
                            return;
                        } else {
                            configuration.SHOW_PATCH_IN_EXPLORER =
                                    ShowFilePathAction.showDialog(myProject, message, VcsBundle.message("create.patch.commit.action.title"), file);
                        }
                    }
                }, null, myProject);
            } catch (ProcessCanceledException e) {
                //
            } catch (final Exception ex) {
                LOG.info(ex);
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    public void run() {
                        Messages.showErrorDialog(myProject, VcsBundle.message("create.patch.error.title", ex.getMessage()), CommonBundle.getErrorTitle());
                    }
                }, null, myProject);
            }
        }

        public void executionCanceled() {
        }

        @Override
        public String getHelpId() {
            return null;
        }
    }
}
