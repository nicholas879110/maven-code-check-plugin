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
package com.gome.maven.openapi.application.impl;

import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.ModalityStateListener;
import com.gome.maven.openapi.diagnostic.FrequentEventDetector;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.EventDispatcher;
import com.gome.maven.util.concurrency.Semaphore;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.Stack;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"SSBasedInspection"})
public class LaterInvocator {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.application.impl.LaterInvocator");
    private static final boolean DEBUG = LOG.isDebugEnabled();

    public static final Object LOCK = new Object(); //public for tests
    private static final IdeEventQueue ourEventQueue = IdeEventQueue.getInstance();
    private static final FrequentEventDetector ourFrequentEventDetector = new FrequentEventDetector(1009, 100);

    private LaterInvocator() {
    }

    private static class RunnableInfo {
         private final Runnable runnable;
         private final ModalityState modalityState;
         private final Condition<Object> expired;
         private final ActionCallback callback;

        public RunnableInfo( Runnable runnable,
                             ModalityState modalityState,
                             Condition<Object> expired,
                             ActionCallback callback) {
            this.runnable = runnable;
            this.modalityState = modalityState;
            this.expired = expired;
            this.callback = callback;
        }

        @Override
        
        public String toString() {
            return "[runnable: " + runnable + "; state=" + modalityState + (expired.value(null) ? "; expired" : "")+"] ";
        }
    }

    private static final List<Object> ourModalEntities = ContainerUtil.createLockFreeCopyOnWriteList();
    private static final List<RunnableInfo> ourQueue = new ArrayList<RunnableInfo>(); //protected by LOCK
    private static volatile int ourQueueSkipCount = 0; // optimization
    private static final Runnable ourFlushQueueRunnable = new FlushQueue();

    private static final Stack<AWTEvent> ourEventStack = new Stack<AWTEvent>(); // guarded by RUN_LOCK

    private static final EventDispatcher<ModalityStateListener> ourModalityStateMulticaster = EventDispatcher.create(ModalityStateListener.class);

    private static final List<RunnableInfo> ourForcedFlushQueue = new ArrayList<RunnableInfo>();

    public static void addModalityStateListener( ModalityStateListener listener,  Disposable parentDisposable) {
        ourModalityStateMulticaster.addListener(listener, parentDisposable);
    }

    
    static ModalityStateEx modalityStateForWindow( Window window) {
        int index = ourModalEntities.indexOf(window);
        if (index < 0) {
            Window owner = window.getOwner();
            if (owner == null) return (ModalityStateEx)ApplicationManager.getApplication().getNoneModalityState();
            ModalityStateEx ownerState = modalityStateForWindow(owner);
            if (window instanceof Dialog && ((Dialog)window).isModal()) {
                return ownerState.appendEntity(window);
            }
            return ownerState;
        }

        ArrayList<Object> result = new ArrayList<Object>();
        for (Object entity : ourModalEntities) {
            if (entity instanceof Window) {
                result.add(entity);
            }
            else if (entity instanceof ProgressIndicator) {
                if (((ProgressIndicator)entity).isModal()) {
                    result.add(entity);
                }
            }
        }
        return new ModalityStateEx(result.toArray());
    }

    
    static ActionCallback invokeLater( Runnable runnable,  Condition expired) {
        ModalityState modalityState = ModalityState.defaultModalityState();
        return invokeLater(runnable, modalityState, expired);
    }

    
    static ActionCallback invokeLater( Runnable runnable,  ModalityState modalityState) {
        return invokeLater(runnable, modalityState, Conditions.FALSE);
    }

    
    static ActionCallback invokeLater( Runnable runnable,
                                       ModalityState modalityState,
                                       Condition<Object> expired) {
        ourFrequentEventDetector.eventHappened();

        final ActionCallback callback = new ActionCallback();
        RunnableInfo runnableInfo = new RunnableInfo(runnable, modalityState, expired, callback);
        synchronized (LOCK) {
            ourQueue.add(runnableInfo);
        }
        requestFlush();
        return callback;
    }

    static void invokeAndWait( final Runnable runnable,  ModalityState modalityState) {
        LOG.assertTrue(!isDispatchThread());

        final Semaphore semaphore = new Semaphore();
        semaphore.down();
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                }
                finally {
                    semaphore.up();
                }
            }

            @Override
            
            public String toString() {
                return "InvokeAndWait[" + runnable + "]";
            }
        };
        invokeLater(runnable1, modalityState);
        semaphore.waitFor();
    }

    public static void enterModal( Object modalEntity) {
        LOG.assertTrue(isDispatchThread(), "enterModal() should be invoked in event-dispatch thread");

        if (LOG.isDebugEnabled()) {
            LOG.debug("enterModal:" + modalEntity);
        }

        ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(true);

        ourModalEntities.add(modalEntity);
    }

    public static void leaveModal( Object modalEntity) {
        LOG.assertTrue(isDispatchThread(), "leaveModal() should be invoked in event-dispatch thread");

        if (LOG.isDebugEnabled()) {
            LOG.debug("leaveModal:" + modalEntity);
        }

        ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(false);

        boolean removed = ourModalEntities.remove(modalEntity);
        LOG.assertTrue(removed, modalEntity);
        cleanupQueueForModal(modalEntity);
        ourQueueSkipCount = 0;
        requestFlush();
    }

    private static void cleanupQueueForModal( final Object modalEntity) {
        synchronized (LOCK) {
            for (Iterator<RunnableInfo> iterator = ourQueue.iterator(); iterator.hasNext(); ) {
                RunnableInfo runnableInfo = iterator.next();
                if (runnableInfo.modalityState instanceof ModalityStateEx) {
                    ModalityStateEx stateEx = (ModalityStateEx)runnableInfo.modalityState;
                    if (stateEx.contains(modalEntity)) {
                        ourForcedFlushQueue.add(runnableInfo);
                        iterator.remove();
                    }
                }
            }
        }
    }

    
    static void leaveAllModals() {
        ourModalEntities.clear();
        ourQueueSkipCount = 0;
        requestFlush();
    }

    
    public static Object[] getCurrentModalEntities() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        //TODO!
        //LOG.assertTrue(IdeEventQueue.getInstance().isInInputEvent() || isInMyRunnable());

        return ArrayUtil.toObjectArray(ourModalEntities);
    }

    public static boolean isInModalContext() {
        LOG.assertTrue(isDispatchThread());
        return !ourModalEntities.isEmpty();
    }

    private static boolean isDispatchThread() {
        return ApplicationManager.getApplication().isDispatchThread();
    }

    private static void requestFlush() {
        if (FLUSHER_SCHEDULED.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(ourFlushQueueRunnable);
        }
    }

    /**
     * There might be some requests in the queue, but ourFlushQueueRunnable might not be scheduled yet. In these circumstances
     * {@link EventQueue#peekEvent()} default implementation would return null, and {@link UIUtil#dispatchAllInvocationEvents()} would
     * stop processing events too early and lead to spurious test failures.
     *
     * @see IdeEventQueue#peekEvent()
     */
    public static boolean ensureFlushRequested() {
        if (getNextEvent(false) != null) {
            SwingUtilities.invokeLater(ourFlushQueueRunnable);
            return true;
        }
        return false;
    }

    private static RunnableInfo getNextEvent(boolean remove) {
        synchronized (LOCK) {
            if (!ourForcedFlushQueue.isEmpty()) {
                final RunnableInfo toRun = remove ? ourForcedFlushQueue.remove(0) : ourForcedFlushQueue.get(0);
                if (!toRun.expired.value(null)) {
                    return toRun;
                }
                else {
                    toRun.callback.setDone();
                }
            }


            ModalityState currentModality;
            if (ourModalEntities.isEmpty()) {
                Application application = ApplicationManager.getApplication();
                currentModality = application == null ? ModalityState.NON_MODAL : application.getNoneModalityState();
            }
            else {
                currentModality = new ModalityStateEx(ourModalEntities.toArray());
            }

            while (ourQueueSkipCount < ourQueue.size()) {
                RunnableInfo info = ourQueue.get(ourQueueSkipCount);

                if (info.expired.value(null)) {
                    ourQueue.remove(ourQueueSkipCount);
                    info.callback.setDone();
                    continue;
                }

                if (!currentModality.dominates(info.modalityState)) {
                    if (remove) {
                        ourQueue.remove(ourQueueSkipCount);
                    }
                    return info;
                }
                ourQueueSkipCount++;
            }

            return null;
        }
    }

    private static final AtomicBoolean FLUSHER_SCHEDULED = new AtomicBoolean(false);
    private static final Object RUN_LOCK = new Object();

    private static class FlushQueue implements Runnable {
        private RunnableInfo myLastInfo;

        @Override
        public void run() {
            FLUSHER_SCHEDULED.set(false);

            final RunnableInfo lastInfo = getNextEvent(true);
            myLastInfo = lastInfo;

            if (lastInfo != null) {
                synchronized (RUN_LOCK) { // necessary only because of switching to our own event queue
                    AWTEvent event = ourEventQueue.getTrueCurrentEvent();
                    ourEventStack.push(event);
                    int stackSize = ourEventStack.size();

                    try {
                        lastInfo.runnable.run();
                        lastInfo.callback.setDone();
                    }
                    catch (ProcessCanceledException ex) {
                        // ignore
                    }
                    catch (Throwable t) {
                        if (t instanceof StackOverflowError) {
                            t.printStackTrace();
                        }
                        LOG.error(t);
                    }
                    finally {
                        LOG.assertTrue(ourEventStack.size() == stackSize);
                        ourEventStack.pop();

                        if (!DEBUG) myLastInfo = null;
                    }
                }

                requestFlush();
            }
        }

        @Override
        
        public String toString() {
            return "LaterInvocator.FlushQueue" + (myLastInfo == null ? "" : " lastInfo="+myLastInfo);
        }
    }

    
    public static List<RunnableInfo> getLaterInvocatorQueue() {
        synchronized (LOCK) {
            return ContainerUtil.newArrayList(ourQueue);
        }
    }

    
    static String dumpQueue() {
        synchronized (LOCK) {
             String result = "";
            if (!ourForcedFlushQueue.isEmpty()) {
                result = "(Forced queue: " + ourForcedFlushQueue + ") ";
            }
            List<RunnableInfo> r = new ArrayList<RunnableInfo>(ourQueue);
            result += r + (ourQueueSkipCount == 0 ? "" : " (ourQueueSkipCount="+ourQueueSkipCount+")")
                    + (ourModalEntities.isEmpty() ? " (non-modal)" : " (modal entities: "+ourModalEntities+")"
                    + (FLUSHER_SCHEDULED.get() ? " (Flusher scheduled)" : "")
            );
            return result;
        }
    }
}
