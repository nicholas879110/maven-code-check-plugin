/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.gome.maven.ide.scopeView;

import com.gome.maven.ide.SelectInContext;
import com.gome.maven.ide.SelectInManager;
import com.gome.maven.ide.StandardTargetWeights;
import com.gome.maven.ide.impl.ProjectViewSelectInTarget;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.packageDependencies.DependencyValidationManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;
import com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder;
import com.gome.maven.psi.search.scope.packageSet.PackageSet;

/**
 * @author cdr
 */
public class ScopePaneSelectInTarget extends ProjectViewSelectInTarget {
    public ScopePaneSelectInTarget(final Project project) {
        super(project);
    }

    public String toString() {
        return SelectInManager.SCOPE;
    }

    @Override
    public boolean canSelect(PsiFileSystemItem fileSystemItem) {
        if (!super.canSelect(fileSystemItem)) return false;
        if (!(fileSystemItem instanceof PsiFile)) return false;
        return getContainingScope((PsiFile)fileSystemItem) != null;
    }

    
    private NamedScope getContainingScope(PsiFile file) {
        NamedScopesHolder scopesHolder = DependencyValidationManager.getInstance(myProject);
        for (NamedScope scope : ScopeViewPane.getShownScopes(myProject)) {
            PackageSet packageSet = scope.getValue();
            if (packageSet != null && packageSet.contains(file, scopesHolder)) {
                return scope;
            }
        }
        return null;
    }

    @Override
    public void select(PsiElement element, boolean requestFocus) {
        if (getSubId() == null) {
            NamedScope scope = getContainingScope(element.getContainingFile());
            if (scope == null) return;
            setSubId(scope.getName());
        }
        super.select(element, requestFocus);
    }

    @Override
    public String getMinorViewId() {
        return ScopeViewPane.ID;
    }

    @Override
    public float getWeight() {
        return StandardTargetWeights.SCOPE_WEIGHT;
    }

    @Override
    protected boolean canWorkWithCustomObjects() {
        return false;
    }

    @Override
    public boolean isSubIdSelectable( String subId,  SelectInContext context) {
        PsiFileSystemItem file = getContextPsiFile(context);
        if (!(file instanceof PsiFile)) return false;
        final NamedScope scope = NamedScopesHolder.getScope(myProject, subId);
        PackageSet packageSet = scope != null ? scope.getValue() : null;
        if (packageSet == null) return false;
        NamedScopesHolder holder = NamedScopesHolder.getHolder(myProject, subId, DependencyValidationManager.getInstance(myProject));
        return packageSet.contains((PsiFile)file, holder);
    }
}
