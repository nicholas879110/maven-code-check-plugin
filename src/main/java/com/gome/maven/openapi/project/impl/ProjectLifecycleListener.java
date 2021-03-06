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
package com.gome.maven.openapi.project.impl;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.messages.Topic;

/**
 * @author max
 */
public interface ProjectLifecycleListener {
    Topic<ProjectLifecycleListener> TOPIC = Topic.create("Various stages of project lifecycle notifications", ProjectLifecycleListener.class);

    void projectComponentsInitialized(Project project);

    void beforeProjectLoaded( Project project);

    void afterProjectClosed( Project project);

    abstract class Adapter implements ProjectLifecycleListener {
        public void projectComponentsInitialized(final Project project) { }

        public void beforeProjectLoaded( final Project project) { }

        public void afterProjectClosed( Project project) { }
    }
}
