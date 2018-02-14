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

/*
 * @author max
 */
package com.gome.maven.psi.stubs;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.io.EnumeratorStringDescriptor;
import com.gome.maven.util.io.KeyDescriptor;

public abstract class StringStubIndexExtension<Psi extends PsiElement> extends AbstractStubIndex<String, Psi> {
    public int getVersion() {
        return 2;
    }

    
    public KeyDescriptor<String> getKeyDescriptor() {
        return new EnumeratorStringDescriptor();
    }

    public boolean traceKeyHashToVirtualFileMapping() {
        return false;
    }
}