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
package com.gome.maven.openapi.progress.impl;

import com.google.common.collect.ConcurrentHashMultiset;
import com.gome.maven.concurrency.JobScheduler;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.progress.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.ThrowableComputable;
import com.gome.maven.openapi.wm.ex.ProgressIndicatorEx;
import com.gome.maven.psi.PsiLock;
import com.gome.maven.util.containers.ConcurrentLongObjectMap;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.SmartHashSet;
import gnu.trove.THashMap;

import javax.swing.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CoreProgressManager extends ProgressManager implements Disposable {
    static final int CHECK_CANCELED_DELAY_MILLIS = 10;
    final AtomicInteger myCurrentUnsafeProgressCount = new AtomicInteger(0);
    private final AtomicInteger myCurrentModalProgressCount = new AtomicInteger(0);

    private static volatile int ourLockedCheckCounter = 0;
    private static final boolean DISABLED = "disabled".equals(System.getProperty("idea.ProcessCanceledException"));
    private final ScheduledFuture<?> myCheckCancelledFuture;

    // indicator -> threads which are running under this indicator. guarded by this.
    private static final Map<ProgressIndicator, Set<Thread>> threadsUnderIndicator = new THashMap<ProgressIndicator, Set<Thread>>();
    // the active indicator for the thread id
    private static final ConcurrentLongObjectMap<ProgressIndicator> currentIndicators = ContainerUtil.createConcurrentLongObjectMap();
    // threads which are running under canceled indicator
    static final Set<Thread> threadsUnderCanceledIndicator = ContainerUtil.newConcurrentSet();

    // active (i.e. which have executeProcessUnderProgress() method running) indicators which are not inherited from StandardProgressIndicator.
    // for them an extra processing thread (see myCheckCancelledFuture) has to be run to call their non-standard checkCanceled() method
    private static final Collection<ProgressIndicator> nonStandardIndicators = ConcurrentHashMultiset.create();

    public CoreProgressManager() {
        myCheckCancelledFuture = JobScheduler.getScheduler().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (ProgressIndicator indicator : nonStandardIndicators) {
                    try {
                        indicator.checkCanceled();
                    }
                    catch (ProcessCanceledException e) {
                        indicatorCanceled(indicator);
                    }
                }
            }
        }, 0, CHECK_CANCELED_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        myCheckCancelledFuture.cancel(true);
    }

    @Override
    protected void doCheckCanceled() throws ProcessCanceledException {
        boolean thereIsCanceledIndicator = !threadsUnderCanceledIndicator.isEmpty();
        if (thereIsCanceledIndicator) {
            final ProgressIndicator progress = getProgressIndicator();
            if (progress != null) {
                try {
                    progress.checkCanceled();
                }
                catch (ProcessCanceledException e) {
                    if (DISABLED) {
                        return;
                    }
                    if (Thread.holdsLock(PsiLock.LOCK)) {
                        ourLockedCheckCounter++;
                        if (ourLockedCheckCounter > 10) {
                            ourLockedCheckCounter = 0;
                        }
                    }
                    else {
                        ourLockedCheckCounter = 0;
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public boolean hasProgressIndicator() {
        return getProgressIndicator() != null;
    }

    @Override
    public boolean hasUnsafeProgressIndicator() {
        return myCurrentUnsafeProgressCount.get() > 0;
    }

    @Override
    public boolean hasModalProgressIndicator() {
        return myCurrentModalProgressCount.get() > 0;
    }

    @Override
    public void runProcess( final Runnable process, final ProgressIndicator progress) {
        executeProcessUnderProgress(new Runnable(){
            @Override
            public void run() {
                try {
                    try {
                        if (progress != null && !progress.isRunning()) {
                            progress.start();
                        }
                    }
                    catch (RuntimeException e) {
                        throw e;
                    }
                    catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                    process.run();
                }
                finally {
                    if (progress != null && progress.isRunning()) {
                        progress.stop();
                        if (progress instanceof ProgressIndicatorEx) {
                            ((ProgressIndicatorEx)progress).processFinish();
                        }
                    }
                }
            }
        },progress);
    }

    @Override
    public <T> T runProcess( final Computable<T> process, ProgressIndicator progress) throws ProcessCanceledException {
        final Ref<T> ref = new Ref<T>();
        runProcess(new Runnable() {
            @Override
            public void run() {
                ref.set(process.compute());
            }
        }, progress);
        return ref.get();
    }

    @Override
    public void executeNonCancelableSection( Runnable runnable) {
        executeProcessUnderProgress(runnable, new NonCancelableIndicator());
    }

    @Override
    public void setCancelButtonText(String cancelButtonText) {

    }

    @Override
    public boolean runProcessWithProgressSynchronously( Runnable process,
                                                         String progressTitle,
                                                       boolean canBeCanceled,
                                                        Project project) {
        return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
    }

    @Override
    public <T, E extends Exception> T runProcessWithProgressSynchronously( final ThrowableComputable<T, E> process,
                                                                            String progressTitle,
                                                                          boolean canBeCanceled,
                                                                           Project project) throws E {
        final AtomicReference<T> result = new AtomicReference<T>();
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();

        runProcessWithProgressSynchronously(new Task.Modal(project, progressTitle, canBeCanceled) {
            @Override
            public void run( ProgressIndicator indicator) {
                try {
                    T compute = process.compute();
                    result.set(compute);
                }
                catch (Throwable t) {
                    exception.set(t);
                }
            }
        }, null);

        Throwable t = exception.get();
        if (t != null) {
            if (t instanceof Error) throw (Error)t;
            if (t instanceof RuntimeException) throw (RuntimeException)t;
            @SuppressWarnings("unchecked") E e = (E)t;
            throw e;
        }

        return result.get();
    }

    @Override
    public boolean runProcessWithProgressSynchronously( final Runnable process,
                                                         String progressTitle,
                                                       boolean canBeCanceled,
                                                        Project project,
                                                        JComponent parentComponent) {
        Task.Modal task = new Task.Modal(project, progressTitle, canBeCanceled) {
            @Override
            public void run( ProgressIndicator indicator) {
                process.run();
            }
        };
        return runProcessWithProgressSynchronously(task, parentComponent);
    }

    @Override
    public void runProcessWithProgressAsynchronously( Project project,
                                                       String progressTitle,
                                                      Runnable process,
                                                      Runnable successRunnable,
                                                      Runnable canceledRunnable) {
        runProcessWithProgressAsynchronously(project, progressTitle, process, successRunnable, canceledRunnable, PerformInBackgroundOption.DEAF);
    }

    @Override
    public void runProcessWithProgressAsynchronously( Project project,
                                                       String progressTitle,
                                                      final Runnable process,
                                                      final Runnable successRunnable,
                                                      final Runnable canceledRunnable,
                                                      PerformInBackgroundOption option) {
        runProcessWithProgressAsynchronously(new Task.Backgroundable(project, progressTitle, true, option) {
            @Override
            public void run( final ProgressIndicator indicator) {
                process.run();
            }


            @Override
            public void onCancel() {
                if (canceledRunnable != null) {
                    canceledRunnable.run();
                }
            }

            @Override
            public void onSuccess() {
                if (successRunnable != null) {
                    successRunnable.run();
                }
            }
        });
    }

    @Override
    public void run( final Task task) {
        if (task.isHeadless()) {
            if (ApplicationManager.getApplication().isDispatchThread()) {
                runProcessWithProgressSynchronously(task, null);
            }
            else {
                new TaskRunnable(task, new EmptyProgressIndicator()).run();
            }
        }
        else if (task.isModal()) {
            runProcessWithProgressSynchronously(task.asModal(), null);
        }
        else {
            final Task.Backgroundable backgroundable = task.asBackgroundable();
            if (backgroundable.isConditionalModal() && !backgroundable.shouldStartInBackground()) {
                runProcessWithProgressSynchronously(task, null);
            }
            else {
                runProcessWithProgressAsynchronously(backgroundable);
            }
        }
    }

    
    public Future<?> runProcessWithProgressAsynchronously( Task.Backgroundable task) {
        return runProcessWithProgressAsynchronously(task, new EmptyProgressIndicator(), null);
    }

    
    public Future<?> runProcessWithProgressAsynchronously( final Task.Backgroundable task,
                                                           final ProgressIndicator progressIndicator,
                                                           final Runnable continuation) {
        return runProcessWithProgressAsynchronously(task, progressIndicator, continuation, ModalityState.NON_MODAL);
    }

    
    public Future<?> runProcessWithProgressAsynchronously( final Task.Backgroundable task,
                                                           final ProgressIndicator progressIndicator,
                                                           final Runnable continuation,
                                                           final ModalityState modalityState) {
        if (progressIndicator instanceof Disposable) {
            Disposer.register(ApplicationManager.getApplication(), (Disposable)progressIndicator);
        }

        final Runnable process = new TaskRunnable(task, progressIndicator, continuation);

        TaskContainer action = new TaskContainer(task) {
            @Override
            public void run() {
                boolean canceled = false;
                try {
                    ProgressManager.getInstance().runProcess(process, progressIndicator);
                }
                catch (ProcessCanceledException e) {
                    canceled = true;
                }

                if (canceled || progressIndicator.isCanceled()) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            task.onCancel();
                        }
                    }, modalityState);
                }
                else {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            task.onSuccess();
                        }
                    }, modalityState);
                }
            }
        };

        return ApplicationManager.getApplication().executeOnPooledThread(action);
    }

    protected boolean runProcessWithProgressSynchronously( final Task task,  final JComponent parentComponent) {
        final boolean result = ((ApplicationEx)ApplicationManager.getApplication())
                .runProcessWithProgressSynchronously(new TaskContainer(task) {
                    @Override
                    public void run() {
                        new TaskRunnable(task, ProgressManager.getInstance().getProgressIndicator()).run();
                    }
                }, task.getTitle(), task.isCancellable(), task.getProject(), parentComponent, task.getCancelText());
        if (result) {
            task.onSuccess();
        }
        else {
            task.onCancel();
        }
        return result;
    }

    @Override
    public void runProcessWithProgressAsynchronously( Task.Backgroundable task,  ProgressIndicator progressIndicator) {
        runProcessWithProgressAsynchronously(task, progressIndicator, null);
    }

    @Override
    public ProgressIndicator getProgressIndicator() {
        return getCurrentIndicator(Thread.currentThread());
    }

    @Override
    public void executeProcessUnderProgress( Runnable process, ProgressIndicator progress) throws ProcessCanceledException {
        boolean modal = progress != null && progress.isModal();
        if (modal) myCurrentModalProgressCount.incrementAndGet();
        if (progress == null) myCurrentUnsafeProgressCount.incrementAndGet();

        try {
            ProgressIndicator oldIndicator = null;
            boolean set = progress != null && progress != (oldIndicator = getProgressIndicator());
            if (set) {
                Thread currentThread = Thread.currentThread();
                setCurrentIndicator(currentThread, progress);
                try {
                    registerIndicatorAndRun(progress, currentThread, oldIndicator, process);
                }
                finally {
                    setCurrentIndicator(currentThread, oldIndicator);
                }
            }
            else {
                process.run();
            }
        }
        finally {
            if (progress == null) myCurrentUnsafeProgressCount.decrementAndGet();
            if (modal) myCurrentModalProgressCount.decrementAndGet();
        }
    }

    private static void registerIndicatorAndRun( ProgressIndicator indicator,
                                                 Thread currentThread,
                                                ProgressIndicator oldIndicator,
                                                 Runnable process) {
        Set<Thread> underIndicator;
        boolean alreadyUnder;
        boolean isStandard;
        synchronized (threadsUnderIndicator) {
            underIndicator = threadsUnderIndicator.get(indicator);
            if (underIndicator == null) {
                underIndicator = new SmartHashSet<Thread>();
                threadsUnderIndicator.put(indicator, underIndicator);
            }
            alreadyUnder = !underIndicator.add(currentThread);
            isStandard = indicator instanceof StandardProgressIndicator;
            if (!isStandard) {
                nonStandardIndicators.add(indicator);
            }

            if (indicator.isCanceled()) {
                threadsUnderCanceledIndicator.add(currentThread);
            }
            else {
                threadsUnderCanceledIndicator.remove(currentThread);
            }
        }

        try {
            if (indicator instanceof WrappedProgressIndicator) {
                ProgressIndicator wrappee = ((WrappedProgressIndicator)indicator).getOriginalProgressIndicator();
                assert wrappee != indicator : indicator + " wraps itself";
                registerIndicatorAndRun(wrappee, currentThread, oldIndicator, process);
            }
            else {
                process.run();
            }
        }
        finally {
            synchronized (threadsUnderIndicator) {
                boolean removed = alreadyUnder || underIndicator.remove(currentThread);
                if (removed && underIndicator.isEmpty()) {
                    threadsUnderIndicator.remove(indicator);
                }
                if (!isStandard) {
                    nonStandardIndicators.remove(indicator);
                }
                // by this time oldIndicator may have been canceled
                if (oldIndicator != null && oldIndicator.isCanceled()) {
                    threadsUnderCanceledIndicator.add(currentThread);
                }
                else {
                    threadsUnderCanceledIndicator.remove(currentThread);
                }
            }
        }
    }

    @Override
    protected void indicatorCanceled( ProgressIndicator indicator) {
        // mark threads running under this indicator as canceled
        synchronized (threadsUnderIndicator) {
            Set<Thread> threads = threadsUnderIndicator.get(indicator);
            if (threads != null) {
                for (Thread thread : threads) {
                    boolean underCancelledIndicator = false;
                    for (ProgressIndicator currentIndicator = getCurrentIndicator(thread);
                         currentIndicator != null;
                         currentIndicator = currentIndicator instanceof WrappedProgressIndicator ?
                                 ((WrappedProgressIndicator)currentIndicator).getOriginalProgressIndicator() : null) {
                        if (currentIndicator == indicator) {
                            underCancelledIndicator = true;
                            break;
                        }
                    }

                    if (underCancelledIndicator) {
                        threadsUnderCanceledIndicator.add(thread);
                    }
                }
            }
        }
    }

    
    public static boolean isCanceledThread( Thread thread) {
        return threadsUnderCanceledIndicator.contains(thread);
    }

    
    @Override
    public final NonCancelableSection startNonCancelableSection() {
        final ProgressIndicator myOld = ProgressManager.getInstance().getProgressIndicator();

        final Thread currentThread = Thread.currentThread();
        NonCancelableIndicator nonCancelor = new NonCancelableIndicator() {
            @Override
            public void done() {
                setCurrentIndicator(currentThread, myOld);
            }
        };
        setCurrentIndicator(currentThread, nonCancelor);
        return nonCancelor;
    }

    private static void setCurrentIndicator( Thread currentThread, ProgressIndicator indicator) {
        if (indicator == null) {
            currentIndicators.remove(currentThread.getId());
        }
        else {
            currentIndicators.put(currentThread.getId(), indicator);
        }
    }
    private static ProgressIndicator getCurrentIndicator( Thread thread) {
        return currentIndicators.get(thread.getId());
    }

    protected abstract static class TaskContainer implements Runnable {
        private final Task myTask;

        protected TaskContainer( Task task) {
            myTask = task;
        }

        
        public Task getTask() {
            return myTask;
        }
    }
    protected static class TaskRunnable extends TaskContainer {
        private final ProgressIndicator myIndicator;
        private final Runnable myContinuation;

        public TaskRunnable( Task task,  ProgressIndicator indicator) {
            this(task, indicator, null);
        }

        public TaskRunnable( Task task,  ProgressIndicator indicator,  Runnable continuation) {
            super(task);
            myIndicator = indicator;
            myContinuation = continuation;
        }

        @Override
        public void run() {
            try {
                getTask().run(myIndicator);
            }
            finally {
                try {
                    if (myIndicator instanceof ProgressIndicatorEx) {
                        ((ProgressIndicatorEx)myIndicator).finish(getTask());
                    }
                }
                finally {
                    if (myContinuation != null) {
                        myContinuation.run();
                    }
                }
            }
        }
    }
}
