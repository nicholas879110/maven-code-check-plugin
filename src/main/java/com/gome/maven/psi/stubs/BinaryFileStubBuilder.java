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

/*
 * @author max
 */
package com.gome.maven.psi.stubs;

import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.indexing.FileContent;

/**
 * @see com.gome.maven.psi.stubs.BinaryFileStubBuilders#EXTENSION_POINT_NAME
 */
public interface BinaryFileStubBuilder {
    boolean acceptsFile(VirtualFile file);

    Stub buildStubTree(FileContent fileContent);

    int getStubVersion();
}