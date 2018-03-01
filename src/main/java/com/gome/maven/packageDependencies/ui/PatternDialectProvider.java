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

/*
 * User: anna
 * Date: 16-Jan-2008
 */
package com.gome.maven.packageDependencies.ui;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.scope.packageSet.PackageSet;

import javax.swing.*;
import java.util.Set;

public abstract class PatternDialectProvider {
    public static final ExtensionPointName<PatternDialectProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.patternDialectProvider");

    public static PatternDialectProvider getInstance(String shortName) {
        for (PatternDialectProvider provider : Extensions.getExtensions(EP_NAME)) {
            if (Comparing.strEqual(provider.getShortName(), shortName)) return provider;
        }
        return ProjectPatternProvider.FILE.equals(shortName) ? null : getInstance(ProjectPatternProvider.FILE);
    }

    public abstract TreeModel createTreeModel(Project project, Marker marker);

    public abstract TreeModel createTreeModel(Project project, Set<PsiFile> deps, Marker marker,
                                              final DependenciesPanel.DependencyPanelSettings settings);

    public abstract String getDisplayName();

     
    public abstract String getShortName();

    public abstract AnAction[] createActions(Project project, final Runnable update);

    
    public abstract PackageSet createPackageSet(final PackageDependenciesNode node, final boolean recursively);

    
    protected static String getModulePattern(final PackageDependenciesNode node) {
        final ModuleNode moduleParent = getModuleParent(node);
        return moduleParent != null ? moduleParent.getModuleName() : null;
    }

    
    protected static ModuleNode getModuleParent(PackageDependenciesNode node) {
        if (node instanceof ModuleNode) return (ModuleNode)node;
        if (node == null || node instanceof RootNode) return null;
        return getModuleParent((PackageDependenciesNode)node.getParent());
    }

    public abstract Icon getIcon();
}
