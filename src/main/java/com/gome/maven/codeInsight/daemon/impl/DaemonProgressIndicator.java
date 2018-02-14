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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.progress.StandardProgressIndicator;
import com.gome.maven.openapi.progress.util.AbstractProgressIndicatorBase;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.TraceableDisposable;

/**
 * @author cdr
 */
public class DaemonProgressIndicator extends AbstractProgressIndicatorBase implements StandardProgressIndicator, Disposable {
    private static boolean debug;
    private final TraceableDisposable myTraceableDisposable = new TraceableDisposable(debug ? new Throwable() : null);
    private volatile boolean myDisposed;

    @Override
    public synchronized void stop() {
        super.stop();
        cancel();
    }

    public synchronized void stopIfRunning() {
        if (isRunning()) {
            stop();
        }
        else {
            cancel();
        }
    }

    @Override
    public final void cancel() {
        myTraceableDisposable.kill("Daemon Progress Canceled");
        super.cancel();
        Disposer.dispose(this);
    }

    // called when canceled
    @Override
    public void dispose() {
        myDisposed = true;
    }

    @Override
    public final boolean isCanceled() {
        return super.isCanceled();
    }

    @Override
    public final void checkCanceled() {
        super.checkCanceled();
    }

    public void cancel( Throwable cause) {
        myTraceableDisposable.kill("Daemon Progress Canceled because of "+cause);
        super.cancel();
    }

    @Override
    public void start() {
        assert !isCanceled() : "canceled";
        assert !isRunning() : "running";
        super.start();
    }


    public static void setDebug(boolean debug) {
        DaemonProgressIndicator.debug = debug;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + (debug ? "; "+myTraceableDisposable.getStackTrace()+"\n;" : "");
    }

    public boolean isDisposed() {
        return myDisposed;
    }
}