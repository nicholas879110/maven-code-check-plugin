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

import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * Represents a source or exclude root under the content root of a module.
 *
 * @see ContentEntry#getSourceFolders()
 * @see ContentEntry#getExcludeFolders()
 * @author dsl
 */
public interface ContentFolder extends Synthetic {
    /**
     * Returns the root file or directory for this root.
     *
     * @return the file or directory, or null if the source path is invalid.
     */
    
    VirtualFile getFile();

    /**
     * Returns the content entry to which this root belongs.
     *
     * @return this <code>ContentFolder</code>s {@link com.gome.maven.openapi.roots.ContentEntry}.
     */
    
    ContentEntry getContentEntry();

    /**
     * Returns the URL of the root file or directory for this root.
     *
     * @return the root file or directory URL.
     */
    
    String getUrl();
}
