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
package com.gome.maven.psi.search;

import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import java.util.List;

public abstract class PredefinedSearchScopeProvider {
    public static PredefinedSearchScopeProvider getInstance() {
        return ServiceManager.getService(PredefinedSearchScopeProvider.class);
    }

    public abstract List<SearchScope> getPredefinedScopes( final Project project,
                                                           final DataContext dataContext,
                                                          boolean suggestSearchInLibs,
                                                          boolean prevSearchFiles,
                                                          boolean currentSelection,
                                                          boolean usageView);
}