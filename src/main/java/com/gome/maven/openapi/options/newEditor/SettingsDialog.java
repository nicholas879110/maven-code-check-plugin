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
package com.gome.maven.openapi.options.newEditor;

import com.gome.maven.CommonBundle;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.help.HelpManager;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableGroup;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

/**
 * @author Sergey.Malenkov
 */
public class SettingsDialog extends DialogWrapper implements DataProvider {
    private final String myDimensionServiceKey;
    private final AbstractEditor myEditor;
    private boolean myApplyButtonNeeded;
    private boolean myResetButtonNeeded;

    public SettingsDialog(Project project, String key,  Configurable configurable, boolean showApplyButton, boolean showResetButton) {
        super(project, true);
        myDimensionServiceKey = key;
        myEditor = new ConfigurableEditor(myDisposable, configurable);
        myApplyButtonNeeded = showApplyButton;
        myResetButtonNeeded = showResetButton;
        init(configurable, project);
    }

    public SettingsDialog( Component parent, String key,  Configurable configurable, boolean showApplyButton, boolean showResetButton) {
        super(parent, true);
        myDimensionServiceKey = key;
        myEditor = new ConfigurableEditor(myDisposable, configurable);
        myApplyButtonNeeded = showApplyButton;
        myResetButtonNeeded = showResetButton;
        init(configurable, null);
    }

    public SettingsDialog( Project project,  ConfigurableGroup[] groups, Configurable configurable, String filter) {
        super(project, true);
        myDimensionServiceKey = "SettingsEditor";
        myEditor = new SettingsEditor(myDisposable, project, groups, configurable, filter);
        myApplyButtonNeeded = true;
        init(null, project);
    }

    private void init(Configurable configurable,  Project project) {
        String name = configurable == null ? null : configurable.getDisplayName();
        String title = CommonBundle.settingsTitle();
        if (project != null && project.isDefault()) title = "Default " + title;
        setTitle(name == null ? title : name.replaceAll("\n", " "));
        init();
    }

    public Object getData( String dataId) {
        if (myEditor instanceof DataProvider) {
            DataProvider provider = (DataProvider)myEditor;
            return provider.getData(dataId);
        }
        return null;
    }

    @Override
    protected String getDimensionServiceKey() {
        return myDimensionServiceKey;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myEditor.getPreferredFocusedComponent();
    }

    @Override
    public boolean isTypeAheadEnabled() {
        return true;
    }

    
    @Override
    protected DialogStyle getStyle() {
        return DialogStyle.COMPACT;
    }

    protected JComponent createCenterPanel() {
        return myEditor;
    }

    
    @Override
    protected Action[] createActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(getOKAction());
        actions.add(getCancelAction());
        Action apply = myEditor.getApplyAction();
        if (apply != null && myApplyButtonNeeded) {
            actions.add(apply);
        }
        Action reset = myEditor.getResetAction();
        if (reset != null && myResetButtonNeeded) {
            actions.add(reset);
        }
        String topic = myEditor.getHelpTopic();
        if (topic != null) {
            actions.add(getHelpAction());
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    protected void doHelpAction() {
        String topic = myEditor.getHelpTopic();
        if (topic != null) {
            HelpManager.getInstance().invokeHelp(topic);
        }
    }

    @Override
    public void doOKAction() {
        if (myEditor.apply()) {
            ApplicationManager.getApplication().saveAll();
            super.doOKAction();
        }
    }

    @Override
    public void doCancelAction(AWTEvent source) {
        if (source instanceof KeyEvent || source instanceof ActionEvent) {
            if (!myEditor.cancel()) {
                return;
            }
        }
        super.doCancelAction(source);
    }
}
