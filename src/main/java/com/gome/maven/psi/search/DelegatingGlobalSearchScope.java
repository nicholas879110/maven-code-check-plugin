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
package com.gome.maven.psi.search;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;

import java.util.Arrays;

/**
 * @author peter
 */
public class DelegatingGlobalSearchScope extends GlobalSearchScope {
    protected final GlobalSearchScope myBaseScope;
    private final Object myEquality;

    public DelegatingGlobalSearchScope( GlobalSearchScope baseScope) {
        this(baseScope, ArrayUtil.EMPTY_OBJECT_ARRAY);
    }

    public DelegatingGlobalSearchScope( GlobalSearchScope baseScope,  Object... equality) {
        super(baseScope.getProject());
        myBaseScope = baseScope;
        myEquality = Arrays.asList(equality);
    }

    @Override
    public boolean contains( VirtualFile file) {
        return myBaseScope.contains(file);
    }

    @Override
    public int compare( VirtualFile file1,  VirtualFile file2) {
        return myBaseScope.compare(file1, file2);
    }

    @Override
    public boolean isSearchInModuleContent( Module aModule) {
        return myBaseScope.isSearchInModuleContent(aModule);
    }

    @Override
    public boolean isSearchInModuleContent( Module aModule, boolean testSources) {
        return myBaseScope.isSearchInModuleContent(aModule, testSources);
    }

    @Override
    public boolean isSearchInLibraries() {
        return myBaseScope.isSearchInLibraries();
    }

    @Override
    public boolean isSearchOutsideRootModel() {
        return myBaseScope.isSearchOutsideRootModel();
    }

    
    @Override
    public String getDisplayName() {
        return myBaseScope.getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DelegatingGlobalSearchScope that = (DelegatingGlobalSearchScope)o;

        if (!myBaseScope.equals(that.myBaseScope)) return false;
        if (!myEquality.equals(that.myEquality)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = myBaseScope.hashCode();
        result = 31 * result + myEquality.hashCode();
        return result;
    }
}
