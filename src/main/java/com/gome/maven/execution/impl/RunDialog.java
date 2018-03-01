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

package com.gome.maven.execution.impl;

import com.gome.maven.execution.ExecutionBundle;
import com.gome.maven.execution.Executor;
import com.gome.maven.execution.RunnerAndConfigurationSettings;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.openapi.help.HelpManager;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.ex.SingleConfigurableEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.wm.ex.IdeFocusTraversalPolicy;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RunDialog extends DialogWrapper implements RunConfigurable.RunDialogBase {
    private final Project myProject;
    private final RunConfigurable myConfigurable;
    private JComponent myCenterPanel;
     public static final String HELP_ID = "reference.dialogs.rundebug";
    private final Executor myExecutor;

    public RunDialog(final Project project, final Executor executor) {
        super(project, true);
        myProject = project;
        myExecutor = executor;

        final String title = executor.getId();
        setTitle(title);

        setOKButtonText(executor.getStartActionText());
        setOKButtonIcon(executor.getIcon());

        myConfigurable = new RunConfigurable(project, this);
        init();
        myConfigurable.reset();
    }

    @Override
    
    protected Action[] createActions(){
        return new Action[]{getOKAction(),getCancelAction(),new ApplyAction(),getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(HELP_ID);
    }

    @Override
    protected String getDimensionServiceKey(){
        return "#com.gome.maven.execution.impl.RunDialog";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCenterPanel);
    }

    @Override
    protected void doOKAction(){
        try{
            myConfigurable.apply();
        }
        catch(ConfigurationException e){
            Messages.showMessageDialog(myProject, e.getMessage(), ExecutionBundle.message("invalid.data.dialog.title"), Messages.getErrorIcon());
            return;
        }
        super.doOKAction();
    }

    @Override
    protected JComponent createCenterPanel() {
        myCenterPanel = myConfigurable.createComponent();
        return myCenterPanel;
    }

    @Override
    public void setOKActionEnabled(final boolean isEnabled){
        super.setOKActionEnabled(isEnabled);
    }

    @Override
    protected void dispose() {
        myConfigurable.disposeUIResources();
        super.dispose();
    }

    public static boolean editConfiguration(final Project project, final RunnerAndConfigurationSettings configuration, final String title) {
        return editConfiguration(project, configuration, title, null);
    }

    public static boolean editConfiguration( ExecutionEnvironment environment,  String title) {
        return editConfiguration(environment.getProject(), environment.getRunnerAndConfigurationSettings(), title, environment.getExecutor());
    }

    public static boolean editConfiguration(final Project project, final RunnerAndConfigurationSettings configuration, final String title,  final Executor executor) {
        final SingleConfigurationConfigurable<RunConfiguration> configurable =
                SingleConfigurationConfigurable.editSettings(configuration, executor);
        final SingleConfigurableEditor dialog = new SingleConfigurableEditor(project, configurable, IdeModalityType.PROJECT) {
            {
                if (executor != null) setOKButtonText(executor.getActionName());
                if (executor != null) setOKButtonIcon(executor.getIcon());
            }
        };

        dialog.setTitle(title);
        return dialog.showAndGet();
    }

    private class ApplyAction extends AbstractAction {
        public ApplyAction() {
            super(ExecutionBundle.message("apply.action.name"));
        }

        @Override
        public void actionPerformed(final ActionEvent event) {
            try{
                myConfigurable.apply();
            }
            catch(ConfigurationException e){
                Messages.showMessageDialog(myProject, e.getMessage(), ExecutionBundle.message("invalid.data.dialog.title"), Messages.getErrorIcon());
            }
        }
    }

    @Override
    public Executor getExecutor() {
        return myExecutor;
    }
}
