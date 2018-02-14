/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.psi.search.scope.packageSet;

import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * User: anna
 */
public class InvalidPackageSet extends AbstractPackageSet {

    public InvalidPackageSet( String text) {
        super(text);
    }

    @Override
    public boolean contains(VirtualFile file, NamedScopesHolder holder) {
        return false;
    }
}
