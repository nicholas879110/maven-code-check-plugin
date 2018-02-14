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

package com.gome.maven.execution.ui.actions;

import com.gome.maven.execution.ui.layout.Grid;
import com.gome.maven.execution.ui.layout.Tab;
import com.gome.maven.execution.ui.layout.ViewContext;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.ui.content.Content;

public abstract class BaseViewAction extends DumbAwareAction {

    @Override
    public final void update(final AnActionEvent e) {
        ViewContext context = getViewFacade(e);
        Content[] content = getContent(e);

        if (context != null && content != null) {
            if (containsInvalidContent(content)) {
                e.getPresentation().setEnabled(false);
            } else {
                update(e, context, content);
            }
        } else {
            e.getPresentation().setEnabled(false);
        }
    }

    private boolean containsInvalidContent(Content[] content) {
        for (Content each : content) {
            if (!each.isValid()) {
                return true;
            }
        }

        return false;
    }

    protected void update(AnActionEvent e, ViewContext context, Content[] content) {

    }

    @Override
    public final void actionPerformed(final AnActionEvent e) {
        actionPerformed(e, getViewFacade(e), getContent(e));
    }


    protected abstract void actionPerformed(AnActionEvent e, ViewContext context, Content[] content);


    
    private ViewContext getViewFacade(final AnActionEvent e) {
        return e.getData(ViewContext.CONTEXT_KEY);
    }

    
    private Content[] getContent(final AnActionEvent e) {
        return e.getData(ViewContext.CONTENT_KEY);
    }

    
    protected static Tab getTabFor(final ViewContext context, final Content[] content) {
        Grid grid = context.findGridFor(content[0]);
        return context.getTabFor(grid);
    }

    protected final void setEnabled(AnActionEvent e, boolean enabled) {
        e.getPresentation().setVisible(enabled);
    }
}
