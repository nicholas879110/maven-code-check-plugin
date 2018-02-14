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
package com.gome.maven.openapi.application.ex;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;
import java.io.IOException;

/**
 * @author max
 */
public interface ApplicationEx extends Application {
    String LOCATOR_FILE_NAME = ".home";

    /**
     * Loads the application configuration from the specified path
     *
     * @param optionsPath Path to /config folder
     * @throws IOException
     */
    void load( String optionsPath) throws IOException;

    boolean isLoaded();

    
    String getName();

    /**
     * @return true if this thread is inside read action.
     * @see #runReadAction(Runnable)
     */
    boolean holdsReadLock();

    /**
     * @return true if the EDT is performing write action right now.
     * @see #runWriteAction(Runnable)
     */
    boolean isWriteActionInProgress();

    /**
     * @return true if the EDT started to acquire write action but has not acquired it yet.
     * @see #runWriteAction(Runnable)
     */
    boolean isWriteActionPending();

    void doNotSave();
    void doNotSave(boolean value);
    boolean isDoNotSave();

    /**
     * @param force if true, no additional confirmations will be shown. The application is guaranteed to exit
     * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
     *                      a corresponding confirmation will be shown with the possibility to cancel the operation
     */
    void exit(boolean force, boolean exitConfirmed);

    /**
     * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
     *                      a corresponding confirmation will be shown with the possibility to cancel the operation
     */
    void restart(boolean exitConfirmed);

    /**
     * Runs modal process. For internal use only, see {@link Task}
     */
    boolean runProcessWithProgressSynchronously( Runnable process,
                                                 String progressTitle,
                                                boolean canBeCanceled,
                                                Project project);

    /**
     * Runs modal process. For internal use only, see {@link Task}
     */
    boolean runProcessWithProgressSynchronously( Runnable process,
                                                 String progressTitle,
                                                boolean canBeCanceled,
                                                 Project project,
                                                JComponent parentComponent);

    /**
     * Runs modal process. For internal use only, see {@link Task}
     */
    boolean runProcessWithProgressSynchronously( Runnable process,
                                                 String progressTitle,
                                                boolean canBeCanceled,
                                                 Project project,
                                                JComponent parentComponent,
                                                final String cancelText);

    void assertIsDispatchThread( JComponent component);

    void assertTimeConsuming();

    void runEdtSafeAction( Runnable runnable);

    /**
     * Grab the lock and run the action, in a non-blocking fashion
     *
     * @return true if action was run while holding the lock, false if was unable to get the lock and action was not run
     */
    boolean tryRunReadAction( Runnable action);
}
