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
package com.gome.maven.openapi.vcs.history;

import com.gome.maven.openapi.actionSystem.AnAction;
//import org.jetbrains.annotations.CalledInBackground;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.VcsProviderMarker;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;

public interface VcsHistoryProvider extends VcsProviderMarker {

    VcsDependentHistoryComponents getUICustomization(final VcsHistorySession session, final JComponent forShortcutRegistration);

    AnAction[] getAdditionalActions(final Runnable refresher);

    /**
     * Returns whether the history provider submits the custom-formatted date
     * and the standard "Date" column must be omitted to get rid of confusion.
     * @return true if history provider submits the custom-formatted date.
     */
    boolean isDateOmittable();

    
    
    String getHelpId();

    /**
     * Returns the history session for the specified file path.
     *
     * @param filePath the file path for which the session is requested.
     * @return the session, or null if the initial revisions loading process was cancelled.
     * @throws VcsException if an error occurred when loading the revisions
     */
    
//    @CalledInBackground
    VcsHistorySession createSessionFor(FilePath filePath) throws VcsException;

    void reportAppendableHistory(final FilePath path, final VcsAppendableHistorySessionPartner partner) throws VcsException;

    boolean supportsHistoryForDirectories();

    /**
     * The returned {@link DiffFromHistoryHandler} will be called, when user calls "Show Diff" from the file history panel.
     * If {@code null} is returned, the standard handler will be used, which is suitable for most cases.
     */
    
    DiffFromHistoryHandler getHistoryDiffHandler();

    /**
     * Provide any additional restrictions for showing history for the given file.
     * Basic restrictions are checked in the TabbedShowHistoryAction.
     * @param file File which history is requested or may be requested.
     * @return true if the VCS can show history for this file.
     */
    boolean canShowHistoryFor( VirtualFile file);

}
