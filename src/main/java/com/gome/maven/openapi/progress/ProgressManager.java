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
package com.gome.maven.openapi.progress;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.ThrowableComputable;


import javax.swing.*;

public abstract class ProgressManager extends ProgressIndicatorProvider {
    private static class ProgressManagerHolder {
        private static final ProgressManager ourInstance = ServiceManager.getService(ProgressManager.class);
    }

    
    public static ProgressManager getInstance() {
        return ProgressManagerHolder.ourInstance;
    }

    public abstract boolean hasProgressIndicator();
    public abstract boolean hasModalProgressIndicator();
    public abstract boolean hasUnsafeProgressIndicator();

    /**
     * Runs given process synchronously (in calling thread).
     */
    public abstract void runProcess( Runnable process, ProgressIndicator progress) throws ProcessCanceledException;

    /**
     * Runs given process synchronously (in calling thread).
     */
    public abstract <T> T runProcess( Computable<T> process, ProgressIndicator progress) throws ProcessCanceledException;

    @Override
    public ProgressIndicator getProgressIndicator() {
        return null;
    }

    public static void progress( String text) throws ProcessCanceledException {
        progress(text, "");
    }

    public static void progress2( final String text) throws ProcessCanceledException {
        final ProgressIndicator pi = getInstance().getProgressIndicator();
        if (pi != null) {
            pi.checkCanceled();
            pi.setText2(text);
        }
    }

    public static void progress( String text,  String text2) throws ProcessCanceledException {
        final ProgressIndicator pi = getInstance().getProgressIndicator();
        if (pi != null) {
            pi.checkCanceled();
            pi.setText(text);
            pi.setText2(text2 == null ? "" : text2);
        }
    }

    public abstract void executeNonCancelableSection( Runnable runnable);

    public abstract void setCancelButtonText(String cancelButtonText);


    /**
     * Runs the specified operation in a background thread and shows a modal progress dialog in the
     * main thread while the operation is executing.
     *
     * @param process       the operation to execute.
     * @param progressTitle the title of the progress window.
     * @param canBeCanceled whether "Cancel" button is shown on the progress window.
     * @param project       the project in the context of which the operation is executed.
     * @return true if the operation completed successfully, false if it was cancelled.
     */
    public abstract boolean runProcessWithProgressSynchronously( Runnable process,
                                                                  String progressTitle,
                                                                boolean canBeCanceled,
                                                                 Project project);

    /**
     * Runs the specified operation in a background thread and shows a modal progress dialog in the
     * main thread while the operation is executing.
     *
     * @param process       the operation to execute.
     * @param progressTitle the title of the progress window.
     * @param canBeCanceled whether "Cancel" button is shown on the progress window.
     * @param project       the project in the context of which the operation is executed.
     * @return true result of operation
     * @throws E exception thrown by process
     */
    public abstract <T, E extends Exception> T runProcessWithProgressSynchronously( ThrowableComputable<T, E> process,
                                                                                    String progressTitle,
                                                                                   boolean canBeCanceled,
                                                                                    Project project) throws E;

    /**
     * Runs the specified operation in a background thread and shows a modal progress dialog in the
     * main thread while the operation is executing.
     *
     * @param process         the operation to execute.
     * @param progressTitle   the title of the progress window.
     * @param canBeCanceled   whether "Cancel" button is shown on the progress window.
     * @param project         the project in the context of which the operation is executed.
     * @param parentComponent the component which will be used to calculate the progress window ancestor
     * @return true if the operation completed successfully, false if it was cancelled.
     */
    public abstract boolean runProcessWithProgressSynchronously( Runnable process,
                                                                  String progressTitle,
                                                                boolean canBeCanceled,
                                                                 Project project,
                                                                 JComponent parentComponent);

    /**
     * Runs a specified <code>process</code> in a background thread and shows a progress dialog, which can be made non-modal by pressing
     * background button. Upon successful termination of the process a <code>successRunnable</code> will be called in Swing UI thread and
     * <code>canceledRunnable</code> will be called if terminated on behalf of the user by pressing either cancel button, while running in
     * a modal state or stop button if running in background.
     *
     * @param project          the project in the context of which the operation is executed.
     * @param progressTitle    the title of the progress window.
     * @param process          the operation to execute.
     * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
     * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
     * @deprecated use {@link #run(Task)}
     */
    public abstract void runProcessWithProgressAsynchronously( Project project,
                                                                String progressTitle,
                                                               Runnable process,
                                                               Runnable successRunnable,
                                                               Runnable canceledRunnable);
    /**
     * Runs a specified <code>process</code> in a background thread and shows a progress dialog, which can be made non-modal by pressing
     * background button. Upon successful termination of the process a <code>successRunnable</code> will be called in Swing UI thread and
     * <code>canceledRunnable</code> will be called if terminated on behalf of the user by pressing either cancel button, while running in
     * a modal state or stop button if running in background.
     *
     * @param project          the project in the context of which the operation is executed.
     * @param progressTitle    the title of the progress window.
     * @param process          the operation to execute.
     * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
     * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
     * @param option           progress indicator behavior controller.
     * @deprecated use {@link #run(Task)}
     */
    public abstract void runProcessWithProgressAsynchronously( Project project,
                                                                String progressTitle,
                                                               Runnable process,
                                                               Runnable successRunnable,
                                                               Runnable canceledRunnable,
                                                               PerformInBackgroundOption option);

    /**
     * Runs a specified <code>task</code> in either background/foreground thread and shows a progress dialog.
     *
     * @param task task to run (either {@link Task.Modal}
     *             or {@link Task.Backgroundable}).
     */
    public abstract void run( Task task);

    public abstract void runProcessWithProgressAsynchronously( Task.Backgroundable task,  ProgressIndicator progressIndicator);

    protected void indicatorCanceled( ProgressIndicator indicator) {
    }

    public static void canceled( ProgressIndicator indicator) {
        getInstance().indicatorCanceled(indicator);
    }

    public static void checkCanceled() throws ProcessCanceledException {
        getInstance().doCheckCanceled();
    }

    public abstract void executeProcessUnderProgress( Runnable process,
                                                      ProgressIndicator progress)
            throws ProcessCanceledException;
}