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

package com.gome.maven.usages.impl;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.ui.IdeBorderFactory;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usages.UsageContextPanel;
import com.gome.maven.usages.UsageViewPresentation;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author cdr
 */
public abstract class UsageContextPanelBase extends JPanel implements UsageContextPanel {
    protected final Project myProject;
     protected final UsageViewPresentation myPresentation;
    protected volatile boolean isDisposed;

    public UsageContextPanelBase( Project project,  UsageViewPresentation presentation) {
        myProject = project;
        myPresentation = presentation;
        setLayout(new BorderLayout());
        setBorder(IdeBorderFactory.createBorder());
    }

    
    @Override
    public final JComponent createComponent() {
        isDisposed = false;
        return this;
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public final void updateLayout( final List<UsageInfo> infos) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (isDisposed || myProject.isDisposed()) return;
                updateLayoutLater(infos);
            }
        });
    }

    protected abstract void updateLayoutLater( List<UsageInfo> infos);
}
