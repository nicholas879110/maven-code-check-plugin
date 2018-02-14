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
package com.gome.maven.ide;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationActivationListener;
import com.gome.maven.openapi.application.impl.ApplicationImpl;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.BusyObject;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;

public class FrameStateManagerImpl extends FrameStateManager {
    private final List<FrameStateListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    private boolean myShouldSynchronize;
    private final Alarm mySyncAlarm;

    private final BusyObject.Impl myActive;
    private final ApplicationImpl myApp;

    public FrameStateManagerImpl(final ApplicationImpl app) {
        myApp = app;
        myActive = new BusyObject.Impl() {
            @Override
            public boolean isReady() {
                return myApp.isActive();
            }
        };

        myShouldSynchronize = false;
        mySyncAlarm = new Alarm();

        app.getMessageBus().connect().subscribe(ApplicationActivationListener.TOPIC, new ApplicationActivationListener() {
            @Override
            public void applicationActivated(IdeFrame ideFrame) {
                myActive.onReady();
                mySyncAlarm.cancelAllRequests();
                if (myShouldSynchronize) {
                    myShouldSynchronize = false;
                    fireActivationEvent();
                }
            }

            @Override
            public void applicationDeactivated(IdeFrame ideFrame) {
                mySyncAlarm.cancelAllRequests();
                mySyncAlarm.addRequest(new Runnable() {
                    @Override
                    public void run() {
                        if (!app.isActive() && !app.isDisposed()) {
                            myShouldSynchronize = true;
                            fireDeactivationEvent();
                        }
                    }
                }, 200);
            }
        });
    }

    @Override
    public ActionCallback getApplicationActive() {
        return myActive.getReady(this);
    }

    private void fireDeactivationEvent() {
        for (FrameStateListener listener : myListeners) {
            listener.onFrameDeactivated();
        }
    }

    private void fireActivationEvent() {
        for (FrameStateListener listener : myListeners) {
            listener.onFrameActivated();
        }
    }

    @Override
    public void addListener( FrameStateListener listener) {
        addListener(listener, null);
    }

    @Override
    public void addListener( final FrameStateListener listener,  Disposable disposable) {
        myListeners.add(listener);
        if (disposable != null) {
            Disposer.register(disposable, new Disposable() {
                @Override
                public void dispose() {
                    removeListener(listener);
                }
            });
        }
    }

    @Override
    public void removeListener( FrameStateListener listener) {
        myListeners.remove(listener);
    }
}
