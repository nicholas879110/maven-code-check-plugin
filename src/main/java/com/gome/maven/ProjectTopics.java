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
package com.gome.maven;

import com.gome.maven.openapi.project.ModuleListener;
import com.gome.maven.openapi.roots.ModuleRootListener;
import com.gome.maven.util.messages.Topic;

public class ProjectTopics {
    public static final Topic<ModuleRootListener> PROJECT_ROOTS = new Topic<ModuleRootListener>("project root changes", ModuleRootListener.class);
    public static final Topic<ModuleListener> MODULES = new Topic<ModuleListener>("modules added or removed from project", ModuleListener.class);

    private ProjectTopics() {
    }
}