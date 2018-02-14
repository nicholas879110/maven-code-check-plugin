/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.continuation.ContinuationPause;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author max
 */
public abstract class ChangeListManager implements ChangeListModification {
    
    public static ChangeListManager getInstance(Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetComponent(project, ChangeListManager.class);
    }

    public abstract void scheduleUpdate();
    public abstract void scheduleUpdate(boolean updateUnversionedFiles);
    public abstract void invokeAfterUpdate(final Runnable afterUpdate, final InvokeAfterUpdateMode mode, final String title,
                                           final ModalityState state);
    public abstract void invokeAfterUpdate(final Runnable afterUpdate, final InvokeAfterUpdateMode mode, final String title,
                                           final Consumer<VcsDirtyScopeManager> dirtyScopeManager,
                                           final ModalityState state);
    
    public abstract boolean ensureUpToDate(boolean canBeCanceled);

    public abstract int getChangeListsNumber();
    public abstract List<LocalChangeList> getChangeListsCopy();
    
    public abstract List<LocalChangeList> getChangeLists();

    public abstract List<File> getAffectedPaths();
    
    public abstract List<VirtualFile> getAffectedFiles();
    public abstract boolean isFileAffected(final VirtualFile file);

    /**
     * @return all changes in all changelists.
     */
    
    public abstract Collection<Change> getAllChanges();

    
    public abstract LocalChangeList findChangeList(final String name);
    
    public abstract LocalChangeList getChangeList(String id);
//  public abstract LocalChangeList addChangeList( String name, final String comment);
//  public abstract void setDefaultChangeList( LocalChangeList list);

    /**
     * Returns currently active changelist
     * @return active changelist
     */
    public abstract LocalChangeList getDefaultChangeList();

    public abstract boolean isDefaultChangeList(ChangeList list);

    
    public abstract LocalChangeList getChangeList( Change change);

    
    public abstract String getChangeListNameIfOnlyOne(Change[] changes);

    
    public abstract Runnable prepareForChangeDeletion(final Collection<Change> changes);

    
    public abstract Change getChange( VirtualFile file);

    
    public abstract LocalChangeList getChangeList( VirtualFile file);

    
    public abstract Change getChange(FilePath file);

    public abstract boolean isUnversioned(VirtualFile file);

    
    public abstract FileStatus getStatus(VirtualFile file);

    
    public abstract Collection<Change> getChangesIn(VirtualFile dir);

    
    public abstract Collection<Change> getChangesIn(FilePath path);

//  public abstract void removeChangeList(final LocalChangeList list);

//  public abstract void moveChangesTo(final LocalChangeList list, final Change[] changes);

    public abstract void addChangeListListener(ChangeListListener listener);
    public abstract void removeChangeListListener(ChangeListListener listener);

    public abstract void registerCommitExecutor(CommitExecutor executor);

    public abstract void commitChanges(LocalChangeList changeList, List<Change> changes);

    public abstract void commitChangesSynchronously(LocalChangeList changeList, List<Change> changes);

    /**
     * @return if commit successful
     */
    public abstract boolean commitChangesSynchronouslyWithResult(LocalChangeList changeList, List<Change> changes);

    public abstract void reopenFiles(List<FilePath> paths);

    public abstract List<CommitExecutor> getRegisteredExecutors();

    public abstract void addFilesToIgnore(final IgnoredFileBean... ignoredFiles);
    public abstract void addDirectoryToIgnoreImplicitly( String path);
    public abstract void setFilesToIgnore(final IgnoredFileBean... ignoredFiles);
    public abstract IgnoredFileBean[] getFilesToIgnore();
    public abstract boolean isIgnoredFile( VirtualFile file);

    
    public abstract String getSwitchedBranch(VirtualFile file);
    public abstract String getDefaultListName();
    public abstract void freeze(final ContinuationPause context, String reason);
    public abstract void letGo();
    public abstract String isFreezed();
    public abstract boolean isFreezedWithNotification( String modalTitle);

    public static boolean isFileChanged(final Project project, final VirtualFile vf) {
        FileStatus status = getInstance(project).getStatus(vf);
        if (FileStatus.NOT_CHANGED.equals(status) || FileStatus.UNKNOWN.equals(status) || FileStatus.IGNORED.equals(status)) {
            return false;
        }
        return true;
    }


    public abstract List<VirtualFile> getModifiedWithoutEditing();

    
    public abstract ThreeState haveChangesUnder( VirtualFile vf);
}
