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
package com.gome.maven.openapi.project.ex;

import com.gome.maven.openapi.components.impl.stores.IProjectStore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.messages.Topic;

public interface ProjectEx extends Project {
    interface ProjectSaved {
        Topic<ProjectSaved> TOPIC = Topic.create("SaveProjectTopic", ProjectSaved.class, Topic.BroadcastDirection.NONE);
        void saved( final Project project);
    }

    IProjectStore getStateStore();

    void init();

    boolean isOptimiseTestLoadSpeed();

    void setOptimiseTestLoadSpeed(boolean optimiseTestLoadSpeed);

    void checkUnknownMacros(final boolean showDialog);

    void setProjectName( String name);
}
