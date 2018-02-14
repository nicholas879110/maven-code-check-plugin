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

package com.gome.maven.openapi.vcs;

import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vcs.changes.committed.DecoratorManager;
import com.gome.maven.openapi.vcs.changes.committed.VcsCommittedListsZipper;
import com.gome.maven.openapi.vcs.changes.committed.VcsCommittedViewAuxiliary;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.gome.maven.openapi.vcs.versionBrowser.ChangesBrowserSettingsEditor;
import com.gome.maven.openapi.vcs.versionBrowser.CommittedChangeList;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.AsynchConsumer;

import java.util.List;

/**
 * @author yole
 */
public interface CommittedChangesProvider<T extends CommittedChangeList, U extends ChangeBrowserSettings> extends VcsProviderMarker {
    
    U createDefaultSettings();
    ChangesBrowserSettingsEditor<U> createFilterUI(final boolean showDateFilter);

    
    RepositoryLocation getLocationFor(FilePath root);
    
    RepositoryLocation getLocationFor(final FilePath root, final String repositoryPath);

    
    VcsCommittedListsZipper getZipper();

    List<T> getCommittedChanges(U settings, RepositoryLocation location, final int maxCount) throws VcsException;

    void loadCommittedChanges(U settings, RepositoryLocation location, final int maxCount, final AsynchConsumer<CommittedChangeList> consumer) throws VcsException;

    ChangeListColumn[] getColumns();

    
    VcsCommittedViewAuxiliary createActions(final DecoratorManager manager, final RepositoryLocation location);

    /**
     * since may be different for different VCSs
     */
    int getUnlimitedCountValue();

    /**
     * @return required list and path of the target file in that revision (changes when move/rename)
     */
    
    Pair<T, FilePath> getOneList(final VirtualFile file, final VcsRevisionNumber number) throws VcsException;

    RepositoryLocation getForNonLocal(final VirtualFile file);

    /**
     * Return true if this committed changes provider can be used to show the incoming changes.
     * If false is returned, the "Incoming" tab won't be shown in the Changes toolwindow.
     */
    boolean supportsIncomingChanges();
}
