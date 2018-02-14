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
package com.gome.maven.openapi.roots.libraries;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import java.util.List;

public abstract class LibraryTablesRegistrar {
     public static final String PROJECT_LEVEL = "project";
     public static final String APPLICATION_LEVEL = "application";

    public static LibraryTablesRegistrar getInstance() {
        return ServiceManager.getService(LibraryTablesRegistrar.class);
    }

    
    public abstract LibraryTable getLibraryTable();

    
    public abstract LibraryTable getLibraryTable( Project project);

    public abstract LibraryTable getLibraryTableByLevel( String level,  Project project);

    public abstract void registerLibraryTable( LibraryTable libraryTable);

    public abstract List<LibraryTable> getCustomLibraryTables();
}