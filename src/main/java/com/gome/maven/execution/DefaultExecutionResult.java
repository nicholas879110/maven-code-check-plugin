/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.ui.ExecutionConsole;
import com.gome.maven.openapi.actionSystem.AnAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dyoma
 */
public class DefaultExecutionResult implements ExecutionResult {
    private final ExecutionConsole myConsole;
    private final ProcessHandler myProcessHandler;
    private AnAction[] myActions;
    
    private AnAction[] myRestartActions = AnAction.EMPTY_ARRAY;
    private final List<AnAction> myStopActions = new ArrayList<AnAction>();

    public DefaultExecutionResult() {
        myConsole = null;
        myProcessHandler = null;
        myActions = AnAction.EMPTY_ARRAY;
    }

    public DefaultExecutionResult( ExecutionConsole console,  final ProcessHandler processHandler) {
        this(console, processHandler, AnAction.EMPTY_ARRAY);
    }

    public DefaultExecutionResult(final ExecutionConsole console,  final ProcessHandler processHandler, final AnAction... actions) {
        myConsole = console;
        myProcessHandler = processHandler;
        myActions = actions;
    }

    @Override
    public ExecutionConsole getExecutionConsole() {
        return myConsole;
    }

    @Override
    public AnAction[] getActions() {
        return myActions;
    }

    public void setActions( final AnAction... actions) {
        myActions = actions;
    }

    
    public AnAction[] getRestartActions() {
        return myRestartActions;
    }

    // TODO: Find all usages, make sure there is no null and make this method NotNull
    public void setRestartActions( AnAction... restartActions) {
        myRestartActions = (restartActions != null ? restartActions : AnAction.EMPTY_ARRAY);
    }

    public void addStopAction(AnAction action) {
        myStopActions.add(action);
    }

    
    public AnAction[] getAdditionalStopActions() {
        return myStopActions.toArray(new AnAction[myStopActions.size()]);
    }

    @Override
    public ProcessHandler getProcessHandler() {
        return myProcessHandler;
    }
}