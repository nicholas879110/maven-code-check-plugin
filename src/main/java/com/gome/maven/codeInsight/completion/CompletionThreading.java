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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.util.ProgressWrapper;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.concurrency.FutureResult;
import com.gome.maven.util.concurrency.Semaphore;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author peter
 */
interface CompletionThreading {

    Future<?> startThread(final ProgressIndicator progressIndicator, Runnable runnable);

    WeighingDelegate delegateWeighing(CompletionProgressIndicator indicator);
}

interface WeighingDelegate extends Consumer<CompletionResult> {
    void waitFor();
}

class SyncCompletion implements CompletionThreading {

    @Override
    public Future<?> startThread(final ProgressIndicator progressIndicator, Runnable runnable) {
        ProgressManager.getInstance().runProcess(runnable, progressIndicator);

        FutureResult<Object> result = new FutureResult<Object>();
        result.set(true);
        return result;
    }

    @Override
    public WeighingDelegate delegateWeighing(final CompletionProgressIndicator indicator) {
        return new WeighingDelegate() {
            @Override
            public void waitFor() {
            }

            @Override
            public void consume(CompletionResult result) {
                indicator.addItem(result);
            }
        };
    }
}

class AsyncCompletion implements CompletionThreading {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.completion.AsyncCompletion");

    @Override
    public Future<?> startThread(final ProgressIndicator progressIndicator, final Runnable runnable) {
        final Semaphore startSemaphore = new Semaphore();
        startSemaphore.down();
        Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().runProcess(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ApplicationManager.getApplication().runReadAction(new Runnable() {
                                @Override
                                public void run() {
                                    startSemaphore.up();
                                    ProgressManager.checkCanceled();
                                    runnable.run();
                                }
                            });
                        }
                        catch (ProcessCanceledException ignored) {
                        }
                    }
                }, progressIndicator);
            }
        });
        startSemaphore.waitFor();
        return future;
    }

    @Override
    public WeighingDelegate delegateWeighing(final CompletionProgressIndicator indicator) {
        final LinkedBlockingQueue<Computable<Boolean>> queue = new LinkedBlockingQueue<Computable<Boolean>>();

        class WeighItems implements Runnable {
            @Override
            public void run() {
                try {
                    while (true) {
                        Computable<Boolean> next = queue.poll(30, TimeUnit.MILLISECONDS);
                        if (next != null && !next.compute()) {
                            return;
                        }
                        indicator.checkCanceled();
                    }
                }
                catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        }

        final Future<?> future = startThread(ProgressWrapper.wrap(indicator), new WeighItems());
        return new WeighingDelegate() {
            @Override
            public void waitFor() {
                queue.offer(new Computable.PredefinedValueComputable<Boolean>(false));
                try {
                    future.get();
                }
                catch (InterruptedException e) {
                    LOG.error(e);
                }
                catch (ExecutionException e) {
                    LOG.error(e);
                }
            }

            @Override
            public void consume(final CompletionResult result) {
                queue.offer(new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        indicator.addItem(result);
                        return true;
                    }
                });
            }
        };
    }
}

