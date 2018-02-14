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
package com.gome.maven.execution.ui;

import com.gome.maven.execution.Executor;
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.util.messages.Topic;

import java.util.List;

/**
 * The manager of tabs in the Run/Debug toolwindows.
 *
 * @see com.gome.maven.execution.ExecutionManager#getContentManager()
 */
public interface RunContentManager {
    Topic<RunContentWithExecutorListener> TOPIC =
            Topic.create("Run Content", RunContentWithExecutorListener.class);

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * @deprecated Use {@link LangDataKeys#RUN_CONTENT_DESCRIPTOR} instead
     */
            DataKey<RunContentDescriptor> RUN_CONTENT_DESCRIPTOR = LangDataKeys.RUN_CONTENT_DESCRIPTOR;

    /**
     * Returns the content descriptor for the selected run configuration in the last activated Run/Debug toolwindow.
     *
     * @return the content descriptor, or null if there are no active run or debug configurations.
     */
    
    RunContentDescriptor getSelectedContent();

    /**
     * Returns the content descriptor for the selected run configuration in the toolwindow corresponding to the specified executor.
     *
     * @param executor the executor (e.g. {@link com.gome.maven.execution.executors.DefaultRunExecutor#getRunExecutorInstance()} or
     *                 {@link com.gome.maven.execution.executors.DefaultDebugExecutor#getDebugExecutorInstance()})
     * @return the content descriptor, or null if there is no selected run configuration in the specified toolwindow.
     */
    
    RunContentDescriptor getSelectedContent(Executor executor);

    /**
     * Returns the list of content descriptors for all currently displayed run/debug configurations.
     */
    
    List<RunContentDescriptor> getAllDescriptors();

    /**
     * @deprecated use {@link #getReuseContent(ExecutionEnvironment)}
     * to remove in IDEA 15
     */
    @Deprecated
    
    RunContentDescriptor getReuseContent(Executor requestor,  RunContentDescriptor contentToReuse);

    /**
     * @deprecated use {@link #getReuseContent(ExecutionEnvironment)}
     * to remove in IDEA 15
     */
    @Deprecated
    
    RunContentDescriptor getReuseContent(Executor requestor,  ExecutionEnvironment executionEnvironment);

    
    /**
     * To reduce number of open contents RunContentManager reuses
     * some of them during showRunContent (for ex. if a process was stopped)
     */
    RunContentDescriptor getReuseContent( ExecutionEnvironment executionEnvironment);

    /**
     * @deprecated use {@link #getReuseContent(ExecutionEnvironment)}
     * to remove in IDEA 15
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    
    RunContentDescriptor getReuseContent(Executor requestor, DataContext dataContext);

    
    RunContentDescriptor findContentDescriptor(Executor requestor, ProcessHandler handler);

    void showRunContent( Executor executor,  RunContentDescriptor descriptor,  RunContentDescriptor contentToReuse);

    void showRunContent( Executor executor,  RunContentDescriptor descriptor);

    void hideRunContent( Executor executor, RunContentDescriptor descriptor);

    boolean removeRunContent( Executor executor, RunContentDescriptor descriptor);

    void toFrontRunContent(Executor requestor, RunContentDescriptor descriptor);

    void toFrontRunContent(Executor requestor, ProcessHandler handler);

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * @deprecated Use {@link RunContentManager#TOPIC} instead
     * to remove in IDEA 15
     */
    void addRunContentListener( RunContentListener listener);

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * @deprecated Use {@link RunContentManager#TOPIC} instead
     * to remove in IDEA 15
     */
    void removeRunContentListener(RunContentListener listener);

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * @deprecated Use {@link RunContentManager#TOPIC} instead
     * to remove in IDEA 15
     */
    void addRunContentListener( RunContentListener myContentListener, Executor executor);

    
    ToolWindow getToolWindowByDescriptor( RunContentDescriptor descriptor);
}
