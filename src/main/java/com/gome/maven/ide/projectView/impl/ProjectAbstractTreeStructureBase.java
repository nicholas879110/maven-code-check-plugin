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

package com.gome.maven.ide.projectView.impl;

import com.gome.maven.ide.projectView.TreeStructureProvider;
import com.gome.maven.ide.util.treeView.AbstractTreeStructureBase;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;

import java.util.Arrays;
import java.util.List;

public abstract class ProjectAbstractTreeStructureBase extends AbstractTreeStructureBase {
    private List<TreeStructureProvider> myProviders;

    protected ProjectAbstractTreeStructureBase(Project project) {
        super(project);
    }

    @Override
    public List<TreeStructureProvider> getProviders() {
        if (myProviders == null) {
            final TreeStructureProvider[] providers = Extensions.getExtensions(TreeStructureProvider.EP_NAME, myProject);
            myProviders = Arrays.asList(providers);
        }
        return myProviders;
    }

    public void setProviders(TreeStructureProvider... treeStructureProviders) {
        myProviders = treeStructureProviders == null ? null : Arrays.asList(treeStructureProviders);
    }
}
