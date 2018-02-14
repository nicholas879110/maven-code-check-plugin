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
package com.gome.maven.ide.actions;

import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.ShadowAction;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowContentUiType;
import com.gome.maven.openapi.wm.ToolWindowManager;

import javax.swing.*;
import java.awt.*;

public class ShowContentAction extends AnAction implements DumbAware {
    private ToolWindow myWindow;

    @SuppressWarnings({"UnusedDeclaration"})
    public ShowContentAction() {
    }

    public ShowContentAction(ToolWindow window, JComponent c) {
        myWindow = window;
        AnAction original = ActionManager.getInstance().getAction("ShowContent");
        new ShadowAction(this, original, c);
        copyFrom(original);
    }

    @Override
    public void update(AnActionEvent e) {
        final ToolWindow window = getWindow(e);
        e.getPresentation().setEnabled(window != null && window.getContentManager().getContentCount() > 1);
        e.getPresentation().setText(window == null || window.getContentUiType() == ToolWindowContentUiType.TABBED
                ? "Show List of Tabs"
                : "Show List of Views");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        getWindow(e).showContentPopup(e.getInputEvent());
    }

    private ToolWindow getWindow(AnActionEvent event) {
        if (myWindow != null) return myWindow;

        Project project = CommonDataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) return null;

        ToolWindowManager manager = ToolWindowManager.getInstance(project);

        final ToolWindow window = manager.getToolWindow(manager.getActiveToolWindowId());
        if (window == null) return null;

        final Component context = PlatformDataKeys.CONTEXT_COMPONENT.getData(event.getDataContext());
        if (context == null) return null;

        return SwingUtilities.isDescendingFrom(window.getComponent(), context) ? window : null;
    }
}
