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
package com.gome.maven.openapi.vfs.impl.jrt;

import com.gome.maven.openapi.vfs.impl.ArchiveHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

class JrtHandlerStub extends ArchiveHandler {
    public JrtHandlerStub( String path) {
        super(path);
    }

    
    @Override
    protected Map<String, EntryInfo> createEntriesMap() throws IOException {
        return Collections.emptyMap();
    }

    
    @Override
    public byte[] contentsToByteArray( String relativePath) throws IOException {
        return new byte[0];
    }
}
