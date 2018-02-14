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

package com.gome.maven.openapi.vcs.rollback;

import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.VcsProviderMarker;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * Interface for performing VCS rollback / revert operations.
 *
 * @author yole
 * @since 7.0
 */
public interface RollbackEnvironment extends VcsProviderMarker {
    /**
     * Returns the name of operation which is shown in the UI (in menu item name, dialog title and button text).
     *
     * @return the user-readable name of operation (for example, "Rollback" or "Revert").
     */
    String getRollbackOperationName();

    /**
     * Rolls back the specified changes.
     *
     * @param changes the changes to roll back.
     * @param vcsExceptions
     * @param listener
     * @return list of errors occurred, or an empty list if no errors occurred.
     */
    void rollbackChanges(List<Change> changes, final List<VcsException> vcsExceptions,  final RollbackProgressListener listener);

    /**
     * Rolls back the deletion of files which have been deleted locally but not scheduled for deletion
     * from VCS. The implementation of this method should get the current version of the listed files from VCS.
     * You do not need to implement this method if you never report such files to
     * {@link com.gome.maven.openapi.vcs.changes.ChangelistBuilder#processLocallyDeletedFile}.
     *
     * @param files the files to rollback deletion of.
     * @param exceptions
     * @param listener @return list of errors occurred, or an empty list if no errors occurred.
     */
    void rollbackMissingFileDeletion(List<FilePath> files, final List<VcsException> exceptions,
                                     final RollbackProgressListener listener);

    /**
     * Rolls back the modifications of files which have been made writable but not properly checked out from VCS.
     * You do not need to implement this method if you never report such files to
     * {@link com.gome.maven.openapi.vcs.changes.ChangelistBuilder#processModifiedWithoutCheckout}.
     *
     * @param files the files to rollback.
     * @param exceptions
     * @param listener @return list of errors occurred, or an empty list if no errors occurred.
     */
    void rollbackModifiedWithoutCheckout(List<VirtualFile> files, final List<VcsException> exceptions,
                                         final RollbackProgressListener listener);

    /**
     * This is called when the user performs an undo that returns a file to a state in which it was
     * checked out or last saved. The implementation of this method can compare the current state of file
     * with the base revision and undo the checkout if the file is identical. Implementing this method is
     * optional.
     *
     * @param file the file to rollback.
     */
    void rollbackIfUnchanged(VirtualFile file);
}
