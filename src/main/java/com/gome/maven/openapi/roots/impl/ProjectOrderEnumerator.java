/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootModel;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.util.Processor;
import gnu.trove.THashSet;

/**
 * @author nik
 */
public class ProjectOrderEnumerator extends OrderEnumeratorBase {
    private final Project myProject;

    public ProjectOrderEnumerator(Project project, OrderRootsCache rootsCache) {
        super(null, project, rootsCache);
        myProject = project;
    }

    @Override
    public void processRootModules( Processor<Module> processor) {
        Module[] modules = myModulesProvider != null ? myModulesProvider.getModules() : ModuleManager.getInstance(myProject).getSortedModules();
        for (Module each : modules) {
            processor.process(each);
        }
    }

    @Override
    public void forEach( final Processor<OrderEntry> processor) {
        myRecursively = false;
        myWithoutDepModules = true;
        final THashSet<Module> processed = new THashSet<Module>();
        processRootModules(new Processor<Module>() {
            @Override
            public boolean process(Module module) {
                processEntries(getRootModel(module), processor, processed, true);
                return true;
            }
        });
    }

    @Override
    public boolean isRootModuleModel( ModuleRootModel rootModel) {
        return true;
    }
}
