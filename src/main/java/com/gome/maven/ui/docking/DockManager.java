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
package com.gome.maven.ui.docking;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.wm.IdeFrame;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;

public abstract class DockManager {

    public abstract void register(DockContainer container);
    public abstract void register(String id, DockContainerFactory factory);

    public static DockManager getInstance(Project project) {
        return ServiceManager.getService(project, DockManager.class);
    }

    public abstract DragSession createDragSession(MouseEvent mouseEvent,  DockableContent content);

    public abstract Set<DockContainer> getContainers();

    public abstract IdeFrame getIdeFrame(DockContainer container);

    public abstract String getDimensionKeyForFocus( String key);

    
    public abstract DockContainer getContainerFor(Component c);
}
