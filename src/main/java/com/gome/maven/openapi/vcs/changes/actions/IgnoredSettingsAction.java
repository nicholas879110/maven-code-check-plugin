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

package com.gome.maven.openapi.vcs.changes.actions;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.changes.ChangeListManager;
import com.gome.maven.openapi.vcs.changes.ui.IgnoredSettingsDialog;

/**
 * @author yole
 */
public class IgnoredSettingsAction extends AnAction implements DumbAware {
    public IgnoredSettingsAction() {
        super("Configure Ignored Files...", "Specify file paths and masks which are ignored",
                AllIcons.Actions.Properties);
    }

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;
        IgnoredSettingsDialog.configure(project);
    }

}
