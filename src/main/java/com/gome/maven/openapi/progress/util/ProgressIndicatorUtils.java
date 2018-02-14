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
package com.gome.maven.openapi.progress.util;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationAdapter;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.util.Ref;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.swing.*;
import java.util.concurrent.Executor;

/**
 * Methods in this class are used to equip long background processes which take read actions with a special listener
 * that fires when a write action is about to begin, and cancels corresponding progress indicators to avoid blocking the UI.
 * These processes should be ready to get {@link com.gome.maven.openapi.progress.ProcessCanceledException} at any moment.
 * Processes may want to react on cancellation event by restarting the activity, see
 * {@link com.gome.maven.openapi.progress.util.ReadTask#onCanceled(com.gome.maven.openapi.progress.ProgressIndicator)} for that.
 *
 * @author gregsh
 */
public class ProgressIndicatorUtils {
    private ProgressIndicatorUtils() {
    }

    
    public static ProgressIndicator forceWriteActionPriority( final ProgressIndicator progress,  final Disposable builder) {
        ApplicationManager.getApplication().addApplicationListener(new ApplicationAdapter() {
            @Override
            public void beforeWriteActionStart(Object action) {
                if (progress.isRunning()) {
                    progress.cancel();
                }
            }
        }, builder);
        return progress;
    }

    public static void scheduleWithWriteActionPriority( ReadTask task) {
        scheduleWithWriteActionPriority(new ProgressIndicatorBase(), task);
    }

    public static void scheduleWithWriteActionPriority( ProgressIndicator progressIndicator,  ReadTask readTask) {
        scheduleWithWriteActionPriority(progressIndicator, PooledThreadExecutor.INSTANCE, readTask);
    }

    public static boolean runWithWriteActionPriority( final Runnable action) {
        return runWithWriteActionPriority(action, new ProgressIndicatorBase());
    }

    public static boolean runWithWriteActionPriority( final Runnable action,
                                                      final ProgressIndicator progressIndicator) {
        final ApplicationEx application = (ApplicationEx)ApplicationManager.getApplication();

        if (application.isWriteActionPending()) {
            // first catch: check if write action acquisition started: especially important when current thread has read action, because
            // tryRunReadAction below would just run without really checking if a write action is pending
            if (!progressIndicator.isCanceled()) progressIndicator.cancel();
            return false;
        }

        final ApplicationAdapter listener = new ApplicationAdapter() {
            @Override
            public void beforeWriteActionStart(Object action) {
                if (!progressIndicator.isCanceled()) progressIndicator.cancel();
            }
        };

        boolean succeededWithAddingListener = application.tryRunReadAction(new Runnable() {
            @Override
            public void run() {
                // Even if writeLock.lock() acquisition is in progress at this point then runProcess will block wanting read action which is
                // also ok as last resort.
                application.addApplicationListener(listener);
            }
        });
        if (!succeededWithAddingListener) { // second catch: writeLock.lock() acquisition is in progress or already acquired
            if (!progressIndicator.isCanceled()) progressIndicator.cancel();
            return false;
        }
        final Ref<Boolean> wasCancelled = new Ref<Boolean>();
        try {
            ProgressManager.getInstance().runProcess(new Runnable() {
                @Override
                public void run() {
                    try {
                        action.run();
                    }
                    catch (ProcessCanceledException ignore) {
                        wasCancelled.set(Boolean.TRUE);
                    }
                }
            }, progressIndicator);
        }
        finally {
            application.removeApplicationListener(listener);
        }
        return wasCancelled.get() != Boolean.TRUE;
    }

    public static void scheduleWithWriteActionPriority( final ProgressIndicator progressIndicator,
                                                        final Executor executor,
                                                        final ReadTask readTask) {
        final Application application = ApplicationManager.getApplication();
        // invoke later even if on EDT
        // to avoid tasks eagerly restarting immediately, allocating many pooled threads
        // which get cancelled too soon when a next write action arrives in the same EDT batch
        // (can happen when processing multiple VFS events or writing multiple files on save)

        // use SwingUtilities instead of application.invokeLater
        // to tolerate any immediate modality changes (e.g. https://youtrack.jetbrains.com/issue/IDEA-135180)

        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (application.isDisposed()) return;
                application.assertIsDispatchThread();
                final ApplicationAdapter listener = new ApplicationAdapter() {
                    @Override
                    public void beforeWriteActionStart(Object action) {
                        if (!progressIndicator.isCanceled()) {
                            progressIndicator.cancel();
                            readTask.onCanceled(progressIndicator);
                        }
                    }
                };
                application.addApplicationListener(listener);
                try {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                runUnderProgress(progressIndicator, readTask);
                            }
                            finally {
                                application.removeApplicationListener(listener);
                            }
                        }
                    });
                }
                catch (RuntimeException e) {
                    application.removeApplicationListener(listener);
                    throw e;
                }
                catch (Error e) {
                    application.removeApplicationListener(listener);
                    throw e;
                }
            }
        });
    }

    private static void runUnderProgress( final ProgressIndicator progressIndicator,  final ReadTask task) {
        ProgressManager.getInstance().runProcess(new Runnable() {
            @Override
            public void run() {
                try {
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            task.computeInReadAction(progressIndicator);
                        }
                    });
                }
                catch (ProcessCanceledException ignore) {
                }
            }
        }, progressIndicator);
    }
}
