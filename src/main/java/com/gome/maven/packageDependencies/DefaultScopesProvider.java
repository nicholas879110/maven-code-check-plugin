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
package com.gome.maven.packageDependencies;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.problems.WolfTheProblemSolver;
import com.gome.maven.psi.search.scope.NonProjectFilesScope;
import com.gome.maven.psi.search.scope.ProjectFilesScope;
import com.gome.maven.psi.search.scope.packageSet.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
public class DefaultScopesProvider extends CustomScopesProviderEx {
    private final NamedScope myProblemsScope;
    private final Project myProject;
    private final List<NamedScope> myScopes;

    public static DefaultScopesProvider getInstance(Project project) {
        return Extensions.findExtension(CUSTOM_SCOPES_PROVIDER, project, DefaultScopesProvider.class);
    }

    public DefaultScopesProvider( Project project) {
        myProject = project;
        final NamedScope projectScope = new ProjectFilesScope();
        final NamedScope nonProjectScope = new NonProjectFilesScope();
        final String text = FilePatternPackageSet.SCOPE_FILE + ":*//*";
        myProblemsScope = new NamedScope(IdeBundle.message("predefined.scope.problems.name"), new AbstractPackageSet(text) {
            @Override
            public boolean contains(VirtualFile file,  NamedScopesHolder holder) {
                return contains(file, holder.getProject(), holder);
            }

            @Override
            public boolean contains(VirtualFile file,  Project project,  NamedScopesHolder holder) {
                return project == myProject
                        && WolfTheProblemSolver.getInstance(myProject).isProblemFile(file);
            }
        });
        myScopes = Arrays.asList(projectScope, getProblemsScope(), getAllScope(), nonProjectScope);
    }

    @Override
    
    public List<NamedScope> getCustomScopes() {
        return myScopes;
    }

    
    public NamedScope getProblemsScope() {
        return myProblemsScope;
    }

    
    public List<NamedScope> getAllCustomScopes() {
        final List<NamedScope> scopes = new ArrayList<NamedScope>();
        for (CustomScopesProvider provider : Extensions.getExtensions(CUSTOM_SCOPES_PROVIDER, myProject)) {
            scopes.addAll(provider.getCustomScopes());
        }
        return scopes;
    }

    
    public NamedScope findCustomScope(String name) {
        for (NamedScope scope : getAllCustomScopes()) {
            if (name.equals(scope.getName())) {
                return scope;
            }
        }
        return null;
    }
}
