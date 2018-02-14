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
package com.gome.maven.execution.configurations;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.registry.Registry;
//import com.pty4j.PtyProcess;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A flavor of GeneralCommandLine to start processes with Pseudo-Terminal (PTY).
 *
 * Warning: PtyCommandLine works with ProcessHandler only in blocking read mode.
 * Please make sure that you use appropriate ProcessHandler implementation.
 *
 * Note: this works only on Unix, on Windows regular processes are used instead.
 */
public class PtyCommandLine extends GeneralCommandLine {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.execution.configurations.PtyCommandLine");
    public static final String RUN_PROCESSES_WITH_PTY = "run.processes.with.pty";
    private boolean myUseCygwinLaunch;
    private boolean myConsoleMode = true;

    public PtyCommandLine() { }

    public static boolean isEnabled() {
        return Registry.is(RUN_PROCESSES_WITH_PTY);
    }

    
    @Override
    protected Process startProcess( List<String> commands) throws IOException {
        try {
            return startProcessWithPty(commands, myConsoleMode);
        }
        catch (Throwable e) {
            File logFile = getPtyLogFile();
            if (ApplicationManager.getApplication().isEAP() && logFile.exists()) {
                String logContent;
                try {
                    logContent = FileUtil.loadFile(logFile);
                } catch (Exception ignore) {
                    logContent = "Unable to retrieve log";
                }

                LOG.error("Couldn't run process with PTY", e, logContent);
            }
            else {
                LOG.error("Couldn't run process with PTY", e);
            }
        }

        return super.startProcess(commands);
    }

    public void setUseCygwinLaunch(boolean useCygwinLaunch) {
        myUseCygwinLaunch = useCygwinLaunch;
    }

    public void setConsoleMode(boolean consoleMode) {
        myConsoleMode = consoleMode;
    }

    private static File getPtyLogFile() {
        return new File(PathManager.getLogPath(), "pty.log");
    }

    
    public Process startProcessWithPty( List<String> commands, boolean console) throws IOException {
        Map<String, String> env = new HashMap<String, String>();
        setupEnvironment(env);

        if (isRedirectErrorStream()) {
            LOG.error("Launching process with PTY and redirected error stream is unsupported yet");
        }

        File workDirectory = getWorkDirectory();
        boolean cygwin = myUseCygwinLaunch && SystemInfo.isWindows;
//        return PtyProcess.exec(ArrayUtil.toStringArray(commands), env, workDirectory != null ? workDirectory.getPath() : null, console, cygwin,
//                ApplicationManager.getApplication().isEAP() ? getPtyLogFile() : null);
        return null;
    }
}