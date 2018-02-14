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
package com.gome.maven.openapi.vcs;

import com.gome.maven.ide.errorTreeView.HotfixData;
import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.annotate.AnnotationProvider;
import com.gome.maven.openapi.vcs.annotate.FileAnnotation;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.CommitResultHandler;
import com.gome.maven.openapi.vcs.changes.LocalChangeList;
import com.gome.maven.openapi.vcs.history.VcsFileRevision;
import com.gome.maven.openapi.vcs.history.VcsHistoryProvider;
import com.gome.maven.openapi.vcs.merge.MergeDialogCustomizer;
import com.gome.maven.openapi.vcs.merge.MergeProvider;
import com.gome.maven.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.gome.maven.openapi.vcs.versionBrowser.CommittedChangeList;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Component which provides means to invoke different VCS-related services.
 */
public abstract class AbstractVcsHelper {

    protected final Project myProject;

    protected AbstractVcsHelper( Project project) {
        myProject = project;
    }

    
    public static AbstractVcsHelper getInstance(Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetService(project, AbstractVcsHelper.class);
    }

    public abstract void showErrors(List<VcsException> abstractVcsExceptions,  String tabDisplayName);

    public abstract void showErrors(Map<HotfixData, List<VcsException>> exceptionGroups,  String tabDisplayName);

    /**
     * Runs the runnable inside the vcs transaction (if needed), collects all exceptions, commits/rollbacks transaction
     * and returns all exceptions together.
     */
    public abstract List<VcsException> runTransactionRunnable(AbstractVcs vcs, TransactionRunnable runnable, Object vcsParameters);

    public void showError(final VcsException e, final String tabDisplayName) {
        showErrors(Arrays.asList(e), tabDisplayName);
    }

    public abstract void showAnnotation(FileAnnotation annotation, VirtualFile file, AbstractVcs vcs);

    public abstract void showAnnotation(FileAnnotation annotation, VirtualFile file, AbstractVcs vcs, int line);

    public abstract void showDifferences(final VcsFileRevision cvsVersionOn, final VcsFileRevision cvsVersionOn1, final File file);

    public abstract void showChangesListBrowser(CommittedChangeList changelist,  String title);

    public void showChangesListBrowser(CommittedChangeList changelist,  VirtualFile toSelect,  String title) {
        showChangesListBrowser(changelist, title);
    }

    public abstract void showChangesBrowser(List<CommittedChangeList> changelists);

    public abstract void showChangesBrowser(List<CommittedChangeList> changelists,  String title);

    public abstract void showChangesBrowser(CommittedChangesProvider provider,
                                            final RepositoryLocation location,
                                             String title,
                                             final Component parent);

    public abstract void showWhatDiffersBrowser( Component parent, Collection<Change> changes,  String title);

    
    public abstract <T extends CommittedChangeList, U extends ChangeBrowserSettings> T chooseCommittedChangeList( CommittedChangesProvider<T, U> provider,
                                                                                                                 RepositoryLocation location);

    public abstract void openCommittedChangesTab(AbstractVcs vcs,
                                                 VirtualFile root,
                                                 ChangeBrowserSettings settings,
                                                 int maxCount,
                                                 final String title);

    public abstract void openCommittedChangesTab(CommittedChangesProvider provider,
                                                 RepositoryLocation location,
                                                 ChangeBrowserSettings settings,
                                                 int maxCount,
                                                 final String title);

    /**
     * Shows the multiple file merge dialog for resolving conflicts in the specified set of virtual files.
     * Assumes all files are under the same VCS.
     *
     * @param files the files to show in the merge dialog.
     * @param provider MergeProvider to be used for merging.
     * @param mergeDialogCustomizer custom container of titles, descriptions and messages for the merge dialog.
     * @return changed files for which the merge was actually performed.
     */
    public abstract  List<VirtualFile> showMergeDialog(List<VirtualFile> files, MergeProvider provider,  MergeDialogCustomizer mergeDialogCustomizer);

    /**
     * {@link #showMergeDialog(java.util.List, com.gome.maven.openapi.vcs.merge.MergeProvider)} without description.
     */
    
    public final List<VirtualFile> showMergeDialog(List<VirtualFile> files, MergeProvider provider) {
        return showMergeDialog(files, provider, new MergeDialogCustomizer());
    }

    /**
     * {@link #showMergeDialog(java.util.List, com.gome.maven.openapi.vcs.merge.MergeProvider)} without description and with default merge provider
     * for the current VCS.
     */
    
    public final List<VirtualFile> showMergeDialog(List<VirtualFile> files) {
        if (files.isEmpty()) return Collections.emptyList();
        MergeProvider provider = null;
        for (VirtualFile virtualFile : files) {
            final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(virtualFile);
            if (vcs != null) {
                provider = vcs.getMergeProvider();
                if (provider != null) break;
            }
        }
        if (provider == null) return Collections.emptyList();
        return showMergeDialog(files, provider);
    }

    public abstract void showFileHistory(VcsHistoryProvider vcsHistoryProvider, FilePath path, final AbstractVcs vcs,
                                         final String repositoryPath);

    public abstract void showFileHistory(VcsHistoryProvider vcsHistoryProvider, AnnotationProvider annotationProvider, FilePath path,
                                         final String repositoryPath, final AbstractVcs vcs);

    /**
     * Shows the "Rollback Changes" dialog with the specified list of changes.
     *
     * @param changes the changes to show in the dialog.
     */
    public abstract void showRollbackChangesDialog(List<Change> changes);

    
    public abstract Collection<VirtualFile> selectFilesToProcess(List<VirtualFile> files,
                                                                 final String title,
                                                                  final String prompt,
                                                                 final String singleFileTitle,
                                                                 final String singleFilePromptTemplate,
                                                                 final VcsShowConfirmationOption confirmationOption);

    
    public abstract Collection<FilePath> selectFilePathsToProcess(List<FilePath> files,
                                                                  final String title,
                                                                   final String prompt,
                                                                  final String singleFileTitle,
                                                                  final String singleFilePromptTemplate,
                                                                  final VcsShowConfirmationOption confirmationOption);

    
    public Collection<FilePath> selectFilePathsToProcess(List<FilePath> files,
                                                         final String title,
                                                          final String prompt,
                                                         final String singleFileTitle,
                                                         final String singleFilePromptTemplate,
                                                         final VcsShowConfirmationOption confirmationOption,
                                                          String okActionName,
                                                          String cancelActionName) {
        return selectFilePathsToProcess(files, title, prompt, singleFileTitle, singleFilePromptTemplate, confirmationOption);
    };


    /**
     * <p>Shows commit dialog, fills it with the given changes and given commit message, initially selects the given changelist.</p>
     * <p>Note that the method is asynchronous: it returns right after user presses "Commit" or "Cancel" and after all pre-commit handlers
     *    have been called. It doesn't wait for commit itself to succeed or fail - for this use the {@code customResultHandler}.</p>
     * @return true if user decides to commit the changes, false if user presses Cancel.
     */
    public abstract boolean commitChanges( Collection<Change> changes,  LocalChangeList initialChangeList,
                                           String commitMessage,  CommitResultHandler customResultHandler);
}
