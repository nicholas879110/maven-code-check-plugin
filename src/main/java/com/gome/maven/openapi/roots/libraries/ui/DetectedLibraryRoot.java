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
package com.gome.maven.openapi.roots.libraries.ui;

import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.libraries.LibraryRootType;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class DetectedLibraryRoot {
    private final VirtualFile myFile;
    private final List<LibraryRootType> myTypes;

    public DetectedLibraryRoot( VirtualFile file,  OrderRootType rootType, boolean jarDirectory) {
        this(file, Collections.singletonList(new LibraryRootType(rootType, jarDirectory)));
    }

    public DetectedLibraryRoot( VirtualFile file,  List<LibraryRootType> types) {
        myFile = file;
        myTypes = types;
    }

    
    public VirtualFile getFile() {
        return myFile;
    }

    
    public List<LibraryRootType> getTypes() {
        return myTypes;
    }
}
