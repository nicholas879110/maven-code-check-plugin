/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.openapi.application.impl;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.TransferToEDTQueue;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Passes elements for processing in the dedicated thread.
 * Processes elements in batches, no longer than {maxUnitOfWorkThresholdMs}ms per batch, and reschedules processing later for longer batches.
 * Usage: {@link #offer(Object)} } : schedules element for processing in a pooled thread
 */
public class TransferToPooledThreadQueue<T> extends TransferToEDTQueue<T> {
    private final ScheduledThreadPoolExecutor myExecutor;

    public TransferToPooledThreadQueue(  String name,
                                        Condition<?> shutUpCondition,
                                       int maxUnitOfWorkThresholdMs,
                                        Processor<T> processor) {
        super(name, processor, shutUpCondition, maxUnitOfWorkThresholdMs);
        myExecutor = ConcurrencyUtil.newSingleScheduledThreadExecutor(name);
    }

    @Override
    protected void schedule( Runnable updateRunnable) {
        myExecutor.execute(updateRunnable);
    }

}
