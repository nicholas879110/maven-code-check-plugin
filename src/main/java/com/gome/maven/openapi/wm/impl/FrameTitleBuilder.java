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
package com.gome.maven.openapi.wm.impl;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.project.Project;

/**
 * @author yole
 */
public abstract class FrameTitleBuilder {
    public static FrameTitleBuilder getInstance() {
        return ServiceManager.getService(FrameTitleBuilder.class);
    }

    public abstract String getProjectTitle( final Project project);

    public abstract String getFileTitle( final Project project,  final VirtualFile file);
}
