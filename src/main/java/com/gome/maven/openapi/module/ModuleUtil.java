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

/**
 * @author cdr
 */
package com.gome.maven.openapi.module;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ContentEntry;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.util.CachedValueProvider;
import com.gome.maven.psi.util.CachedValuesManager;
import com.gome.maven.psi.util.ParameterizedCachedValue;
import com.gome.maven.psi.util.ParameterizedCachedValueProvider;
import com.gome.maven.util.containers.MultiMap;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModuleUtil extends ModuleUtilCore {
    private static final ParameterizedCachedValueProvider<MultiMap<ModuleType<?>,Module>,Project> MODULE_BY_TYPE_VALUE_PROVIDER =
            new ParameterizedCachedValueProvider<MultiMap<ModuleType<?>, Module>, Project>() {
                
                @Override
                public CachedValueProvider.Result<MultiMap<ModuleType<?>, Module>> compute(Project param) {
                    MultiMap<ModuleType<?>, Module> map = new MultiMap<ModuleType<?>, Module>();
                    for (Module module : ModuleManager.getInstance(param).getModules()) {
                        map.putValue(ModuleType.get(module), module);
                    }
                    return CachedValueProvider.Result.createSingleDependency(map, ProjectRootManager.getInstance(param));
                }
            };
    private static final Key<ParameterizedCachedValue<MultiMap<ModuleType<?>, Module>, Project>> MODULES_BY_TYPE_KEY = Key.create("MODULES_BY_TYPE");

    private ModuleUtil() {}

    /**
     * @deprecated use ModuleManager#getModuleDependentModules(com.intellij.openapi.module.Module) instead
     */
    
    public static Module getParentModuleOfType(ModuleType expectedModuleType, Module module) {
        if (module == null) return null;
        if (expectedModuleType.equals(ModuleType.get(module))) return module;
        final List<Module> parents = getParentModulesOfType(expectedModuleType, module);
        return parents.isEmpty() ? null : parents.get(0);
    }

    /**
     * @deprecated use ModuleManager#getModuleDependentModules(com.intellij.openapi.module.Module) instead
     */
    
    public static List<Module> getParentModulesOfType(ModuleType expectedModuleType, Module module) {
        final List<Module> parents = ModuleManager.getInstance(module.getProject()).getModuleDependentModules(module);
        ArrayList<Module> modules = new ArrayList<Module>();
        for (Module parent : parents) {
            if (expectedModuleType.equals(ModuleType.get(parent))) {
                modules.add(parent);
            }
        }
        return modules;
    }

    
    public static Collection<Module> getModulesOfType( Project project,  ModuleType<?> moduleType) {
        return CachedValuesManager.getManager(project).getParameterizedCachedValue(project, MODULES_BY_TYPE_KEY, MODULE_BY_TYPE_VALUE_PROVIDER,
                false, project).get(moduleType);
    }

    public static boolean hasModulesOfType( Project project,  ModuleType<?> module) {
        return !getModulesOfType(project, module).isEmpty();
    }

    public static boolean isSupportedRootType(Project project, JpsModuleSourceRootType sourceRootType) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            if (ModuleType.get(module).isSupportedRootType(sourceRootType)) {
                return true;
            }
        }
        return modules.length == 0;
    }

    
    public static ModuleType getModuleType( Module module) {
        String type = module.getOptionValue(Module.ELEMENT_TYPE);
        return ModuleTypeManager.getInstance().findByID(type);
    }
}
