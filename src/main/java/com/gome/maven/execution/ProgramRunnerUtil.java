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
package com.gome.maven.execution;

import com.gome.maven.execution.configurations.ConfigurationFactory;
import com.gome.maven.execution.configurations.ConfigurationType;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.impl.RunDialog;
import com.gome.maven.execution.impl.RunManagerImpl;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.execution.runners.ExecutionEnvironmentBuilder;
import com.gome.maven.execution.runners.ExecutionUtil;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.internal.statistic.UsageTrigger;
import com.gome.maven.internal.statistic.beans.ConvertUsagesUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.IconLoader;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.LayeredIcon;

import javax.swing.*;

public class ProgramRunnerUtil {
    private static final Logger LOG = Logger.getInstance(ProgramRunnerUtil.class);

    private ProgramRunnerUtil() {
    }

    
    public static ProgramRunner getRunner( final String executorId, final RunnerAndConfigurationSettings configuration) {
        return configuration == null ? null : RunnerRegistry.getInstance().getRunner(executorId, configuration.getConfiguration());
    }

    public static void executeConfiguration( ExecutionEnvironment environment, boolean showSettings, boolean assignNewId) {
        if (ExecutorRegistry.getInstance().isStarting(environment)) {
            return;
        }

        RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
        if (runnerAndConfigurationSettings != null) {
            if (!ExecutionTargetManager.canRun(environment)) {
                ExecutionUtil.handleExecutionError(environment, new ExecutionException(
                        StringUtil.escapeXml("Cannot run '" + environment.getRunProfile().getName() + "' on '" + environment.getExecutionTarget().getDisplayName() + "'")));
                return;
            }

            if (!RunManagerImpl.canRunConfiguration(environment) || (showSettings && runnerAndConfigurationSettings.isEditBeforeRun())) {
                if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
                    return;
                }

                while (!RunManagerImpl.canRunConfiguration(environment)) {
                    if (Messages.YES == Messages
                            .showYesNoDialog(environment.getProject(), "Configuration is still incorrect. Do you want to edit it again?", "Change Configuration Settings",
                                    "Edit", "Continue Anyway", Messages.getErrorIcon())) {
                        if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
                            return;
                        }
                    }
                    else {
                        break;
                    }
                }
            }

            ConfigurationType configurationType = runnerAndConfigurationSettings.getType();
            if (configurationType != null) {
                UsageTrigger.trigger("execute." + ConvertUsagesUtil.ensureProperKey(configurationType.getId()) + "." + environment.getExecutor().getId());
            }
        }

        try {
            if (assignNewId) {
                environment.assignNewExecutionId();
            }
            environment.getRunner().execute(environment);
        }
        catch (ExecutionException e) {
            String name = runnerAndConfigurationSettings != null ? runnerAndConfigurationSettings.getName() : null;
            if (name == null) {
                name = environment.getRunProfile().getName();
            }
            if (name == null && environment.getContentToReuse() != null) {
                name = environment.getContentToReuse().getDisplayName();
            }
            if (name == null) {
                name = "<Unknown>";
            }
            ExecutionUtil.handleExecutionError(environment.getProject(), environment.getExecutor().getToolWindowId(), name, e);
        }
    }

    public static void executeConfiguration( Project project,
                                             RunnerAndConfigurationSettings configuration,
                                             Executor executor) {
        ExecutionEnvironmentBuilder builder;
        try {
            builder = ExecutionEnvironmentBuilder.create(executor, configuration);
        }
        catch (ExecutionException e) {
            LOG.error(e);
            return;
        }

        executeConfiguration(builder
                .contentToReuse(null)
                .dataContext(null)
                .activeTarget()
                .build(), true, true);
    }

    public static Icon getConfigurationIcon(final RunnerAndConfigurationSettings settings,
                                            final boolean invalid) {
        RunConfiguration configuration = settings.getConfiguration();
        ConfigurationFactory factory = settings.getFactory();
        Icon icon =  factory != null ? factory.getIcon(configuration) : null;
        if (icon == null) icon = AllIcons.RunConfigurations.Unknown;

        final Icon configurationIcon = settings.isTemporary() ? IconLoader.getTransparentIcon(icon, 0.3f) : icon;
        if (invalid) {
            return LayeredIcon.create(configurationIcon, AllIcons.RunConfigurations.InvalidConfigurationLayer);
        }

        return configurationIcon;
    }

    public static String shortenName( String name, final int toBeAdded) {
        if (name == null) {
            return "";
        }

        final int symbols = Math.max(10, 20 - toBeAdded);
        return name.length() < symbols ? name : name.substring(0, symbols) + "...";
    }
}
