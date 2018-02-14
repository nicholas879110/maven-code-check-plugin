/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.compiler.generic;

import com.gome.maven.openapi.compiler.CompileContext;
import com.gome.maven.openapi.compiler.CompileScope;
import com.gome.maven.openapi.compiler.Compiler;
import com.gome.maven.util.io.DataExternalizer;
import com.gome.maven.util.io.EnumeratorStringDescriptor;
import com.gome.maven.util.io.KeyDescriptor;

/**
 * @author nik
 *
 * @deprecated this interface is part of the obsolete build system which runs as part of the IDE process. Since IDEA 12 plugins need to
 * integrate into 'external build system' instead (https://confluence.jetbrains.com/display/IDEADEV/External+Builder+API+and+Plugins).
 * Since IDEA 13 users cannot switch to the old build system via UI and it will be completely removed in IDEA 14.
 */
public abstract class GenericCompiler<Key, SourceState, OutputState> implements Compiler {
    protected static final KeyDescriptor<String> STRING_KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private final String myId;
    private final int myVersion;
    private final CompileOrderPlace myOrderPlace;

    protected GenericCompiler( String id, int version,  CompileOrderPlace orderPlace) {
        myId = id;
        myVersion = version;
        myOrderPlace = orderPlace;
    }

    
    public abstract KeyDescriptor<Key> getItemKeyDescriptor();
    
    public abstract DataExternalizer<SourceState> getSourceStateExternalizer();
    
    public abstract DataExternalizer<OutputState> getOutputStateExternalizer();

    
    public abstract GenericCompilerInstance<?, ? extends CompileItem<Key, SourceState, OutputState>, Key, SourceState, OutputState> createInstance( CompileContext context);

    public final String getId() {
        return myId;
    }

    public final int getVersion() {
        return myVersion;
    }

    @Override
    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public CompileOrderPlace getOrderPlace() {
        return myOrderPlace;
    }

    public enum CompileOrderPlace {
        CLASS_INSTRUMENTING, CLASS_POST_PROCESSING, PACKAGING, VALIDATING
    }

}
