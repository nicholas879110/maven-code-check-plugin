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
package com.gome.maven.execution.configurations;

import com.gome.maven.execution.*;
import com.gome.maven.execution.executors.DefaultRunExecutor;
import com.gome.maven.execution.filters.Filter;
import com.gome.maven.execution.filters.TextConsoleBuilder;
import com.gome.maven.execution.filters.TextConsoleBuilderFactory;
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.execution.ui.ConsoleView;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * Base implementation of {@link RunProfileState}. Takes care of putting together a process and a console and wrapping them into an
 * {@link ExecutionResult}. Does not contain any logic for actually starting the process.
 *
 * @see com.gome.maven.execution.configurations.JavaCommandLineState
 * @see GeneralCommandLine
 */
public abstract class CommandLineState implements RunProfileState {
    private TextConsoleBuilder myConsoleBuilder;

    private final ExecutionEnvironment myEnvironment;

    protected CommandLineState(ExecutionEnvironment environment) {
        myEnvironment = environment;
        if (myEnvironment != null) {
            final Project project = myEnvironment.getProject();
            final GlobalSearchScope searchScope = SearchScopeProvider.createSearchScope(project, myEnvironment.getRunProfile());
            myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope);
        }
    }

    public ExecutionEnvironment getEnvironment() {
        return myEnvironment;
    }

    public RunnerSettings getRunnerSettings() {
        return myEnvironment.getRunnerSettings();
    }

    
    public ExecutionTarget getExecutionTarget() {
        return myEnvironment.getExecutionTarget();
    }

    public void addConsoleFilters(Filter... filters) {
        myConsoleBuilder.filters(filters);
    }

    @Override
    
    public ExecutionResult execute( final Executor executor,  final ProgramRunner runner) throws ExecutionException {
        final ProcessHandler processHandler = startProcess();
        final ConsoleView console = createConsole(executor);
        if (console != null) {
            console.attachToProcess(processHandler);
        }
        return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
    }

    
    protected ConsoleView createConsole( final Executor executor) throws ExecutionException {
        TextConsoleBuilder builder = getConsoleBuilder();
        return builder != null ? builder.getConsole() : null;
    }

    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     * @see com.gome.maven.execution.process.OSProcessHandler
     */
    
    protected abstract ProcessHandler startProcess() throws ExecutionException;

    protected AnAction[] createActions(final ConsoleView console, final ProcessHandler processHandler) {
        return createActions(console, processHandler, null);
    }

    protected AnAction[] createActions(final ConsoleView console, final ProcessHandler processHandler, Executor executor) {
        if (console == null || !console.canPause() || (executor != null && !DefaultRunExecutor.EXECUTOR_ID.equals(executor.getId()))) {
            return new AnAction[0];
        }
        return new AnAction[]{new PauseOutputAction(console, processHandler)};
    }

    public TextConsoleBuilder getConsoleBuilder() {
        return myConsoleBuilder;
    }

    public void setConsoleBuilder(final TextConsoleBuilder consoleBuilder) {
        myConsoleBuilder = consoleBuilder;
    }

    protected static class PauseOutputAction extends ToggleAction implements DumbAware{
        private final ConsoleView myConsole;
        private final ProcessHandler myProcessHandler;

        public PauseOutputAction(final ConsoleView console, final ProcessHandler processHandler) {
            super(ExecutionBundle.message("run.configuration.pause.output.action.name"), null, AllIcons.Actions.Pause);
            myConsole = console;
            myProcessHandler = processHandler;
        }

        @Override
        public boolean isSelected(final AnActionEvent event) {
            return myConsole.isOutputPaused();
        }

        @Override
        public void setSelected(final AnActionEvent event, final boolean flag) {
            myConsole.setOutputPaused(flag);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    update(event);
                }
            });
        }

        @Override
        public void update(final AnActionEvent event) {
            super.update(event);
            final Presentation presentation = event.getPresentation();
            final boolean isRunning = myProcessHandler != null && !myProcessHandler.isProcessTerminated();
            if (isRunning) {
                presentation.setEnabled(true);
            }
            else {
                if (!myConsole.canPause()) {
                    presentation.setEnabled(false);
                    return;
                }
                if (!myConsole.hasDeferredOutput()) {
                    presentation.setEnabled(false);
                }
                else {
                    presentation.setEnabled(true);
                    myConsole.performWhenNoDeferredOutput(new Runnable() {
                        @Override
                        public void run() {
                            update(event);
                        }
                    });
                }
            }
        }
    }
}
