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
package com.gome.maven.openapi.components.store;

import com.gome.maven.openapi.components.StateStorage;
import com.gome.maven.openapi.vfs.VirtualFile;

public final class ReadOnlyModificationException extends RuntimeException {
    private final VirtualFile myFile;
    private final StateStorage.SaveSession mySession;

    public ReadOnlyModificationException( VirtualFile file, Throwable cause, StateStorage.SaveSession session) {
        super(cause);

        myFile = file;
        mySession = session;
    }

    
    public VirtualFile getFile() {
        return myFile;
    }

   
    public StateStorage.SaveSession getSession() {
        return mySession;
    }
}