/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.packageDependencies;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.impl.ContentManagerWatcher;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.ui.content.Content;
import com.gome.maven.ui.content.ContentManager;

/**
 * @author yole
 */
public class DependenciesToolWindow {
    public static DependenciesToolWindow getInstance(Project project) {
        return ServiceManager.getService(project, DependenciesToolWindow.class);
    }

    private final Project myProject;
    private ContentManager myContentManager;

    public DependenciesToolWindow(final Project project) {
        myProject = project;
        StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
            @Override
            public void run() {
                final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
                if (toolWindowManager == null) return;
                ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.DEPENDENCIES,
                        true,
                        ToolWindowAnchor.BOTTOM,
                        project);
                myContentManager = toolWindow.getContentManager();

                toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowInspection);
                new ContentManagerWatcher(toolWindow, myContentManager);
            }
        });
    }

    public void addContent(Content content) {
        myContentManager.addContent(content);
        myContentManager.setSelectedContent(content);
        ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.DEPENDENCIES).activate(null);
    }

    public void closeContent(Content content) {
        myContentManager.removeContent(content, true);
    }
}
