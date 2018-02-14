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

package com.gome.maven.execution.impl;

import com.gome.maven.execution.BeforeRunTask;
import com.gome.maven.execution.RunnerAndConfigurationSettings;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.configurations.UnknownRunConfiguration;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.SettingsEditor;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.ui.HideableDecorator;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * User: anna
 * Date: 27-Mar-2006
 */
public class ConfigurationSettingsEditorWrapper extends SettingsEditor<RunnerAndConfigurationSettings>
        implements BeforeRunStepsPanel.StepsBeforeRunListener {
    public static DataKey<ConfigurationSettingsEditorWrapper> CONFIGURATION_EDITOR_KEY = DataKey.create("ConfigurationSettingsEditor");
     private static final String EXPAND_PROPERTY_KEY = "ExpandBeforeRunStepsPanel";
    private JPanel myComponentPlace;
    private JPanel myWholePanel;

    private JPanel myBeforeLaunchContainer;
    private final BeforeRunStepsPanel myBeforeRunStepsPanel;

    private final ConfigurationSettingsEditor myEditor;
    private final HideableDecorator myDecorator;

    public <T extends SettingsEditor> T selectExecutorAndGetEditor(ProgramRunner runner, Class<T> editorClass) {
        return myEditor.selectExecutorAndGetEditor(runner, editorClass);
    }

    public <T extends SettingsEditor> T selectTabAndGetEditor(Class<T> editorClass) {
        return myEditor.selectTabAndGetEditor(editorClass);
    }

    public ConfigurationSettingsEditorWrapper(final RunnerAndConfigurationSettings settings) {
        myEditor = new ConfigurationSettingsEditor(settings);
        Disposer.register(this, myEditor);
        myBeforeRunStepsPanel = new BeforeRunStepsPanel(this);
        myDecorator = new HideableDecorator(myBeforeLaunchContainer, "", false) {
            @Override
            protected void on() {
                super.on();
                storeState();
            }

            @Override
            protected void off() {
                super.off();
                storeState();
            }
            private void storeState() {
                PropertiesComponent.getInstance().setValue(EXPAND_PROPERTY_KEY, String.valueOf(isExpanded()));
            }
        };
        myDecorator.setOn(PropertiesComponent.getInstance().getBoolean(EXPAND_PROPERTY_KEY, true));
        myDecorator.setContentComponent(myBeforeRunStepsPanel);
        doReset(settings);
    }

    private void doReset(RunnerAndConfigurationSettings settings) {
        final RunConfiguration runConfiguration = settings.getConfiguration();
        myBeforeRunStepsPanel.doReset(settings);
        myBeforeLaunchContainer.setVisible(!(runConfiguration instanceof UnknownRunConfiguration));
    }

    @Override
    
    protected JComponent createEditor() {
        myComponentPlace.setLayout(new BorderLayout());
        myComponentPlace.add(myEditor.getComponent(), BorderLayout.CENTER);
        DataManager.registerDataProvider(myWholePanel, new MyDataProvider());
        return myWholePanel;
    }

    @Override
    public void resetEditorFrom(final RunnerAndConfigurationSettings settings) {
        myEditor.resetEditorFrom(settings);
        doReset(settings);
    }

    @Override
    public void applyEditorTo(final RunnerAndConfigurationSettings settings) throws ConfigurationException {
        myEditor.applyEditorTo(settings);
        doApply(settings);
    }

    @Override
    public RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings result = myEditor.getSnapshot();
        doApply(result);
        return result;
    }

    private void doApply(final RunnerAndConfigurationSettings settings) {
        final RunConfiguration runConfiguration = settings.getConfiguration();
        final RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(runConfiguration.getProject());
        runManager.setBeforeRunTasks(runConfiguration, myBeforeRunStepsPanel.getTasks(true), false);
        RunnerAndConfigurationSettings runManagerSettings = runManager.getSettings(runConfiguration);
        if (runManagerSettings != null) {
            runManagerSettings.setEditBeforeRun(myBeforeRunStepsPanel.needEditBeforeRun());
        } else {
            settings.setEditBeforeRun(myBeforeRunStepsPanel.needEditBeforeRun());
        }
    }

    public void addBeforeLaunchStep(BeforeRunTask<?> task) {
        myBeforeRunStepsPanel.addTask(task);
    }

    public List<BeforeRunTask> getStepsBeforeLaunch() {
        return Collections.unmodifiableList(myBeforeRunStepsPanel.getTasks(true));
    }

    @Override
    public void fireStepsBeforeRunChanged() {
        fireEditorStateChanged();
    }

    @Override
    public void titleChanged(String title) {
        myDecorator.setTitle(title);
    }

    private class MyDataProvider implements DataProvider {

        
        @Override
        public Object getData( String dataId) {
            if (CONFIGURATION_EDITOR_KEY.is(dataId)) {
                return ConfigurationSettingsEditorWrapper.this;
            }
            return null;
        }
    }
}
