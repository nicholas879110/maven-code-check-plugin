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
package com.gome.maven.execution.process;

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.configurations.PtyCommandLine;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.encoding.EncodingManager;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

public class OSProcessHandler extends BaseOSProcessHandler {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.execution.process.OSProcessHandler");
    private boolean myHasPty = false;

    private boolean myDestroyRecursively = true;

    public OSProcessHandler( GeneralCommandLine commandLine) throws ExecutionException {
        this(commandLine.createProcess(), commandLine.getCommandLineString(), CharsetToolkit.UTF8_CHARSET);
        setHasPty(commandLine instanceof PtyCommandLine);
    }

    public OSProcessHandler( final Process process) {
        this(process, null);
    }

    public OSProcessHandler( final Process process,  final String commandLine) {
        this(process, commandLine, EncodingManager.getInstance().getDefaultCharset());
    }

    public OSProcessHandler( final Process process,  final String commandLine,  final Charset charset) {
        super(process, commandLine, charset);
    }

    protected OSProcessHandler( final OSProcessHandler base) {
        this(base.myProcess, base.myCommandLine);
    }

    @Override
    protected Future<?> executeOnPooledThread(Runnable task) {
        final Application application = ApplicationManager.getApplication();

        if (application != null) {
            return application.executeOnPooledThread(task);
        }

        return super.executeOnPooledThread(task);
    }

    protected boolean shouldDestroyProcessRecursively() {
        // Override this method if you want to kill process recursively (whole process try) by default
        // such behaviour is better than default java one, which doesn't kill children processes
        return myDestroyRecursively;
    }

    public void setShouldDestroyProcessRecursively(boolean destroyRecursively) {
        myDestroyRecursively = destroyRecursively;
    }

    @Override
    protected void doDestroyProcess() {
        // Override this method if you want to customize default destroy behaviour, e.g.
        // if you want use some soft-kill.
        final Process process = getProcess();
        if (shouldDestroyProcessRecursively() && processCanBeKilledByOS(process)) {
            killProcessTree(process);
        }
        else {
            process.destroy();
        }
    }

    public static boolean processCanBeKilledByOS(Process process) {
        return !(process instanceof SelfKiller);
    }

    /**
     * Kill the whole process tree.
     *
     * @param process Process
     * @return True if process tree has been successfully killed.
     */
    protected boolean killProcessTree( Process process) {
        LOG.debug("killing process tree");
        final boolean destroyed = OSProcessManager.getInstance().killProcessTree(process);
        if (!destroyed) {
            LOG.warn("Cannot kill process tree. Trying to destroy process using Java API. Cmdline:\n" + myCommandLine);
            process.destroy();
        }
        return destroyed;
    }

    /**
     * In case of pty this process handler will use blocking read. The value should be set before
     * startNotify invocation. It is set by default in case of using GeneralCommandLine based constructor.
     *
     * @param hasPty true if process is pty based
     */
    public void setHasPty(boolean hasPty) {
        myHasPty = hasPty;
    }

    @Override
    protected boolean useNonBlockingRead() {
        if (myHasPty) {
            // blocking read in case of pty based process
            return false;
        }
        else {
            return super.useNonBlockingRead();
        }
    }
}
