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

package com.gome.maven.execution.actions;

import com.gome.maven.execution.*;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.runners.ExecutionUtil;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.util.Pair;

public class RunContextAction extends BaseRunConfigurationAction {
    private final Executor myExecutor;

    public RunContextAction( final Executor executor) {
        super(ExecutionBundle.message("perform.action.with.context.configuration.action.name", executor.getStartActionText()), null,
                executor.getIcon());
        myExecutor = executor;
    }

    @Override
    protected void perform(final ConfigurationContext context) {
        RunnerAndConfigurationSettings configuration = context.findExisting();
        final RunManagerEx runManager = (RunManagerEx)context.getRunManager();
        if (configuration == null) {
            configuration = context.getConfiguration();
            if (configuration == null) {
                return;
            }
            runManager.setTemporaryConfiguration(configuration);
        }
        runManager.setSelectedConfiguration(configuration);

        ExecutionUtil.runConfiguration(configuration, myExecutor);
    }

    @Override
    protected boolean isEnabledFor(RunConfiguration configuration) {
        return getRunner(configuration) != null;
    }

    
    private ProgramRunner getRunner(final RunConfiguration configuration) {
        return RunnerRegistry.getInstance().getRunner(myExecutor.getId(), configuration);
    }

    @Override
    protected void updatePresentation(final Presentation presentation,  final String actionText, final ConfigurationContext context) {
        presentation.setText(myExecutor.getStartActionText(actionText), true);

        Pair<Boolean, Boolean> b = isEnabledAndVisible(context);

        presentation.setEnabled(b.first);
        presentation.setVisible(b.second);
    }

    private Pair<Boolean, Boolean> isEnabledAndVisible(ConfigurationContext context) {
        RunnerAndConfigurationSettings configuration = context.findExisting();
        if (configuration == null) {
            configuration = context.getConfiguration();
        }

        ProgramRunner runner = configuration == null ? null : getRunner(configuration.getConfiguration());
        if (runner == null) {
            return Pair.create(false, false);
        }
        return Pair.create(!ExecutorRegistry.getInstance().isStarting(context.getProject(), myExecutor.getId(), runner.getRunnerId()), true);
    }
}
