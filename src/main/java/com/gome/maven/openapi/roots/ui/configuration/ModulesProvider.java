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
package com.gome.maven.openapi.roots.ui.configuration;

import com.gome.maven.facet.FacetManager;
import com.gome.maven.facet.FacetModel;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.roots.ModuleRootModel;
import com.gome.maven.openapi.roots.RootModelProvider;

public interface ModulesProvider extends RootModelProvider {
    ModulesProvider EMPTY_MODULES_PROVIDER = new ModulesProvider() {
        @Override
        
        public Module[] getModules() {
            return Module.EMPTY_ARRAY;
        }
        @Override
        public Module getModule(String name) {
            return null;
        }

        @Override
        public ModuleRootModel getRootModel( Module module) {
            return ModuleRootManager.getInstance(module);
        }

        @Override
        public FacetModel getFacetModel( Module module) {
            return FacetManager.getInstance(module);
        }
    };

    
    Module getModule(String name);

    FacetModel getFacetModel( Module module);
}
