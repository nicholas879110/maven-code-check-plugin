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
package com.gome.maven.openapi.diff.impl.patch.formove;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diff.impl.mergeTool.MergeVersion;
import com.gome.maven.openapi.diff.impl.patch.ApplyPatchContext;
import com.gome.maven.openapi.diff.impl.patch.ApplyPatchStatus;
import com.gome.maven.openapi.diff.impl.patch.FilePatch;
import com.gome.maven.openapi.diff.impl.patch.apply.ApplyFilePatchBase;
import com.gome.maven.openapi.diff.impl.patch.apply.ApplyTextFilePatch;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.fileTypes.ex.FileTypeChooser;
import com.gome.maven.openapi.progress.AsynchronousExecution;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.changes.patch.ApplyPatchAction;
import com.gome.maven.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.ReadonlyStatusHandler;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.RefreshQueue;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.WaitForProgressToShow;
import com.gome.maven.util.continuation.*;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

/**
 * for patches. for shelve.
 */
public class PatchApplier<BinaryType extends FilePatch> {
    private final Project myProject;
    private final VirtualFile myBaseDirectory;
    private final List<FilePatch> myPatches;
    private final CustomBinaryPatchApplier<BinaryType> myCustomForBinaries;
    private final CommitContext myCommitContext;
    private final Consumer<Collection<FilePath>> myToTargetListsMover;
    private final List<FilePatch> myRemainingPatches;
    private final PathsVerifier<BinaryType> myVerifier;
    private boolean mySystemOperation;

    private final boolean myReverseConflict;
     private final String myLeftConflictPanelTitle;
     private final String myRightConflictPanelTitle;

    public PatchApplier( Project project, final VirtualFile baseDirectory, final List<FilePatch> patches,
                         final Consumer<Collection<FilePath>> toTargetListsMover,
                        final CustomBinaryPatchApplier<BinaryType> customForBinaries, final CommitContext commitContext,
                        boolean reverseConflict,  String leftConflictPanelTitle,  String rightConflictPanelTitle) {
        myProject = project;
        myBaseDirectory = baseDirectory;
        myPatches = patches;
        myToTargetListsMover = toTargetListsMover;
        myCustomForBinaries = customForBinaries;
        myCommitContext = commitContext;
        myReverseConflict = reverseConflict;
        myLeftConflictPanelTitle = leftConflictPanelTitle;
        myRightConflictPanelTitle = rightConflictPanelTitle;
        myRemainingPatches = new ArrayList<FilePatch>();
        myVerifier = new PathsVerifier<BinaryType>(myProject, myBaseDirectory, myPatches, new PathsVerifier.BaseMapper() {
            @Override
            
            public VirtualFile getFile(FilePatch patch, String path) {
                return PathMerger.getFile(myBaseDirectory, path);
            }

            @Override
            public FilePath getPath(FilePatch patch, String path) {
                return PathMerger.getFile(new FilePathImpl(myBaseDirectory), path);
            }
        });
    }

    public PatchApplier(final Project project, final VirtualFile baseDirectory, final List<FilePatch> patches,
                        final LocalChangeList targetChangeList, final CustomBinaryPatchApplier<BinaryType> customForBinaries,
                        final CommitContext commitContext,
                        boolean reverseConflict,  String leftConflictPanelTitle,  String rightConflictPanelTitle) {
        this(project, baseDirectory, patches, createMover(project, targetChangeList), customForBinaries, commitContext,
                reverseConflict, leftConflictPanelTitle, rightConflictPanelTitle);
    }

    public void setIgnoreContentRootsCheck() {
        myVerifier.setIgnoreContentRootsCheck(true);
    }

    public PatchApplier(final Project project, final VirtualFile baseDirectory, final List<FilePatch> patches,
                        final LocalChangeList targetChangeList, final CustomBinaryPatchApplier<BinaryType> customForBinaries,
                        final CommitContext commitContext) {
        this(project, baseDirectory, patches, targetChangeList, customForBinaries, commitContext, false, null, null);
    }

    public void setIsSystemOperation(boolean systemOperation) {
        mySystemOperation = systemOperation;
    }

    
    private static Consumer<Collection<FilePath>> createMover(final Project project, final LocalChangeList targetChangeList) {
        final ChangeListManager clm = ChangeListManager.getInstance(project);
        if (targetChangeList == null || clm.getDefaultListName().equals(targetChangeList.getName())) return null;
        return new FilesMover(clm, targetChangeList);
    }

    @AsynchronousExecution
    public void execute() {
        execute(true);
    }

    @AsynchronousExecution
    public void execute(boolean showSuccessNotification) {
        final Continuation continuation = ApplicationManager.getApplication().isDispatchThread() ?
                Continuation.createFragmented(myProject, true) :
                Continuation.createForCurrentProgress(myProject, true, "Apply patch");
        final GatheringContinuationContext initContext =
                new GatheringContinuationContext();
        scheduleSelf(showSuccessNotification, initContext, false);
        continuation.run(initContext.getList());
    }

    public class ApplyPatchTask extends TaskDescriptor {
        private ApplyPatchStatus myStatus;
        private final boolean myShowNotification;
        private final boolean mySystemOperation;
        private VcsShowConfirmationOption.Value myAddconfirmationvalue;
        private VcsShowConfirmationOption.Value myDeleteconfirmationvalue;

        public ApplyPatchTask(final boolean showNotification, boolean systemOperation) {
            super("", Where.AWT);
            myShowNotification = showNotification;
            mySystemOperation = systemOperation;
        }

        @Override
        public void run(ContinuationContext context) {
            myRemainingPatches.addAll(myPatches);

            final ApplyPatchStatus patchStatus = nonWriteActionPreCheck();
            if (ApplyPatchStatus.FAILURE.equals(patchStatus)) {
                if (myShowNotification) {
                    showApplyStatus(myProject, patchStatus);
                }
                myStatus = patchStatus;
                return;
            }

            final TriggerAdditionOrDeletion trigger = new TriggerAdditionOrDeletion(myProject, mySystemOperation);
            final ApplyPatchStatus applyStatus;
            try {
                applyStatus = ApplicationManager.getApplication().runReadAction(new Computable<ApplyPatchStatus>() {
                    @Override
                    public ApplyPatchStatus compute() {
                        final Ref<ApplyPatchStatus> refStatus = new Ref<ApplyPatchStatus>(ApplyPatchStatus.FAILURE);
                        try {
                            setConfirmationToDefault();
                            CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
                                @Override
                                public void run() {
                                    if (! createFiles()) {
                                        refStatus.set(ApplyPatchStatus.FAILURE);
                                        return;
                                    }
                                    addSkippedItems(trigger);
                                    trigger.prepare();
                                    refStatus.set(executeWritable());
                                }
                            }, VcsBundle.message("patch.apply.command"), null);
                        } finally {
                            returnConfirmationBack();
                        }
                        return refStatus.get();
                    }
                });
            } finally {
                VcsFileListenerContextHelper.getInstance(myProject).clearContext();
            }
            myStatus = ApplyPatchStatus.SUCCESS.equals(patchStatus) ? applyStatus :
                    ApplyPatchStatus.and(patchStatus, applyStatus);
            // listeners finished, all 'legal' file additions/deletions with VCS are done
            trigger.processIt();
            if(myShowNotification || !ApplyPatchStatus.SUCCESS.equals(myStatus)) {
                showApplyStatus(myProject, myStatus);
            }
            refreshFiles(trigger.getAffected(), context);
        }

        private void returnConfirmationBack() {
            if (mySystemOperation) {
                final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
                final VcsShowConfirmationOption addConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, null);
                addConfirmation.setValue(myAddconfirmationvalue);
                final VcsShowConfirmationOption deleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, null);
                deleteConfirmation.setValue(myDeleteconfirmationvalue);
            }
        }

        private void setConfirmationToDefault() {
            if (mySystemOperation) {
                final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
                final VcsShowConfirmationOption addConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, null);
                myAddconfirmationvalue = addConfirmation.getValue();
                addConfirmation.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);

                final VcsShowConfirmationOption deleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, null);
                myDeleteconfirmationvalue = deleteConfirmation.getValue();
                deleteConfirmation.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
            }
        }

        public ApplyPatchStatus getStatus() {
            return myStatus;
        }
    }

    public ApplyPatchTask createApplyPart(final boolean showSuccessNotification, boolean silentAddDelete) {
        return new ApplyPatchTask(showSuccessNotification, silentAddDelete);
    }

    @AsynchronousExecution
    public void scheduleSelf(boolean showSuccessNotification,  final ContinuationContext context, boolean silentAddDelete) {
        context.next(createApplyPart(showSuccessNotification, silentAddDelete));
    }

    public static ApplyPatchStatus executePatchGroup(final Collection<PatchApplier> group, final LocalChangeList localChangeList) {
        if (group.isEmpty()) return ApplyPatchStatus.SUCCESS; //?
        final Project project = group.iterator().next().myProject;

        ApplyPatchStatus result = ApplyPatchStatus.SUCCESS;
        for (PatchApplier patchApplier : group) {
            result = ApplyPatchStatus.and(result, patchApplier.nonWriteActionPreCheck());
            if (ApplyPatchStatus.FAILURE.equals(result)) return result;
        }
        final TriggerAdditionOrDeletion trigger = new TriggerAdditionOrDeletion(project, false);
        final Ref<ApplyPatchStatus> refStatus = new Ref<ApplyPatchStatus>(null);
        try {
            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                @Override
                public void run() {
                    for (PatchApplier applier : group) {
                        if (! applier.createFiles()) {
                            refStatus.set(ApplyPatchStatus.FAILURE);
                            return;
                        }
                        applier.addSkippedItems(trigger);
                    }
                    trigger.prepare();
                    for (PatchApplier applier : group) {
                        refStatus.set(ApplyPatchStatus.and(refStatus.get(), applier.executeWritable()));
                    }
                }
            }, VcsBundle.message("patch.apply.command"), null);
        } finally {
            VcsFileListenerContextHelper.getInstance(project).clearContext();
        }
        result =  refStatus.get();
        result = result == null ? ApplyPatchStatus.FAILURE : result;

        trigger.processIt();
        final Set<FilePath> directlyAffected = new HashSet<FilePath>();
        final Set<VirtualFile> indirectlyAffected = new HashSet<VirtualFile>();
        for (PatchApplier applier : group) {
            directlyAffected.addAll(applier.getDirectlyAffected());
            indirectlyAffected.addAll(applier.getIndirectlyAffected());
        }
        directlyAffected.addAll(trigger.getAffected());
        final Consumer<Collection<FilePath>> mover = localChangeList == null ? null : createMover(project, localChangeList);
        refreshPassedFilesAndMoveToChangelist(project, null, directlyAffected, indirectlyAffected, mover, false);
        showApplyStatus(project, result);
        return result;
    }

    protected void addSkippedItems(final TriggerAdditionOrDeletion trigger) {
        trigger.addExisting(myVerifier.getToBeAdded());
        trigger.addDeleted(myVerifier.getToBeDeleted());
    }

    public ApplyPatchStatus nonWriteActionPreCheck() {
        final boolean value = myVerifier.nonWriteActionPreCheck();
        if (! value) return ApplyPatchStatus.FAILURE;

        final List<FilePatch> skipped = myVerifier.getSkipped();
        final boolean applyAll = skipped.isEmpty();
        myPatches.removeAll(skipped);
        return applyAll ? ApplyPatchStatus.SUCCESS : ((skipped.size() == myPatches.size()) ? ApplyPatchStatus.ALREADY_APPLIED : ApplyPatchStatus.PARTIAL) ;
    }

    protected ApplyPatchStatus executeWritable() {
        if (!makeWritable(myVerifier.getWritableFiles())) {
            return ApplyPatchStatus.FAILURE;
        }

        final List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches = myVerifier.getTextPatches();
        if (!fileTypesAreOk(textPatches)) {
            return ApplyPatchStatus.FAILURE;
        }

        try {
            markInternalOperation(textPatches, true);

            final ApplyPatchStatus status = actualApply(myVerifier, myCommitContext);
            return status;
        }
        finally {
            markInternalOperation(textPatches, false);
        }
    }

    private boolean createFiles() {
        final Application application = ApplicationManager.getApplication();
        return application.runWriteAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return myVerifier.execute();
            }
        });
    }

    private static void markInternalOperation(List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches, boolean set) {
        for (Pair<VirtualFile, ApplyTextFilePatch> patch : textPatches) {
            ChangesUtil.markInternalOperation(patch.getFirst(), set);
        }
    }

    protected void refreshFiles(final Collection<FilePath> additionalDirectly,  final ContinuationContext context) {
        final List<FilePath> directlyAffected = myVerifier.getDirectlyAffected();
        final List<VirtualFile> indirectlyAffected = myVerifier.getAllAffected();
        directlyAffected.addAll(additionalDirectly);

        refreshPassedFilesAndMoveToChangelist(myProject, context, directlyAffected, indirectlyAffected, myToTargetListsMover, mySystemOperation);
    }

    public List<FilePath> getDirectlyAffected() {
        return myVerifier.getDirectlyAffected();
    }

    public List<VirtualFile> getIndirectlyAffected() {
        return myVerifier.getAllAffected();
    }

    public static void refreshPassedFilesAndMoveToChangelist( final Project project, final ContinuationContext context,
                                                             final Collection<FilePath> directlyAffected, final Collection<VirtualFile> indirectlyAffected,
                                                             final Consumer<Collection<FilePath>> targetChangelistMover, final boolean systemOperation) {
        if (context != null) {
            context.suspend();
        }

        final Runnable scheduleProjectFilesReload = systemOperation ? EmptyRunnable.getInstance() : new Runnable() {
            @Override
            public void run() {
                final Runnable projectFilesReload =
                        MergeVersion.MergeDocumentVersion.prepareToReportChangedProjectFiles(project, ObjectsConvertor.fp2vf(directlyAffected));
                final TaskDescriptor projectFilesReloadTaskDescriptor = projectFilesReload == null ? null : new TaskDescriptor("", Where.AWT) {
                    @Override
                    public void run(final ContinuationContext context) {
                        projectFilesReload.run();
                    }
                };

                if (projectFilesReloadTaskDescriptor != null) {
                    if (context != null) {
                        context.last(projectFilesReloadTaskDescriptor);
                    }
                    else {
                        SwingUtilities.invokeLater(projectFilesReload);
                    }
                }
            }
        };

        final LocalFileSystem lfs = LocalFileSystem.getInstance();
        for (FilePath filePath : directlyAffected) {
            lfs.refreshAndFindFileByIoFile(filePath.getIOFile());
        }
        RefreshQueue.getInstance().refresh(false, true, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (project.isDisposed()) return;

                        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
                        if (! directlyAffected.isEmpty() && targetChangelistMover != null) {
                            changeListManager.invokeAfterUpdate(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        targetChangelistMover.consume(directlyAffected);
                                                                        scheduleProjectFilesReload.run();
                                                                        if (context != null) {
                                                                            context.ping();
                                                                        }
                                                                    }
                                                                }, InvokeAfterUpdateMode.BACKGROUND_CANCELLABLE,
                                    VcsBundle.message("change.lists.manager.move.changes.to.list"),
                                    new Consumer<VcsDirtyScopeManager>() {
                                        @Override
                                        public void consume(final VcsDirtyScopeManager vcsDirtyScopeManager) {
                                            vcsDirtyScopeManager.filePathsDirty(directlyAffected, null);
                                            vcsDirtyScopeManager.filesDirty(indirectlyAffected, null);
                                        }
                                    }, null);
                        } else {
                            final VcsDirtyScopeManager vcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
                            // will schedule update
                            vcsDirtyScopeManager.filePathsDirty(directlyAffected, null);
                            vcsDirtyScopeManager.filesDirty(indirectlyAffected, null);
                            scheduleProjectFilesReload.run();
                            if (context != null) {
                                context.ping();
                            }
                        }
                    }
                });
            }
        }, indirectlyAffected);
    }

    
    private ApplyPatchStatus actualApply(final PathsVerifier<BinaryType> verifier, final CommitContext commitContext) {
        final List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches = verifier.getTextPatches();
        final ApplyPatchContext context = new ApplyPatchContext(myBaseDirectory, 0, true, true);
        ApplyPatchStatus status = null;

        try {
            status = applyList(textPatches, context, status, commitContext);

            if (myCustomForBinaries == null) {
                status = applyList(verifier.getBinaryPatches(), context, status, commitContext);
            } else {
                final List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> binaryPatches = verifier.getBinaryPatches();
                ApplyPatchStatus patchStatus = myCustomForBinaries.apply(binaryPatches);
                final List<FilePatch> appliedPatches = myCustomForBinaries.getAppliedPatches();
                moveForCustomBinaries(binaryPatches, appliedPatches);

                status = ApplyPatchStatus.and(status, patchStatus);
                myRemainingPatches.removeAll(appliedPatches);
            }
        }
        catch (IOException e) {
            showError(myProject, e.getMessage(), true);
            return ApplyPatchStatus.FAILURE;
        }
        return status;
    }

    private void moveForCustomBinaries(final List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> patches,
                                       final List<FilePatch> appliedPatches) throws IOException {
        for (Pair<VirtualFile, ApplyFilePatchBase<BinaryType>> patch : patches) {
            if (appliedPatches.contains(patch.getSecond().getPatch())) {
                myVerifier.doMoveIfNeeded(patch.getFirst());
            }
        }
    }

    private <V extends FilePatch, T extends ApplyFilePatchBase<V>> ApplyPatchStatus applyList(final List<Pair<VirtualFile, T>> patches,
                                                                                              final ApplyPatchContext context,
                                                                                              ApplyPatchStatus status,
                                                                                              CommitContext commiContext) throws IOException {
        for (Pair<VirtualFile, T> patch : patches) {
            ApplyPatchStatus patchStatus = ApplyPatchAction.applyOnly(myProject, patch.getSecond(), context, patch.getFirst(), commiContext,
                    myReverseConflict, myLeftConflictPanelTitle, myRightConflictPanelTitle);
            myVerifier.doMoveIfNeeded(patch.getFirst());

            status = ApplyPatchStatus.and(status, patchStatus);
            if (patchStatus != ApplyPatchStatus.FAILURE) {
                myRemainingPatches.remove(patch.getSecond().getPatch());
            } else {
                // interrupt if failure
                return status;
            }
        }
        return status;
    }

    protected static void showApplyStatus( Project project, final ApplyPatchStatus status) {
        if (status == ApplyPatchStatus.ALREADY_APPLIED) {
            showError(project, VcsBundle.message("patch.apply.already.applied"), false);
        }
        else if (status == ApplyPatchStatus.PARTIAL) {
            showError(project, VcsBundle.message("patch.apply.partially.applied"), false);
        } else if (ApplyPatchStatus.SUCCESS.equals(status)) {
            final String message = VcsBundle.message("patch.apply.success.applied.text");
            VcsBalloonProblemNotifier.NOTIFICATION_GROUP.createNotification(message, MessageType.INFO).notify(project);
        }
    }

    public List<FilePatch> getRemainingPatches() {
        return myRemainingPatches;
    }

    private boolean makeWritable(final List<VirtualFile> filesToMakeWritable) {
        final VirtualFile[] fileArray = VfsUtilCore.toVirtualFileArray(filesToMakeWritable);
        final ReadonlyStatusHandler.OperationStatus readonlyStatus = ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(fileArray);
        return (! readonlyStatus.hasReadonlyFiles());
    }

    private boolean fileTypesAreOk(final List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches) {
        for (Pair<VirtualFile, ApplyTextFilePatch> textPatch : textPatches) {
            final VirtualFile file = textPatch.getFirst();
            if (! file.isDirectory()) {
                FileType fileType = file.getFileType();
                if (fileType == FileTypes.UNKNOWN) {
                    fileType = FileTypeChooser.associateFileType(file.getPresentableName());
                    if (fileType == null) {
                        showError(myProject, "Cannot apply patch. File " + file.getPresentableName() + " type not defined.", true);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void showError(final Project project, final String message, final boolean error) {
        final Application application = ApplicationManager.getApplication();
        if (application.isUnitTestMode()) {
            return;
        }
        final String title = VcsBundle.message("patch.apply.dialog.title");
        final Runnable messageShower = new Runnable() {
            @Override
            public void run() {
                if (error) {
                    Messages.showErrorDialog(project, message, title);
                }
                else {
                    Messages.showInfoMessage(project, message, title);
                }
            }
        };
        WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
            @Override
            public void run() {
                messageShower.run();
            }
        }, null, project);
    }

    private static class FilesMover implements Consumer<Collection<FilePath>> {
        private final ChangeListManager myChangeListManager;
        private final LocalChangeList myTargetChangeList;

        public FilesMover(final ChangeListManager changeListManager, final LocalChangeList targetChangeList) {
            myChangeListManager = changeListManager;
            myTargetChangeList = targetChangeList;
        }

        @Override
        public void consume(Collection<FilePath> directlyAffected) {
            List<Change> changes = new ArrayList<Change>();
            for(FilePath file: directlyAffected) {
                final Change change = myChangeListManager.getChange(file);
                if (change != null) {
                    changes.add(change);
                }
            }

            myChangeListManager.moveChangesTo(myTargetChangeList, changes.toArray(new Change[changes.size()]));
        }
    }
}
