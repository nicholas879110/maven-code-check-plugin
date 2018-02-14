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
package com.gome.maven.openapi.editor;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

public abstract class LazyRangeMarkerFactory {
    public static LazyRangeMarkerFactory getInstance(Project project) {
        return ServiceManager.getService(project, LazyRangeMarkerFactory.class);
    }

    
    public abstract RangeMarker createRangeMarker( final VirtualFile file, final int offset);

    
    public abstract RangeMarker createRangeMarker( final VirtualFile file, final int line, final int column, final boolean persistent);
}
