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

package com.gome.maven.execution.runners;

import com.gome.maven.execution.*;
import com.gome.maven.execution.configurations.RunProfile;
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.process.ProcessNotCreatedException;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationGroup;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.ui.content.Content;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class ExecutionUtil {
    private static final Logger LOG = Logger.getInstance("com.gome.maven.execution.runners.ExecutionUtil");

    private static final NotificationGroup ourNotificationGroup = NotificationGroup.logOnlyGroup("Execution");

    private ExecutionUtil() {
    }

    public static void handleExecutionError( Project project,
                                             String toolWindowId,
                                             RunProfile runProfile,
                                             ExecutionException e) {
        handleExecutionError(project, toolWindowId, runProfile.getName(), e);
    }

    public static void handleExecutionError( ExecutionEnvironment environment,  ExecutionException e) {
        handleExecutionError(environment.getProject(), environment.getExecutor().getToolWindowId(), environment.getRunProfile().getName(), e);
    }

    public static void handleExecutionError( final Project project,
                                             final String toolWindowId,
                                             String taskName,
                                             ExecutionException e) {
        if (e instanceof RunCanceledByUserException) {
            return;
        }

        LOG.debug(e);

        String description = e.getMessage();
        if (description == null) {
            LOG.warn("Execution error without description", e);
            description = "Unknown error";
        }

        HyperlinkListener listener = null;
        if ((description.contains("87") || description.contains("111") || description.contains("206")) &&
                e instanceof ProcessNotCreatedException &&
                !PropertiesComponent.getInstance(project).isTrueValue("dynamic.classpath")) {
            final String commandLineString = ((ProcessNotCreatedException)e).getCommandLine().getCommandLineString();
            if (commandLineString.length() > 1024 * 32) {
                description = "Command line is too long. In order to reduce its length classpath file can be used.<br>" +
                        "Would you like to enable classpath file mode for all run configurations of your project?<br>" +
                        "<a href=\"\">Enable</a>";

                listener = new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent event) {
                        PropertiesComponent.getInstance(project).setValue("dynamic.classpath", "true");
                    }
                };
            }
        }
        final String title = ExecutionBundle.message("error.running.configuration.message", taskName);
        final String fullMessage = title + ":<br>" + description;

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            LOG.error(fullMessage, e);
        }

        if (listener == null && e instanceof HyperlinkListener) {
            listener = (HyperlinkListener)e;
        }

        final HyperlinkListener finalListener = listener;
        final String finalDescription = description;
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (project.isDisposed()) {
                    return;
                }

                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                if (toolWindowManager.canShowNotification(toolWindowId)) {
                    //noinspection SSBasedInspection
                    toolWindowManager.notifyByBalloon(toolWindowId, MessageType.ERROR, fullMessage, null, finalListener);
                }
                else {
                    Messages.showErrorDialog(project, UIUtil.toHtml(fullMessage), "");
                }
                NotificationListener notificationListener = finalListener == null ? null : new NotificationListener() {
                    @Override
                    public void hyperlinkUpdate( Notification notification,  HyperlinkEvent event) {
                        finalListener.hyperlinkUpdate(event);
                    }
                };
                ourNotificationGroup.createNotification(title, finalDescription, NotificationType.ERROR, notificationListener).notify(project);
            }
        });
    }

    public static void restartIfActive( RunContentDescriptor descriptor) {
        ProcessHandler processHandler = descriptor.getProcessHandler();
        if (processHandler != null
                && processHandler.isStartNotified()
                && !processHandler.isProcessTerminating()
                && !processHandler.isProcessTerminated()) {
            restart(descriptor);
        }
    }

    public static void restart( RunContentDescriptor descriptor) {
        restart(descriptor.getComponent());
    }

    public static void restart( Content content) {
        restart(content.getComponent());
    }

    private static void restart( JComponent component) {
        if (component != null) {
            ExecutionEnvironment environment = LangDataKeys.EXECUTION_ENVIRONMENT.getData(DataManager.getInstance().getDataContext(component));
            if (environment != null) {
                restart(environment);
            }
        }
    }

    public static void restart( ExecutionEnvironment environment) {
        if (!ExecutorRegistry.getInstance().isStarting(environment)) {
            ExecutionManager.getInstance(environment.getProject()).restartRunProfile(environment);
        }
    }

    public static void runConfiguration( RunnerAndConfigurationSettings configuration,  Executor executor) {
        ExecutionEnvironmentBuilder builder = createEnvironment(executor, configuration);
        if (builder != null) {
            ExecutionManager.getInstance(configuration.getConfiguration().getProject()).restartRunProfile(builder
                    .activeTarget()
                    .build());
        }
    }

    
    public static ExecutionEnvironmentBuilder createEnvironment( Executor executor,  RunnerAndConfigurationSettings settings) {
        try {
            return ExecutionEnvironmentBuilder.create(executor, settings);
        }
        catch (ExecutionException e) {
            handleExecutionError(settings.getConfiguration().getProject(), executor.getToolWindowId(), settings.getConfiguration().getName(), e);
            return null;
        }
    }
}
