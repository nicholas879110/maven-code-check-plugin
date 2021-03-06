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
package com.gome.maven.openapi.roots;



import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.EventListener;

/**
 *  Root provider for order entry
 *  @author dsl
 */
public interface RootProvider {
     String[] getUrls( OrderRootType rootType);
     VirtualFile[] getFiles(OrderRootType rootType);

    interface RootSetChangedListener extends EventListener {
        void rootSetChanged(RootProvider wrapper);
    }

    void addRootSetChangedListener( RootSetChangedListener listener);
    void addRootSetChangedListener( RootSetChangedListener listener,  Disposable parentDisposable);
    void removeRootSetChangedListener( RootSetChangedListener listener);
}
