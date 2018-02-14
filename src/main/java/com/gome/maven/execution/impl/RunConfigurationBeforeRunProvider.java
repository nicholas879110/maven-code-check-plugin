/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.execution.*;
import com.gome.maven.execution.configurations.ConfigurationType;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.executors.DefaultRunExecutor;
import com.gome.maven.execution.process.ProcessAdapter;
import com.gome.maven.execution.process.ProcessEvent;
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.execution.runners.ExecutionEnvironmentBuilder;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.ui.ColoredListCellRenderer;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.ui.components.JBScrollPane;
import com.gome.maven.util.concurrency.Semaphore;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * @author Vassiliy Kudryashov
 */
public class RunConfigurationBeforeRunProvider
        extends BeforeRunTaskProvider<RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask> {

    public static final Key<RunConfigurableBeforeRunTask> ID = Key.create("RunConfigurationTask");

    private static final Logger LOG = Logger.getInstance("#com.intellij.execution.impl.RunConfigurationBeforeRunProvider");

    private final Project myProject;

    public RunConfigurationBeforeRunProvider(Project project) {
        myProject = project;
    }

    @Override
    public Key<RunConfigurableBeforeRunTask> getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Actions.Execute;
    }

    @Override
    public Icon getTaskIcon(RunConfigurableBeforeRunTask task) {
        if (task.getSettings() == null)
            return null;
        return ProgramRunnerUtil.getConfigurationIcon(task.getSettings(), false);
    }

    @Override
    public String getName() {
        return ExecutionBundle.message("before.launch.run.another.configuration");
    }

    @Override
    public String getDescription(RunConfigurableBeforeRunTask task) {
        if (task.getSettings() == null) {
            return ExecutionBundle.message("before.launch.run.another.configuration");
        }
        else {
            return ExecutionBundle.message("before.launch.run.certain.configuration", task.getSettings().getName());
        }
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    
    public RunConfigurableBeforeRunTask createTask(RunConfiguration runConfiguration) {
        if (runConfiguration.getProject().isInitialized()) {
            Collection<RunnerAndConfigurationSettings> configurations =
                    RunManagerImpl.getInstanceImpl(runConfiguration.getProject()).getSortedConfigurations();
            if (configurations.isEmpty()
                    || (configurations.size() == 1 && configurations.iterator().next().getConfiguration() == runConfiguration)) {
                return null;
            }
        }
        return new RunConfigurableBeforeRunTask();
    }

    @Override
    public boolean configureTask(RunConfiguration runConfiguration, RunConfigurableBeforeRunTask task) {
        SelectionDialog dialog =
                new SelectionDialog(task.getSettings(), getAvailableConfigurations(runConfiguration));
        dialog.show();
        RunnerAndConfigurationSettings settings = dialog.getSelectedSettings();
        if (settings != null) {
            task.setSettings(settings);
            return true;
        }
        else {
            return false;
        }
    }

    
    private static List<RunnerAndConfigurationSettings> getAvailableConfigurations( RunConfiguration runConfiguration) {
        Project project = runConfiguration.getProject();
        if (project == null || !project.isInitialized()) {
            return Collections.emptyList();
        }

        List<RunnerAndConfigurationSettings> configurations = new ArrayList<RunnerAndConfigurationSettings>(RunManagerImpl.getInstanceImpl(project).getSortedConfigurations());
        String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
        for (Iterator<RunnerAndConfigurationSettings> iterator = configurations.iterator(); iterator.hasNext();) {
            RunnerAndConfigurationSettings settings = iterator.next();
            ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
            if (runner == null || settings.getConfiguration() == runConfiguration) {
                iterator.remove();
            }
        }
        return configurations;
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration,
                                  RunConfigurableBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = task.getSettings();
        if (settings == null) {
            return false;
        }
        String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
        final ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
        return runner != null && runner.canRun(executorId, settings.getConfiguration());
    }

    @Override
    public boolean executeTask(final DataContext dataContext,
                               RunConfiguration configuration,
                               final ExecutionEnvironment env,
                               RunConfigurableBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = task.getSettings();
        if (settings == null) {
            return false;
        }
        final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        final String executorId = executor.getId();
        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, settings);
        if (builder == null) {
            return false;
        }
        final ExecutionEnvironment environment = builder.target(env.getExecutionTarget()).build();
        environment.setExecutionId(env.getExecutionId());
        if (!ExecutionTargetManager.canRun(settings, environment.getExecutionTarget())) {
            return false;
        }

        if (!environment.getRunner().canRun(executorId, environment.getRunProfile())) {
            return false;
        }
        else {
            beforeRun(environment);

            final Semaphore targetDone = new Semaphore();
            final Ref<Boolean> result = new Ref<Boolean>(false);
            final Disposable disposable = Disposer.newDisposable();

            myProject.getMessageBus().connect(disposable).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionAdapter() {
                @Override
                public void processStartScheduled(final String executorIdLocal, final ExecutionEnvironment environmentLocal) {
                    if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                        targetDone.down();
                    }
                }

                @Override
                public void processNotStarted(final String executorIdLocal,  final ExecutionEnvironment environmentLocal) {
                    if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                        Boolean skipRun = environment.getUserData(ExecutionManagerImpl.EXECUTION_SKIP_RUN);
                        if (skipRun != null && skipRun) {
                            result.set(true);
                        }
                        targetDone.up();
                    }
                }

                @Override
                public void processStarted(final String executorIdLocal,
                                            final ExecutionEnvironment environmentLocal,
                                            final ProcessHandler handler) {
                    if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                        handler.addProcessListener(new ProcessAdapter() {
                            @Override
                            public void processTerminated(ProcessEvent event) {
                                result.set(event.getExitCode() == 0);
                                targetDone.up();
                            }
                        });
                    }
                }
            });

            try {
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            environment.getRunner().execute(environment);
                        }
                        catch (ExecutionException e) {
                            targetDone.up();
                            LOG.error(e);
                        }
                    }
                }, ModalityState.NON_MODAL);
            }
            catch (Exception e) {
                LOG.error(e);
                Disposer.dispose(disposable);
                return false;
            }

            targetDone.waitFor();
            Disposer.dispose(disposable);

            return result.get();
        }
    }

    private static void beforeRun( ExecutionEnvironment environment) {
        for (RunConfigurationBeforeRunProviderDelegate delegate : Extensions.getExtensions(RunConfigurationBeforeRunProviderDelegate.EP_NAME)) {
            delegate.beforeRun(environment);
        }
    }

    public class RunConfigurableBeforeRunTask extends BeforeRunTask<RunConfigurableBeforeRunTask> {
        private String myConfigurationName;
        private String myConfigurationType;
        private boolean myInitialized = false;

        private RunnerAndConfigurationSettings mySettings;

        RunConfigurableBeforeRunTask() {
            super(ID);
        }

        @Override
        public void writeExternal(Element element) {
            super.writeExternal(element);
            if (myConfigurationName != null && myConfigurationType != null) {
                element.setAttribute("run_configuration_name", myConfigurationName);
                element.setAttribute("run_configuration_type", myConfigurationType);
            }
            else if (mySettings != null) {
                element.setAttribute("run_configuration_name", mySettings.getName());
                element.setAttribute("run_configuration_type", mySettings.getType().getId());
            }
        }

        @Override
        public void readExternal(Element element) {
            super.readExternal(element);
            Attribute configurationNameAttr = element.getAttribute("run_configuration_name");
            Attribute configurationTypeAttr = element.getAttribute("run_configuration_type");
            myConfigurationName = configurationNameAttr != null ? configurationNameAttr.getValue() : null;
            myConfigurationType = configurationTypeAttr != null ? configurationTypeAttr.getValue() : null;
        }

        void init() {
            if (myInitialized) {
                return;
            }
            if (myConfigurationName != null && myConfigurationType != null) {
                for (RunnerAndConfigurationSettings runConfiguration : RunManagerImpl.getInstanceImpl(myProject).getSortedConfigurations()) {
                    ConfigurationType type = runConfiguration.getType();
                    if (myConfigurationName.equals(runConfiguration.getName())
                            && type != null
                            && myConfigurationType.equals(type.getId())) {
                        setSettings(runConfiguration);
                        return;
                    }
                }
            }
        }

        public void setSettings(RunnerAndConfigurationSettings settings) {
            mySettings = settings;
            myInitialized = true;
        }

        RunnerAndConfigurationSettings getSettings() {
            init();
            return mySettings;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            RunConfigurableBeforeRunTask that = (RunConfigurableBeforeRunTask)o;

            if (myConfigurationName != null ? !myConfigurationName.equals(that.myConfigurationName) : that.myConfigurationName != null) return false;
            if (myConfigurationType != null ? !myConfigurationType.equals(that.myConfigurationType) : that.myConfigurationType != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (myConfigurationName != null ? myConfigurationName.hashCode() : 0);
            result = 31 * result + (myConfigurationType != null ? myConfigurationType.hashCode() : 0);
            return result;
        }
    }

    private class SelectionDialog extends DialogWrapper {
        private RunnerAndConfigurationSettings mySelectedSettings;
         private final List<RunnerAndConfigurationSettings> mySettings;
        private JBList myJBList;

        private SelectionDialog(RunnerAndConfigurationSettings selectedSettings,  List<RunnerAndConfigurationSettings> settings) {
            super(myProject);
            setTitle(ExecutionBundle.message("before.launch.run.another.configuration.choose"));
            mySelectedSettings = selectedSettings;
            mySettings = settings;
            init();
            myJBList.setSelectedValue(mySelectedSettings, true);
            myJBList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() ==2) {
                        doOKAction();
                    }
                }
            });
            FontMetrics fontMetrics = myJBList.getFontMetrics(myJBList.getFont());
            int maxWidth = fontMetrics.stringWidth("m") * 30;
            for (RunnerAndConfigurationSettings setting : settings) {
                maxWidth = Math.max(fontMetrics.stringWidth(setting.getConfiguration().getName()), maxWidth);
            }
            maxWidth += 24;//icon and gap
            myJBList.setMinimumSize(new Dimension(maxWidth, myJBList.getPreferredSize().height));
        }

        
        @Override
        protected String getDimensionServiceKey() {
            return "com.intellij.execution.impl.RunConfigurationBeforeRunProvider.dimensionServiceKey;";
        }

        @Override
        protected JComponent createCenterPanel() {
            myJBList = new JBList(mySettings);
            myJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            myJBList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    Object selectedValue = myJBList.getSelectedValue();
                    if (selectedValue instanceof RunnerAndConfigurationSettings) {
                        mySelectedSettings = (RunnerAndConfigurationSettings)selectedValue;
                    }
                    else {
                        mySelectedSettings = null;
                    }
                    setOKActionEnabled(mySelectedSettings != null);
                }
            });
            myJBList.setCellRenderer(new ColoredListCellRenderer() {
                @Override
                protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
                    if (value instanceof RunnerAndConfigurationSettings) {
                        RunnerAndConfigurationSettings settings = (RunnerAndConfigurationSettings)value;
                        RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
                        setIcon(runManager.getConfigurationIcon(settings));
                        RunConfiguration configuration = settings.getConfiguration();
                        append(configuration.getName(), settings.isTemporary()
                                ? SimpleTextAttributes.GRAY_ATTRIBUTES
                                : SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    }
                }
            });
            return new JBScrollPane(myJBList);
        }

        
        RunnerAndConfigurationSettings getSelectedSettings() {
            return isOK() ? mySelectedSettings : null;
        }
    }
}
