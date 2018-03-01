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
package com.gome.maven.diff.util;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.util.containers.Convertor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Executor to perform <possibly> long operations on pooled thread
 * Is is used to reduce blinking, in case of fast end of background task.
 */
public class WaitingBackgroundableTaskExecutor {
    private static final Runnable TOO_SLOW_OPERATION = new EmptyRunnable();

    private int myModificationStamp = 0;
     private ProgressIndicator myProgressIndicator;

//    @CalledInAwt
    public int getModificationStamp() {
        return myModificationStamp;
    }

//    @CalledInAwt
    public void abort() {
        if (myProgressIndicator != null) {
            myProgressIndicator.cancel();
            myProgressIndicator = null;
            myModificationStamp++;
        }
    }

//    @CalledInAwt
    public void execute( final Convertor<ProgressIndicator, Runnable> backgroundTask,
                         final Runnable onSlowAction,
                        final int waitMillis) {
        execute(backgroundTask, onSlowAction, waitMillis, false);
    }

//    @CalledInAwt
    public void execute( final Convertor<ProgressIndicator, Runnable> backgroundTask,
                         final Runnable onSlowAction,
                        final int waitMillis,
                        final boolean forceEDT) {
        abort();

        myModificationStamp++;
        final int modificationStamp = myModificationStamp;

        final ModalityState modality = ModalityState.current();
        myProgressIndicator = new EmptyProgressIndicator() {
            
            @Override
            public ModalityState getModalityState() {
                return modality;
            }
        };
        final ProgressIndicator indicator = myProgressIndicator;

        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Runnable> resultRef = new AtomicReference<Runnable>();

        if (forceEDT) {
            Runnable result = backgroundTask.convert(indicator);
            finish(result, modificationStamp, indicator);
        }
        else {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    ProgressManager.getInstance().executeProcessUnderProgress(new Runnable() {
                        @Override
                        public void run() {
                            final Runnable result = backgroundTask.convert(indicator);

                            if (indicator.isCanceled()) {
                                semaphore.release();
                                return;
                            }

                            if (!resultRef.compareAndSet(null, result)) {
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish(result, modificationStamp, indicator);
                                    }
                                }, modality);
                            }
                            semaphore.release();
                        }
                    }, indicator);
                }
            });

            try {
                semaphore.tryAcquire(waitMillis, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ignore) {
            }
            if (!resultRef.compareAndSet(null, TOO_SLOW_OPERATION)) {
                // update presentation in the same thread to reduce blinking, caused by 'invokeLater' and fast background operation
                finish(resultRef.get(), modificationStamp, indicator);
            }
            else {
                if (onSlowAction != null) onSlowAction.run();
            }
        }
    }

//    @CalledInAwt
    private void finish( Runnable result, int modificationStamp,  ProgressIndicator indicator) {
        if (indicator.isCanceled()) return;
        if (myModificationStamp != modificationStamp) return;

        result.run();
    }
}
