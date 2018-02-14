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
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.GlobalInspectionContext;
import com.gome.maven.codeInspection.InspectionProfileEntry;
import com.gome.maven.codeInspection.lang.InspectionExtensionsFactory;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefElementImpl;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.profile.ProfileManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;

public class GlobalInspectionContextUtil {
    public static RefElement retrieveRefElement( PsiElement element,  GlobalInspectionContext globalContext) {
        PsiFile elementFile = element.getContainingFile();
        RefElement refElement = globalContext.getRefManager().getReference(elementFile);
        if (refElement == null) {
            PsiElement context = InjectedLanguageManager.getInstance(elementFile.getProject()).getInjectionHost(elementFile);
            if (context != null) refElement = globalContext.getRefManager().getReference(context.getContainingFile());
        }
        return refElement;
    }


    public static boolean isToCheckMember( RefElement owner,  InspectionProfileEntry tool, Tools tools, ProfileManager profileManager) {
        return isToCheckFile(((RefElementImpl)owner).getContainingFile(), tool, tools, profileManager) && !((RefElementImpl)owner).isSuppressed(tool.getShortName());
    }

    public static boolean isToCheckFile(PsiFile file,  InspectionProfileEntry tool, Tools tools, ProfileManager profileManager) {
        if (tools != null && file != null) {
            for (ScopeToolState state : tools.getTools()) {
                final NamedScope namedScope = state.getScope(file.getProject());
                if (namedScope == null || namedScope.getValue().contains(file, profileManager.getScopesManager())) {
                    if (state.isEnabled()) {
                        InspectionToolWrapper toolWrapper = state.getTool();
                        if (toolWrapper.getTool() == tool) return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }


    public static boolean canRunInspections( Project project, final boolean online) {
        for (InspectionExtensionsFactory factory : Extensions.getExtensions(InspectionExtensionsFactory.EP_NAME)) {
            if (!factory.isProjectConfiguredToRunInspections(project, online)) {
                return false;
            }
        }
        return true;
    }
}
