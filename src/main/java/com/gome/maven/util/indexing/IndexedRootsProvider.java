/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.util.indexing;

import com.gome.maven.openapi.extensions.ExtensionPointName;

import java.util.Set;

/**
 * @deprecated extend {@link com.gome.maven.util.indexing.IndexableSetContributor} instead
 * @author Dmitry Avdeev
 */
public interface IndexedRootsProvider {

    ExtensionPointName<IndexedRootsProvider> EP_NAME = new ExtensionPointName<IndexedRootsProvider>("com.gome.maven.indexedRootsProvider");

    /**
     * @deprecated
     * @return each string is VFS url {@link com.gome.maven.openapi.vfs.VirtualFile#getUrl()} of the root to index. Cannot depend on project.
     */
    Set<String> getRootsToIndex();
}
