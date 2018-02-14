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
package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * Provides information about files contained in a module.
 *
 * @see ModuleRootManager#getFileIndex()
 *
 * @author dsl
 */
public interface ModuleFileIndex extends FileIndex {
    /**
     * Returns the order entry to which the specified file or directory
     * belongs.
     *
     * @param fileOrDir the file or directory to check.
     * @return the order entry to which the file or directory belongs, or null if
     * it does not belong to any order entry.
     */
    OrderEntry getOrderEntryForFile(VirtualFile fileOrDir);

    /**
     * Returns the list of all order entries to which the specified file or directory
     * belongs.
     *
     * @param fileOrDir the file or directory to check.
     * @return the list of order entries to which the file or directory belongs.
     */
    List<OrderEntry> getOrderEntriesForFile( VirtualFile fileOrDir);
}
