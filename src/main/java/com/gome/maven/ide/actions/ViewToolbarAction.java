
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
package com.gome.maven.ide.actions;

import com.gome.maven.ide.ui.UISettings;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.project.DumbAware;

public class ViewToolbarAction extends ToggleAction implements DumbAware {
    public ViewToolbarAction() {
        super("Show Toolbar");
    }

    public boolean isSelected(AnActionEvent event) {
        return UISettings.getInstance().SHOW_MAIN_TOOLBAR;
    }

    public void setSelected(AnActionEvent event,boolean state) {
        UISettings uiSettings = UISettings.getInstance();
        uiSettings.SHOW_MAIN_TOOLBAR=state;
        uiSettings.fireUISettingsChanged();
    }
}