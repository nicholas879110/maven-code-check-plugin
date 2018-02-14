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
package com.gome.maven.openapi.vcs.checkin;

import com.gome.maven.openapi.vcs.CheckinProjectPanel;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.VcsProviderMarker;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangeList;
import com.gome.maven.openapi.vcs.ui.RefreshableOnComponent;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.PairConsumer;

import java.util.List;
import java.util.Set;

/**
 * Interface for performing VCS checkin / commit / submit operations.
 *
 * @author lesya
 * @see com.gome.maven.openapi.vcs.AbstractVcs#getCheckinEnvironment()
 */
public interface CheckinEnvironment extends VcsProviderMarker {
    
    RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer);

    
    String getDefaultMessageFor(FilePath[] filesToCheckin);

    
    
    String getHelpId();

    String getCheckinOperationName();

    
    List<VcsException> commit(List<Change> changes, String preparedComment);

    
    List<VcsException> commit(List<Change> changes,
                              String preparedComment,
                               NullableFunction<Object, Object> parametersHolder,
                              Set<String> feedback);

    
    List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files);

    
    List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files);

    boolean keepChangeListAfterCommit(ChangeList changeList);

    /**
     * @return true if VFS refresh has to be performed after commit, because files might have changed during commit
     * (for example, due to keyword substitution in SVN or read-only status in Perforce).
     */
    boolean isRefreshAfterCommitNeeded();
}
