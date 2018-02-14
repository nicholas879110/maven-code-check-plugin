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
package com.gome.maven.compiler.impl;

import com.gome.maven.compiler.options.CompilerConfigurable;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.compiler.CompilerBundle;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.project.Project;

/**
 * @author Eugene Zhuravlev
 *         Date: 9/12/12
 */
class CompilerPropertiesAction extends AnAction {
    public CompilerPropertiesAction() {
        super(CompilerBundle.message("action.compiler.properties.text"), null, AllIcons.General.Settings);
    }

    public void actionPerformed(AnActionEvent e) {
        Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project != null) {
            ShowSettingsUtil.getInstance().editConfigurable(project, new CompilerConfigurable(project));
        }
    }
}
