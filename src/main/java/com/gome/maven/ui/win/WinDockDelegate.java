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
package com.gome.maven.ui.win;

import com.gome.maven.ide.RecentProjectsManager;
import com.gome.maven.ide.ReopenProjectAction;
import com.gome.maven.idea.StartupUtil;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.wm.impl.SystemDock;

import java.io.File;

/**
 * @author Denis Fokin
 */
public class WinDockDelegate implements SystemDock.Delegate {
    private static final String javaExe = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "javaw.exe";
    private static final String argsToExecute = " -classpath \"" +
            PathManager.getJarPathForClass(SocketControlHelper.class) +
            "\" com.gome.maven.ui.win.SocketControlHelper " +
            StartupUtil.getAcquiredPort() +
            " ";

    private static boolean initialized = false;
    private static final SystemDock.Delegate instance = new WinDockDelegate();

    private WinDockDelegate() {}

    @Override
    public void updateRecentProjectsMenu () {
        final AnAction[] recentProjectActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);
        RecentTasks.clear();
        Task[] tasks = new Task[recentProjectActions.length];
        for (int i = 0; i < recentProjectActions.length; i ++) {
            ReopenProjectAction rpa = (ReopenProjectAction)recentProjectActions[i];
            tasks[i] = new Task(javaExe, argsToExecute + RecentTasks.getShortenPath(rpa.getProjectPath()), rpa.getTemplatePresentation().getText());
        }
        RecentTasks.addTasks(tasks);
    }
    synchronized public static SystemDock.Delegate getInstance() {
        if (!initialized) {
            initialized = true;
        }
        return instance;
    }
}
