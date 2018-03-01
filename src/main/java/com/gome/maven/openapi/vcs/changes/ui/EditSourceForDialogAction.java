/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.actions.EditSourceAction;
import com.gome.maven.idea.ActionsBundle;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.util.OpenSourceUtil;

import java.awt.*;

public class EditSourceForDialogAction extends EditSourceAction {
     private final Component mySourceComponent;

    public EditSourceForDialogAction( Component component) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText(ActionsBundle.actionText("EditSource"));
        presentation.setIcon(AllIcons.Actions.EditSource);
        presentation.setDescription(ActionsBundle.actionDescription("EditSource"));
        mySourceComponent = component;
    }

    public void actionPerformed(AnActionEvent e) {
        final Navigatable[] navigatableArray = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
        if (navigatableArray != null && navigatableArray.length > 0) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    OpenSourceUtil.navigate(navigatableArray);
                }
            });
            DialogWrapper dialog = DialogWrapper.findInstance(mySourceComponent);
            if (dialog != null && dialog.isModal()) {
                dialog.doCancelAction();
            }
        }
    }
}
