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
package com.gome.maven.util.containers;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.util.Processor;
import com.gome.maven.util.concurrency.Semaphore;
import gnu.trove.Equality;

import javax.swing.*;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows to process elements in the EDT.
 * Processes elements in batches, no longer than 200ms (or maxUnitOfWorkThresholdMs constructor parameter) per batch,
 * and reschedules processing later for longer batches.
 * Usage: {@link TransferToEDTQueue#offer(Object)} } : schedules element for processing in EDT (via invokeLater)
 */
public class TransferToEDTQueue<T> {
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private final String myName;
    private final Processor<T> myProcessor;
    private volatile boolean stopped;
    private final Condition<?> myShutUpCondition;
    private final int myMaxUnitOfWorkThresholdMs; //-1 means indefinite

    private final Queue<T> myQueue = new Queue<T>(10); // guarded by myQueue
    private final AtomicBoolean invokeLaterScheduled = new AtomicBoolean();
    private final Runnable myUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean b = invokeLaterScheduled.compareAndSet(true, false);
            assert b;
            if (stopped || myShutUpCondition.value(null)) {
                stop();
                return;
            }
            long start = System.currentTimeMillis();
            int processed = 0;
            while (processNext()) {
                processed++;
                long finish = System.currentTimeMillis();
                if (myMaxUnitOfWorkThresholdMs != -1 && finish - start > myMaxUnitOfWorkThresholdMs) break;
            }
            if (!isEmpty()) {
                scheduleUpdate();
            }
        }
    };

    public TransferToEDTQueue(  String name,
                               Processor<T> processor,
                               Condition<?> shutUpCondition,
                              int maxUnitOfWorkThresholdMs) {
        myName = name;
        myProcessor = processor;
        myShutUpCondition = shutUpCondition;
        myMaxUnitOfWorkThresholdMs = maxUnitOfWorkThresholdMs;
    }

    public static TransferToEDTQueue<Runnable> createRunnableMerger(  String name, int maxUnitOfWorkThresholdMs) {
        return new TransferToEDTQueue<Runnable>(name, new Processor<Runnable>() {
            @Override
            public boolean process(Runnable runnable) {
                runnable.run();
                return true;
            }
        }, Conditions.alwaysFalse(), maxUnitOfWorkThresholdMs);
    }

    private boolean isEmpty() {
        synchronized (myQueue) {
            return myQueue.isEmpty();
        }
    }

    // return true if element was pulled from the queue and processed successfully
    private boolean processNext() {
        T thing = pullFirst();
        if (thing == null) return false;
        if (!myProcessor.process(thing)) {
            stop();
            return false;
        }
        return true;
    }

    protected T pullFirst() {
        synchronized (myQueue) {
            return myQueue.isEmpty() ? null : myQueue.pullFirst();
        }
    }

    public boolean offer( T thing) {
        synchronized (myQueue) {
            myQueue.addLast(thing);
        }
        scheduleUpdate();
        return true;
    }

    public boolean offerIfAbsent( T thing) {
        return offerIfAbsent(thing, Equality.CANONICAL);
    }

    public boolean offerIfAbsent( final T thing,  final Equality<T> equality) {
        synchronized (myQueue) {
            boolean absent = myQueue.process(new Processor<T>() {
                @Override
                public boolean process(T t) {
                    return !equality.equals(t, thing);
                }
            });
            if (absent) {
                myQueue.addLast(thing);
                scheduleUpdate();
            }
            return absent;
        }
    }

    private void scheduleUpdate() {
        if (!stopped && invokeLaterScheduled.compareAndSet(false, true)) {
            schedule(myUpdateRunnable);
        }
    }

    protected void schedule( Runnable updateRunnable) {
        SwingUtilities.invokeLater(updateRunnable);
    }

    public void stop() {
        stopped = true;
        synchronized (myQueue) {
            myQueue.clear();
        }
    }

    public int size() {
        synchronized (myQueue) {
            return myQueue.size();
        }
    }

    public Collection<T> dump() {
        synchronized (myQueue) {
            return myQueue.toList();
        }
    }

    // process all queue in current thread
    public void drain() {
        int processed = 0;
        long start = System.currentTimeMillis();
        while (processNext()) {
            processed++;
        }
        long finish = System.currentTimeMillis();
    }

    // blocks until all elements in the queue are processed
    public void waitFor() {
        final Semaphore semaphore = new Semaphore();
        semaphore.down();
        schedule(new Runnable() {
            @Override
            public void run() {
                semaphore.up();
            }
        });
        semaphore.waitFor();
    }

}
