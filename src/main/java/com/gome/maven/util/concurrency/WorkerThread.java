/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.util.concurrency;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;

import java.util.LinkedList;


/**
 * @deprecated use lightweight com.gome.maven.util.concurrency.QueueProcessor instead
 */
public class WorkerThread implements Runnable, Disposable {
    private final LinkedList<Runnable> myTasks = new LinkedList<Runnable>();
    private boolean myToDispose = false;
    private boolean myDisposed = false;
    private final int mySleep;
    private final String myName;

    public WorkerThread( String name, int sleep) {
        mySleep = sleep;
        myName = name;
    }

    public void start() {
        ApplicationManager.getApplication().executeOnPooledThread(this);
    }

    public boolean addTask(Runnable action) {
        synchronized(myTasks){
            if(myDisposed) return false;

            myTasks.add(action);
            myTasks.notifyAll();
            return true;
        }
    }

    public boolean addTaskFirst( Runnable action) {
        synchronized(myTasks){
            if(myDisposed) return false;

            myTasks.add(0, action);
            myTasks.notifyAll();
            return true;
        }
    }

    @Override
    public void dispose() {
        dispose(true);
    }

    public void dispose(boolean cancelTasks){
        synchronized(myTasks){
            if (cancelTasks){
                myTasks.clear();
            }
            myToDispose = true;
            myTasks.notifyAll();
        }
    }

    public void cancelTasks() {
        synchronized(myTasks){
            myTasks.clear();
            myTasks.notifyAll();
        }
    }

    public boolean isDisposeRequested() {
        synchronized(myTasks){
            return myToDispose;
        }
    }

    public boolean isDisposed() {
        synchronized(myTasks){
            return myDisposed;
        }
    }

    @Override
    public void run() {
        while (true) {
            while (true) {
                Runnable task;
                synchronized (myTasks) {
                    if (myTasks.isEmpty()) break;
                    task = myTasks.removeFirst();
                }
                task.run();
                try {
                    if (mySleep > 0) {
                        Thread.sleep(mySleep);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (myTasks) {
                if (myToDispose && myTasks.isEmpty()) {
                    myDisposed = true;
                    return;
                }

                try {
                    myTasks.wait();
                }
                catch (InterruptedException ignored) {
                }
            }
        }
    }
}
