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
import com.gome.maven.openapi.roots.ModuleRootModel;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.util.Processor;
import gnu.trove.THashSet;

/**
 * @author nik
 */
public class ModuleOrderEnumerator extends OrderEnumeratorBase {
    private final ModuleRootModel myRootModel;

    public ModuleOrderEnumerator(ModuleRootModel rootModel, final OrderRootsCache cache) {
        super(rootModel.getModule(), rootModel.getModule().getProject(), cache);
        myRootModel = rootModel;
    }

    @Override
    public void processRootModules( Processor<Module> processor) {
        processor.process(myRootModel.getModule());
    }

    @Override
    public void forEach( Processor<OrderEntry> processor) {
        processEntries(myRootModel, processor, myRecursively ? new THashSet<Module>() : null, true);
    }

    @Override
    public boolean isRootModuleModel( ModuleRootModel rootModel) {
        return rootModel.equals(myRootModel);
    }
}

