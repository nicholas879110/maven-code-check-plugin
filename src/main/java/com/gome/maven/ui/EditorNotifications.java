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
package com.gome.maven.ui;

import com.gome.maven.openapi.components.AbstractProjectComponent;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public abstract class EditorNotifications extends AbstractProjectComponent {

    public abstract static class Provider<T extends JComponent> {
        
        public abstract Key<T> getKey();

        
        public abstract T createNotificationPanel( VirtualFile file,  FileEditor fileEditor);
    }

    public static EditorNotifications getInstance(Project project) {
        return project.getComponent(EditorNotifications.class);
    }

    public EditorNotifications(final Project project) {
        super(project);
    }

    public abstract void updateNotifications( VirtualFile file);

    public abstract void updateAllNotifications();

    public static void updateAll() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            getInstance(project).updateAllNotifications();
        }
    }
}
