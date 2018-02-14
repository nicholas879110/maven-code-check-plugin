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
package com.gome.maven.openapi.vcs.update;

import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsProviderMarker;

import java.util.Collection;

/**
 * Implemented by a VCS integration to support "Update" and optionally "Integrate" and "Check Status"
 * operations for project, directories or files.
 *
 * @see com.gome.maven.openapi.vcs.AbstractVcs#getUpdateEnvironment()
 * @see com.gome.maven.openapi.vcs.AbstractVcs#getIntegrateEnvironment()
 * @see com.gome.maven.openapi.vcs.AbstractVcs#getStatusEnvironment()
 */
public interface UpdateEnvironment extends VcsProviderMarker {
    /**
     * Called before the update operation to register file status groups in addition to standard
     * file status groups registered in {@link UpdatedFiles#create}. The implementation can be left
     * empty if the VCS doesn't support any non-standard file statuses.
     *
     * @param updatedFiles the holder for the results of the update/integrate/status operation.
     */
    void fillGroups(UpdatedFiles updatedFiles);

    /**
     * Performs the update/integrate/status operation.
     *
     * @param contentRoots      the content roots for which update/integrate/status was requested by the user.
     * @param updatedFiles      the holder for the results of the update/integrate/status operation.
     * @param progressIndicator the indicator that can be used to report the progress of the operation.
     * @param context           in-out parameter: a link between several sequential update operations (that can be triggered by one update action)
     * @return the update session instance, which can be used to get information about errors that have occurred
     *         during the operation and to perform additional post-update processing.
     * @throws ProcessCanceledException if the update operation has been cancelled by the user. Alternatively,
     *                                  cancellation can be reported by returning true from
     *                                  {@link UpdateSession#isCanceled}.
     */
    
    UpdateSession updateDirectories( FilePath[] contentRoots, UpdatedFiles updatedFiles,
                                    ProgressIndicator progressIndicator,  final Ref<SequentialUpdatesContext> context) throws ProcessCanceledException;

    /**
     * Allows to show a settings dialog for the operation.
     *
     * @param files the content roots for which update/integrate/status will be perfromed.
     * @return the settings dialog instance, or null if the VCS doesn't provide a settings dialog for this operation.
     */
    
    Configurable createConfigurable(Collection<FilePath> files);

    boolean validateOptions(final Collection<FilePath> roots);
}
