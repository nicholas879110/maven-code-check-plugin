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
package com.gome.maven.openapi.compiler.ex;

import com.gome.maven.openapi.compiler.CompileContext;
import com.gome.maven.openapi.compiler.CompileScope;
import com.gome.maven.openapi.compiler.CompilerMessage;
import com.gome.maven.openapi.vfs.VirtualFile;

public interface CompileContextEx extends CompileContext {

    void addMessage(CompilerMessage message);

    /**
     * the same as FileIndex.isInTestSourceContent(), but takes into account generated output dirs
     */
    boolean isInTestSourceContent( VirtualFile fileOrDir);

    boolean isInSourceContent( VirtualFile fileOrDir);

    void addScope(CompileScope additionalScope);

}
