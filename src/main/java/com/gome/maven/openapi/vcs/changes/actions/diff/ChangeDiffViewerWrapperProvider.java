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
package com.gome.maven.openapi.vcs.changes.actions.diff;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.diff.chains.DiffRequestProducerException;
import com.gome.maven.diff.impl.DiffViewerWrapper;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.util.ThreeState;

public interface ChangeDiffViewerWrapperProvider {
    ExtensionPointName<ChangeDiffViewerWrapperProvider> EP_NAME =
            ExtensionPointName.create("com.gome.maven.openapi.vcs.changes.actions.diff.ChangeDiffViewerWrapperProvider");

    
    ThreeState isEquals( Change change1,  Change change2);

    boolean canCreate( Project project,  Change change);

    
    DiffViewerWrapper process( ChangeDiffRequestProducer presentable,
                               UserDataHolder context,
                               ProgressIndicator indicator) throws DiffRequestProducerException, ProcessCanceledException;
}
