/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author cdr
 */
public class ConcurrencyUtil {
    /**
     * Invokes and waits all tasks using threadPool, avoiding thread starvation on the way
     * (see <a href="http://gafter.blogspot.com/2006/11/thread-pool-puzzler.html">"A Thread Pool Puzzler"</a>).
     */
    public static <T> List<Future<T>> invokeAll( Collection<Callable<T>> tasks, ExecutorService executorService) throws Throwable {
        if (executorService == null) {
            for (Callable<T> task : tasks) {
                task.call();
            }
            return null;
        }

        List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                Future<T> future = executorService.submit(t);
                futures.add(future);
            }
            // force not started futures to execute using the current thread
            for (Future f : futures) {
                ((Runnable)f).run();
            }
            for (Future f : futures) {
                try {
                    f.get();
                }
                catch (CancellationException ignore) {
                }
                catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        throw cause;
                    }
                }
            }
            done = true;
        }
        finally {
            if (!done) {
                for (Future f : futures) {
                    f.cancel(false);
                }
            }
        }
        return futures;
    }

    /**
     * @return defaultValue if there is no entry in the map (in that case defaultValue is placed into the map),
     *         or corresponding value if entry already exists.
     */
    
    public static <K, V> V cacheOrGet( ConcurrentMap<K, V> map,  final K key,  final V defaultValue) {
        V v = map.get(key);
        if (v != null) return v;
        V prev = map.putIfAbsent(key, defaultValue);
        return prev == null ? defaultValue : prev;
    }

    
    public static ThreadPoolExecutor newSingleThreadExecutor(  String name) {
        return newSingleThreadExecutor(name, Thread.NORM_PRIORITY);
    }

    
    public static ThreadPoolExecutor newSingleThreadExecutor(  String name, int priority) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), newNamedThreadFactory(name, true, priority));
    }

    
    public static ScheduledThreadPoolExecutor newSingleScheduledThreadExecutor(  String name) {
        return newSingleScheduledThreadExecutor(name, Thread.NORM_PRIORITY);
    }

    
    public static ScheduledThreadPoolExecutor newSingleScheduledThreadExecutor(  String name, int priority) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, newNamedThreadFactory(name, true, priority));
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return executor;
    }

    
    public static ThreadFactory newNamedThreadFactory(  final String name, final boolean isDaemon, final int priority) {
        return new ThreadFactory() {
            
            @Override
            public Thread newThread( Runnable r) {
                Thread thread = new Thread(r, name);
                thread.setDaemon(isDaemon);
                thread.setPriority(priority);
                return thread;
            }
        };
    }

    
    public static ThreadFactory newNamedThreadFactory(  final String name) {
        return new ThreadFactory() {
            
            @Override
            public Thread newThread( final Runnable r) {
                return new Thread(r, name);
            }
        };
    }
}
