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

package com.gome.maven.codeInspection.actions;

import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.codeInspection.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;

public class CleanupAllIntention extends CleanupIntention {

    public static final CleanupAllIntention INSTANCE = new CleanupAllIntention();

    private CleanupAllIntention() {}

    
    @Override
    public String getFamilyName() {
        return InspectionsBundle.message("cleanup.in.file");
    }

    
    @Override
    protected AnalysisScope getScope(Project project, PsiFile file) {
        return new AnalysisScope(file);
    }
}
