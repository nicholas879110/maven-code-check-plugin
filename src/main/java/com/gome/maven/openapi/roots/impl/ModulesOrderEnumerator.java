/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootModel;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.util.Processor;
import gnu.trove.THashSet;

import java.util.Collection;

/**
 * @author nik
 */
public class ModulesOrderEnumerator extends OrderEnumeratorBase {
    private final Collection<? extends Module> myModules;

    public ModulesOrderEnumerator( Project project,  Collection<? extends Module> modules) {
        super(null, project, null);
        myModules = modules;
    }

    @Override
    public void processRootModules( Processor<Module> processor) {
        for (Module each : myModules) {
            processor.process(each);
        }
    }

    @Override
    public void forEach( Processor<OrderEntry> processor) {
        myRecursivelyExportedOnly = false;

        final THashSet<Module> processed = new THashSet<Module>();
        for (Module module : myModules) {
            processEntries(getRootModel(module), processor, processed, true);
        }
    }

    @Override
    public boolean isRootModuleModel( ModuleRootModel rootModel) {
        return myModules.contains(rootModel.getModule());
    }
}
