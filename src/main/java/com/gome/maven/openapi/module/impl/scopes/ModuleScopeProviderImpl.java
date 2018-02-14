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
package com.gome.maven.openapi.module.impl.scopes;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.impl.ModuleScopeProvider;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.containers.ConcurrentIntObjectMap;
import com.gome.maven.util.containers.ContainerUtil;

/**
 * Author: dmitrylomov
 */
public class ModuleScopeProviderImpl implements ModuleScopeProvider {
    private final Module myModule;
    private final ConcurrentIntObjectMap<GlobalSearchScope> myScopeCache = ContainerUtil.createConcurrentIntObjectMap();
    private ModuleWithDependentsTestScope myModuleTestsWithDependentsScope;

    public ModuleScopeProviderImpl( Module module) {
        myModule = module;
    }

    
    private GlobalSearchScope getCachedScope(@ModuleWithDependenciesScope.ScopeConstant int options) {
        GlobalSearchScope scope = myScopeCache.get(options);
        if (scope == null) {
            scope = new ModuleWithDependenciesScope(myModule, options);
            myScopeCache.put(options, scope);
        }
        return scope;
    }

    @Override
    
    public GlobalSearchScope getModuleScope() {
        return getCachedScope(ModuleWithDependenciesScope.COMPILE | ModuleWithDependenciesScope.TESTS);
    }

    
    @Override
    public GlobalSearchScope getModuleScope(boolean includeTests) {
        return getCachedScope(ModuleWithDependenciesScope.COMPILE | (includeTests ? ModuleWithDependenciesScope.TESTS : 0));
    }

    @Override
    
    public GlobalSearchScope getModuleWithLibrariesScope() {
        return getCachedScope(ModuleWithDependenciesScope.COMPILE | ModuleWithDependenciesScope.TESTS | ModuleWithDependenciesScope.LIBRARIES);
    }

    @Override
    
    public GlobalSearchScope getModuleWithDependenciesScope() {
        return getCachedScope(ModuleWithDependenciesScope.COMPILE | ModuleWithDependenciesScope.TESTS | ModuleWithDependenciesScope.MODULES);
    }

    
    @Override
    public GlobalSearchScope getModuleContentScope() {
        return getCachedScope(ModuleWithDependenciesScope.CONTENT);
    }

    
    @Override
    public GlobalSearchScope getModuleContentWithDependenciesScope() {
        return getCachedScope(ModuleWithDependenciesScope.CONTENT | ModuleWithDependenciesScope.MODULES);
    }

    @Override
    
    public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
        return getCachedScope(ModuleWithDependenciesScope.COMPILE |
                ModuleWithDependenciesScope.MODULES |
                ModuleWithDependenciesScope.LIBRARIES | (includeTests ? ModuleWithDependenciesScope.TESTS : 0));
    }

    @Override
    
    public GlobalSearchScope getModuleWithDependentsScope() {
        return getModuleTestsWithDependentsScope().getBaseScope();
    }

    @Override
    
    public ModuleWithDependentsTestScope getModuleTestsWithDependentsScope() {
        ModuleWithDependentsTestScope scope = myModuleTestsWithDependentsScope;
        if (scope == null) {
            myModuleTestsWithDependentsScope = scope = new ModuleWithDependentsTestScope(myModule);
        }
        return scope;
    }

    @Override
    
    public GlobalSearchScope getModuleRuntimeScope(boolean includeTests) {
        return getCachedScope(
                ModuleWithDependenciesScope.MODULES | ModuleWithDependenciesScope.LIBRARIES | (includeTests ? ModuleWithDependenciesScope.TESTS : 0));
    }

    @Override
    public void clearCache() {
        myScopeCache.clear();
        myModuleTestsWithDependentsScope = null;
    }
}
