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

import com.gome.maven.CommonBundle;
import com.gome.maven.execution.*;
import com.gome.maven.execution.configuration.CompatibilityAwareRunProfile;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.configurations.RunProfile;
import com.gome.maven.execution.configurations.RunProfileState;
import com.gome.maven.execution.process.ProcessAdapter;
import com.gome.maven.execution.process.ProcessEvent;
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.execution.runners.ExecutionEnvironmentBuilder;
import com.gome.maven.execution.runners.ExecutionUtil;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.execution.ui.RunContentManager;
import com.gome.maven.execution.ui.RunContentManagerImpl;
import com.gome.maven.ide.SaveAndSyncHandler;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.impl.SimpleDataContext;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Trinity;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.docking.DockManager;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class ExecutionManagerImpl extends ExecutionManager implements Disposable {
    public static final Key<Object> EXECUTION_SESSION_ID_KEY = Key.create("EXECUTION_SESSION_ID_KEY");
    public static final Key<Boolean> EXECUTION_SKIP_RUN = Key.create("EXECUTION_SKIP_RUN");

    private static final Logger LOG = Logger.getInstance(ExecutionManagerImpl.class);
    private static final ProcessHandler[] EMPTY_PROCESS_HANDLERS = new ProcessHandler[0];

    private final Project myProject;

    private RunContentManagerImpl myContentManager;
    private final Alarm awaitingTerminationAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final List<Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor>> myRunningConfigurations =
            ContainerUtil.createLockFreeCopyOnWriteList();
    private volatile boolean myForceCompilationInTests;

    protected ExecutionManagerImpl( Project project) {
        myProject = project;
    }

    @Override
    public void dispose() {
        for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
            Disposer.dispose(trinity.first);
        }
        myRunningConfigurations.clear();
    }

    
    @Override
    public RunContentManager getContentManager() {
        if (myContentManager == null) {
            myContentManager = new RunContentManagerImpl(myProject, DockManager.getInstance(myProject));
            Disposer.register(myProject, myContentManager);
        }
        return myContentManager;
    }

    
    @Override
    public ProcessHandler[] getRunningProcesses() {
        if (myContentManager == null) return EMPTY_PROCESS_HANDLERS;
        List<ProcessHandler> handlers = null;
        for (RunContentDescriptor descriptor : getContentManager().getAllDescriptors()) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler != null) {
                if (handlers == null) {
                    handlers = new SmartList<ProcessHandler>();
                }
                handlers.add(processHandler);
            }
        }
        return handlers == null ? EMPTY_PROCESS_HANDLERS : handlers.toArray(new ProcessHandler[handlers.size()]);
    }

    @Override
    public void compileAndRun( final Runnable startRunnable,
                               final ExecutionEnvironment environment,
                               final RunProfileState state,
                               final Runnable onCancelRunnable) {
        long id = environment.getExecutionId();
        if (id == 0) {
            id = environment.assignNewExecutionId();
        }

        RunProfile profile = environment.getRunProfile();
        if (!(profile instanceof RunConfiguration)) {
            startRunnable.run();
            return;
        }

        final RunConfiguration runConfiguration = (RunConfiguration)profile;
        final List<BeforeRunTask> beforeRunTasks = RunManagerEx.getInstanceEx(myProject).getBeforeRunTasks(runConfiguration);
        if (beforeRunTasks.isEmpty()) {
            startRunnable.run();
        }
        else {
            DataContext context = environment.getDataContext();
            final DataContext projectContext = context != null ? context : SimpleDataContext.getProjectContext(myProject);
            final long finalId = id;
            final Long executionSessionId = new Long(id);
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                /** @noinspection SSBasedInspection*/
                @Override
                public void run() {
                    for (BeforeRunTask task : beforeRunTasks) {
                        if (myProject.isDisposed()) {
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myProject, task.getProviderId());
                        if (provider == null) {
                            LOG.warn("Cannot find BeforeRunTaskProvider for id='" + task.getProviderId() + "'");
                            continue;
                        }
                        ExecutionEnvironment taskEnvironment = new ExecutionEnvironmentBuilder(environment).contentToReuse(null).build();
                        taskEnvironment.setExecutionId(finalId);
                        EXECUTION_SESSION_ID_KEY.set(taskEnvironment, executionSessionId);
                        if (!provider.executeTask(projectContext, runConfiguration, taskEnvironment, task)) {
                            if (onCancelRunnable != null) {
                                SwingUtilities.invokeLater(onCancelRunnable);
                            }
                            return;
                        }
                    }

                    doRun(environment, startRunnable);
                }
            });
        }
    }

    protected void doRun( final ExecutionEnvironment environment,  final Runnable startRunnable) {
        Boolean allowSkipRun = environment.getUserData(EXECUTION_SKIP_RUN);
        if (allowSkipRun != null && allowSkipRun) {
            environment.getProject().getMessageBus().syncPublisher(EXECUTION_TOPIC).processNotStarted(environment.getExecutor().getId(),
                    environment);
        }
        else {
            // important! Do not use DumbService.smartInvokeLater here because it depends on modality state
            // and execution of startRunnable could be skipped if modality state check fails
            //noinspection SSBasedInspection
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!myProject.isDisposed()) {
                        DumbService.getInstance(myProject).runWhenSmart(startRunnable);
                    }
                }
            });
        }
    }

    @Override
    public void startRunProfile( final RunProfileStarter starter,
                                 final RunProfileState state,
                                 final ExecutionEnvironment environment) {
        final Project project = environment.getProject();
        RunContentDescriptor reuseContent = getContentManager().getReuseContent(environment);
        if (reuseContent != null) {
            reuseContent.setExecutionId(environment.getExecutionId());
            environment.setContentToReuse(reuseContent);
        }

        final Executor executor = environment.getExecutor();
        project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processStartScheduled(executor.getId(), environment);

        Runnable startRunnable = new Runnable() {
            @Override
            public void run() {
                if (project.isDisposed()) {
                    return;
                }

                RunProfile profile = environment.getRunProfile();
                boolean started = false;
                try {
                    project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processStarting(executor.getId(), environment);

                    final RunContentDescriptor descriptor = starter.execute(project, executor, state, environment.getContentToReuse(), environment);
                    if (descriptor != null) {
                        environment.setContentToReuse(descriptor);
                        final Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity =
                                Trinity.create(descriptor, environment.getRunnerAndConfigurationSettings(), executor);
                        myRunningConfigurations.add(trinity);
                        Disposer.register(descriptor, new Disposable() {
                            @Override
                            public void dispose() {
                                myRunningConfigurations.remove(trinity);
                            }
                        });
                        getContentManager().showRunContent(executor, descriptor, environment.getContentToReuse());
                        final ProcessHandler processHandler = descriptor.getProcessHandler();
                        if (processHandler != null) {
                            if (!processHandler.isStartNotified()) {
                                processHandler.startNotify();
                            }
                            project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processStarted(executor.getId(), environment, processHandler);
                            started = true;
                            processHandler.addProcessListener(new ProcessExecutionListener(project, profile, processHandler));
                        }
                    }
                }
                catch (ExecutionException e) {
                    ExecutionUtil.handleExecutionError(project, executor.getToolWindowId(), profile, e);
                    LOG.info(e);
                }
                finally {
                    if (!started) {
                        project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processNotStarted(executor.getId(), environment);
                    }
                }
            }
        };

        if (ApplicationManager.getApplication().isUnitTestMode() && !myForceCompilationInTests) {
            startRunnable.run();
        }
        else {
            compileAndRun(startRunnable, environment, state, new Runnable() {
                @Override
                public void run() {
                    if (!project.isDisposed()) {
                        project.getMessageBus().syncPublisher(EXECUTION_TOPIC).processNotStarted(executor.getId(), environment);
                    }
                }
            });
        }
    }

    @Override
    public void restartRunProfile( Project project,
                                   Executor executor,
                                   ExecutionTarget target,
                                   RunnerAndConfigurationSettings configuration,
                                   ProcessHandler processHandler) {
        ExecutionEnvironmentBuilder builder = createEnvironmentBuilder(project, executor, configuration);
        if (processHandler != null) {
            for (RunContentDescriptor descriptor : getContentManager().getAllDescriptors()) {
                if (descriptor.getProcessHandler() == processHandler) {
                    builder.contentToReuse(descriptor);
                    break;
                }
            }
        }
        restartRunProfile(builder.target(target).build());
    }

    
    private static ExecutionEnvironmentBuilder createEnvironmentBuilder( Project project,  Executor executor,  RunnerAndConfigurationSettings configuration) {
        ExecutionEnvironmentBuilder builder = new ExecutionEnvironmentBuilder(project, executor);

        ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), configuration != null ? configuration.getConfiguration() : null);
        if (runner == null && configuration != null) {
            LOG.error("Cannot find runner for " + configuration.getName());
        }
        else if (runner != null) {
            assert configuration != null;
            builder.runnerAndSettings(runner, configuration);
        }
        return builder;
    }

    @Override
    public void restartRunProfile( Project project,
                                   Executor executor,
                                   ExecutionTarget target,
                                   RunnerAndConfigurationSettings configuration,
                                   RunContentDescriptor currentDescriptor) {
        ExecutionEnvironmentBuilder builder = createEnvironmentBuilder(project, executor, configuration);
        restartRunProfile(builder.target(target).contentToReuse(currentDescriptor).build());
    }

    @Override
    public void restartRunProfile( ProgramRunner runner,
                                   ExecutionEnvironment environment,
                                   RunContentDescriptor currentDescriptor) {
        ExecutionEnvironmentBuilder builder = new ExecutionEnvironmentBuilder(environment).contentToReuse(currentDescriptor);
        if (runner != null) {
            builder.runner(runner);
        }
        restartRunProfile(builder.build());
    }

    public static boolean isProcessRunning( RunContentDescriptor descriptor) {
        ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
        return processHandler != null && !processHandler.isProcessTerminated();
    }

    @Override
    public void restartRunProfile( final ExecutionEnvironment environment) {
        RunnerAndConfigurationSettings configuration = environment.getRunnerAndConfigurationSettings();

        List<RunContentDescriptor> runningIncompatible;
        if (configuration == null) {
            runningIncompatible = Collections.emptyList();
        }
        else {
            runningIncompatible = getIncompatibleRunningDescriptors(configuration);
        }

        RunContentDescriptor contentToReuse = environment.getContentToReuse();
        final List<RunContentDescriptor> runningOfTheSameType = new SmartList<RunContentDescriptor>();
        if (configuration != null && configuration.isSingleton()) {
            runningOfTheSameType.addAll(getRunningDescriptorsOfTheSameConfigType(configuration));
        }
        else if (isProcessRunning(contentToReuse)) {
            runningOfTheSameType.add(contentToReuse);
        }

        List<RunContentDescriptor> runningToStop = ContainerUtil.concat(runningOfTheSameType, runningIncompatible);
        if (!runningToStop.isEmpty()) {
            if (configuration != null) {
                if (!runningOfTheSameType.isEmpty()
                        && (runningOfTheSameType.size() > 1 || contentToReuse == null || runningOfTheSameType.get(0) != contentToReuse) &&
                        !userApprovesStopForSameTypeConfigurations(environment.getProject(), configuration.getName(), runningOfTheSameType.size())) {
                    return;
                }
                if (!runningIncompatible.isEmpty()
                        && !userApprovesStopForIncompatibleConfigurations(myProject, configuration.getName(), runningIncompatible)) {
                    return;
                }
            }

            for (RunContentDescriptor descriptor : runningToStop) {
                stop(descriptor);
            }
        }

        awaitingTerminationAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                if (DumbService.getInstance(myProject).isDumb() || ExecutorRegistry.getInstance().isStarting(environment)) {
                    awaitingTerminationAlarm.addRequest(this, 100);
                    return;
                }

                for (RunContentDescriptor descriptor : runningOfTheSameType) {
                    ProcessHandler processHandler = descriptor.getProcessHandler();
                    if (processHandler != null && !processHandler.isProcessTerminated()) {
                        awaitingTerminationAlarm.addRequest(this, 100);
                        return;
                    }
                }
                start(environment);
            }
        }, 50);
    }

    
    public void setForceCompilationInTests(boolean forceCompilationInTests) {
        myForceCompilationInTests = forceCompilationInTests;
    }

    private static void start( ExecutionEnvironment environment) {
        RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
        ProgramRunnerUtil.executeConfiguration(environment, settings != null && settings.isEditBeforeRun(), true);
    }

    private static boolean userApprovesStopForSameTypeConfigurations(Project project, String configName, int instancesCount) {
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
        final RunManagerConfig config = runManager.getConfig();
        if (!config.isRestartRequiresConfirmation()) return true;

        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return config.isRestartRequiresConfirmation();
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                config.setRestartRequiresConfirmation(value);
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return false;
            }

            
            @Override
            public String getDoNotShowMessage() {
                return CommonBundle.message("dialog.options.do.not.show");
            }
        };
        return Messages.showOkCancelDialog(
                project,
                ExecutionBundle.message("rerun.singleton.confirmation.message", configName, instancesCount),
                ExecutionBundle.message("process.is.running.dialog.title", configName),
                ExecutionBundle.message("rerun.confirmation.button.text"),
                CommonBundle.message("button.cancel"),
                Messages.getQuestionIcon(), option) == Messages.OK;
    }

    private static boolean userApprovesStopForIncompatibleConfigurations(Project project,
                                                                         String configName,
                                                                         List<RunContentDescriptor> runningIncompatibleDescriptors) {
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
        final RunManagerConfig config = runManager.getConfig();
        if (!config.isStopIncompatibleRequiresConfirmation()) return true;

        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return config.isStopIncompatibleRequiresConfirmation();
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                config.setStopIncompatibleRequiresConfirmation(value);
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return false;
            }

            
            @Override
            public String getDoNotShowMessage() {
                return CommonBundle.message("dialog.options.do.not.show");
            }
        };

        final StringBuilder names = new StringBuilder();
        for (final RunContentDescriptor descriptor : runningIncompatibleDescriptors) {
            String name = descriptor.getDisplayName();
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(StringUtil.isEmpty(name) ? ExecutionBundle.message("run.configuration.no.name")
                    : String.format("'%s'", name));
        }

        //noinspection DialogTitleCapitalization
        return Messages.showOkCancelDialog(
                project,
                ExecutionBundle.message("stop.incompatible.confirmation.message",
                        configName, names.toString(), runningIncompatibleDescriptors.size()),
                ExecutionBundle.message("incompatible.configuration.is.running.dialog.title", runningIncompatibleDescriptors.size()),
                ExecutionBundle.message("stop.incompatible.confirmation.button.text"),
                CommonBundle.message("button.cancel"),
                Messages.getQuestionIcon(), option) == Messages.OK;
    }

    
    private List<RunContentDescriptor> getRunningDescriptorsOfTheSameConfigType( final RunnerAndConfigurationSettings configurationAndSettings) {
        return getRunningDescriptors(new Condition<RunnerAndConfigurationSettings>() {
            @Override
            public boolean value( RunnerAndConfigurationSettings runningConfigurationAndSettings) {
                return configurationAndSettings == runningConfigurationAndSettings;
            }
        });
    }

    
    private List<RunContentDescriptor> getIncompatibleRunningDescriptors( RunnerAndConfigurationSettings configurationAndSettings) {
        final RunConfiguration configurationToCheckCompatibility = configurationAndSettings.getConfiguration();
        return getRunningDescriptors(new Condition<RunnerAndConfigurationSettings>() {
            @Override
            public boolean value( RunnerAndConfigurationSettings runningConfigurationAndSettings) {
                RunConfiguration runningConfiguration = runningConfigurationAndSettings == null ? null : runningConfigurationAndSettings.getConfiguration();
                if (runningConfiguration == null || !(runningConfiguration instanceof CompatibilityAwareRunProfile)) {
                    return false;
                }
                return ((CompatibilityAwareRunProfile)runningConfiguration).mustBeStoppedToRun(configurationToCheckCompatibility);
            }
        });
    }

    
    private List<RunContentDescriptor> getRunningDescriptors( Condition<RunnerAndConfigurationSettings> condition) {
        List<RunContentDescriptor> result = new SmartList<RunContentDescriptor>();
        for (Trinity<RunContentDescriptor, RunnerAndConfigurationSettings, Executor> trinity : myRunningConfigurations) {
            if (condition.value(trinity.getSecond())) {
                ProcessHandler processHandler = trinity.getFirst().getProcessHandler();
                if (processHandler != null && !processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
                    result.add(trinity.getFirst());
                }
            }
        }
        return result;
    }

    private static void stop( RunContentDescriptor descriptor) {
        ProcessHandler processHandler = descriptor != null ? descriptor.getProcessHandler() : null;
        if (processHandler == null) {
            return;
        }

        if (processHandler instanceof KillableProcess && processHandler.isProcessTerminating()) {
            ((KillableProcess)processHandler).killProcess();
            return;
        }

        if (!processHandler.isProcessTerminated()) {
            if (processHandler.detachIsDefault()) {
                processHandler.detachProcess();
            }
            else {
                processHandler.destroyProcess();
            }
        }
    }

    private static class ProcessExecutionListener extends ProcessAdapter {
        private final Project myProject;
        private final RunProfile myProfile;
        private final ProcessHandler myProcessHandler;

        public ProcessExecutionListener(Project project, RunProfile profile, ProcessHandler processHandler) {
            myProject = project;
            myProfile = profile;
            myProcessHandler = processHandler;
        }

        @Override
        public void processTerminated(ProcessEvent event) {
            if (myProject.isDisposed()) return;

            myProject.getMessageBus().syncPublisher(EXECUTION_TOPIC).processTerminated(myProfile, myProcessHandler);

            SaveAndSyncHandler.getInstance().scheduleRefresh();
        }

        @Override
        public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
            if (myProject.isDisposed()) return;

            myProject.getMessageBus().syncPublisher(EXECUTION_TOPIC).processTerminating(myProfile, myProcessHandler);
        }
    }
}
