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
package com.gome.maven.execution.configurations;

import com.gome.maven.execution.CommandLineUtil;
import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.Platform;
import com.gome.maven.execution.process.ProcessNotCreatedException;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.EnvironmentUtil;
import com.gome.maven.util.PlatformUtils;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.text.CaseInsensitiveStringHashingStrategy;
import gnu.trove.THashMap;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * OS-independent way of executing external processes with complex parameters.
 * <p/>
 * Main idea of the class is to accept parameters "as-is", just as they should look to an external process, and quote/escape them
 * as required by the underlying platform.
 *
 * @see com.gome.maven.execution.process.OSProcessHandler
 */
public class GeneralCommandLine implements UserDataHolder {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.execution.configurations.GeneralCommandLine");

    private String myExePath = null;
    private File myWorkDirectory = null;
    private final Map<String, String> myEnvParams = new MyTHashMap();
    private boolean myPassParentEnvironment = true;
    private final ParametersList myProgramParams = new ParametersList();
    private Charset myCharset = CharsetToolkit.getDefaultSystemCharset();
    private boolean myRedirectErrorStream = false;
    private Map<Object, Object> myUserData = null;

    public GeneralCommandLine() { }

    public GeneralCommandLine(String... command) {
        this(Arrays.asList(command));
    }

    public GeneralCommandLine(List<String> command) {
        int size = command.size();
        if (size > 0) {
            setExePath(command.get(0));
            if (size > 1) {
                addParameters(command.subList(1, size));
            }
        }
    }

    public String getExePath() {
        return myExePath;
    }

    public void setExePath(final String exePath) {
        myExePath = exePath.trim();
    }

    public File getWorkDirectory() {
        return myWorkDirectory;
    }

    
    public GeneralCommandLine withWorkDirectory( final String path) {
        return withWorkDirectory(path != null ? new File(path) : null);
    }

    
    public GeneralCommandLine withWorkDirectory( final File workDirectory) {
        myWorkDirectory = workDirectory;
        return this;
    }

    /**
     * @deprecated Use {@link #withWorkDirectory(String)} instead.
     */
    public void setWorkDirectory( final String path) {
        withWorkDirectory(path);
    }

    /**
     * @deprecated Use {@link #withWorkDirectory(java.io.File)} instead.
     */
    public void setWorkDirectory( final File workDirectory) {
        myWorkDirectory = workDirectory;
    }

    /**
     * Note: the map returned is forgiving to passing null values into putAll().
     */
    
    public Map<String, String> getEnvironment() {
        return myEnvParams;
    }

    
    public GeneralCommandLine withEnvironment( Map<String, String> environment) {
        if (environment != null) {
            getEnvironment().putAll(environment);
        }
        return this;
    }

    /**
     * @deprecated use {@link #getEnvironment()} (to remove in IDEA 14)
     */
    @SuppressWarnings("unused")
    public Map<String, String> getEnvParams() {
        return getEnvironment();
    }

    /**
     * @deprecated use {@link #getEnvironment()} (to remove in IDEA 14)
     */
    @SuppressWarnings("unused")
    public void setEnvParams( Map<String, String> envParams) {
        myEnvParams.clear();
        if (envParams != null) {
            myEnvParams.putAll(envParams);
        }
    }

    public void setPassParentEnvironment(boolean passParentEnvironment) {
        myPassParentEnvironment = passParentEnvironment;
    }

    /**
     * @deprecated use {@link #setPassParentEnvironment(boolean)} (to remove in IDEA 14)
     */
    @SuppressWarnings({"unused", "SpellCheckingInspection"})
    public void setPassParentEnvs(boolean passParentEnvironment) {
        setPassParentEnvironment(passParentEnvironment);
    }

    public boolean isPassParentEnvironment() {
        return myPassParentEnvironment;
    }

    /**
     * @return unmodifiable map of the parent environment, that will be passed to the process if isPassParentEnvironment() == true
     */
    
    public Map<String, String> getParentEnvironment() {
        return PlatformUtils.isAppCode() ? System.getenv() // Temporarily fix for OC-8606
                : EnvironmentUtil.getEnvironmentMap();
    }

    public void addParameters(final String... parameters) {
        for (String parameter : parameters) {
            addParameter(parameter);
        }
    }

    public void addParameters(final List<String> parameters) {
        for (final String parameter : parameters) {
            addParameter(parameter);
        }
    }

    public void addParameter(final String parameter) {
        myProgramParams.add(parameter);
    }

    public ParametersList getParametersList() {
        return myProgramParams;
    }

    
    public Charset getCharset() {
        return myCharset;
    }

    public GeneralCommandLine withCharset(final Charset charset) {
        myCharset = charset;
        return this;
    }

    /**
     * @deprecated Use {@link #withCharset} instead.
     */
    public void setCharset(final Charset charset) {
        myCharset = charset;
    }

    public boolean isRedirectErrorStream() {
        return myRedirectErrorStream;
    }

    public void setRedirectErrorStream(final boolean redirectErrorStream) {
        myRedirectErrorStream = redirectErrorStream;
    }

    /**
     * Returns string representation of this command line.<br/>
     * Warning: resulting string is not OS-dependent - <b>do not</b> use it for executing this command line.
     *
     * @return single-string representation of this command line.
     */
    public String getCommandLineString() {
        return getCommandLineString(null);
    }

    /**
     * Returns string representation of this command line.<br/>
     * Warning: resulting string is not OS-dependent - <b>do not</b> use it for executing this command line.
     *
     * @param exeName use this executable name instead of given by {@link #setExePath(String)}
     * @return single-string representation of this command line.
     */
    public String getCommandLineString( final String exeName) {
        return ParametersList.join(getCommandLineList(exeName));
    }

    public List<String> getCommandLineList( final String exeName) {
        final List<String> commands = new ArrayList<String>();
        if (exeName != null) {
            commands.add(exeName);
        }
        else if (myExePath != null) {
            commands.add(myExePath);
        }
        else {
            commands.add("<null>");
        }
        commands.addAll(myProgramParams.getList());
        return commands;
    }

    /**
     * Prepares command (quotes and escapes all arguments) and returns it as a newline-separated list
     * (suitable e.g. for passing in an environment variable).
     *
     * @param platform a target platform
     * @return command as a newline-separated list.
     */
    
    public String getPreparedCommandLine(Platform platform) {
        String exePath = myExePath != null ? myExePath : "";
        return StringUtil.join(CommandLineUtil.toCommandLine(exePath, myProgramParams.getList(), platform), "\n");
    }

    
    public Process createProcess() throws ExecutionException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing [" + getCommandLineString() + "]");
        }

        List<String> commands;
        try {
            checkWorkingDirectory();

            if (StringUtil.isEmptyOrSpaces(myExePath)) {
                throw new ExecutionException(IdeBundle.message("run.configuration.error.executable.not.specified"));
            }

            commands = CommandLineUtil.toCommandLine(myExePath, myProgramParams.getList());
        }
        catch (ExecutionException e) {
            LOG.info(e);
            throw e;
        }

        try {
            return startProcess(commands);
        }
        catch (IOException e) {
            LOG.info(e);
            throw new ProcessNotCreatedException(e.getMessage(), e, this);
        }
    }

    
    protected Process startProcess(List<String> commands) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(commands);
        setupEnvironment(builder.environment());
        builder.directory(myWorkDirectory);
        builder.redirectErrorStream(myRedirectErrorStream);
        return builder.start();
    }

    private void checkWorkingDirectory() throws ExecutionException {
        if (myWorkDirectory == null) {
            return;
        }
        if (!myWorkDirectory.exists()) {
            throw new ExecutionException(
                    IdeBundle.message("run.configuration.error.working.directory.does.not.exist", myWorkDirectory.getAbsolutePath()));
        }
        if (!myWorkDirectory.isDirectory()) {
            throw new ExecutionException(IdeBundle.message("run.configuration.error.working.directory.not.directory"));
        }
    }

    protected void setupEnvironment(Map<String, String> environment) {
        environment.clear();

        if (myPassParentEnvironment) {
            environment.putAll(getParentEnvironment());
        }

        if (!myEnvParams.isEmpty()) {
            if (SystemInfo.isWindows) {
                THashMap<String, String> envVars = new THashMap<String, String>(CaseInsensitiveStringHashingStrategy.INSTANCE);
                envVars.putAll(environment);
                envVars.putAll(myEnvParams);
                environment.clear();
                environment.putAll(envVars);
            }
            else {
                environment.putAll(myEnvParams);
            }
        }
    }

    /**
     * Normally, double quotes in parameters are escaped so they arrive to a called program as-is.
     * But some commands (e.g. {@code 'cmd /c start "title" ...'}) should get they quotes non-escaped.
     * Wrapping a parameter by this method (instead of using quotes) will do exactly this.
     *
     * @see com.gome.maven.execution.util.ExecUtil#getTerminalCommand(String, String)
     */
    
    public static String inescapableQuote(String parameter) {
        return CommandLineUtil.specialQuote(parameter);
    }

    @Override
    public String toString() {
        return myExePath + " " + myProgramParams;
    }

    @Override
    public <T> T getUserData(final Key<T> key) {
        if (myUserData != null) {
            @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"}) final T t = (T)myUserData.get(key);
            return t;
        }
        return null;
    }

    @Override
    public <T> void putUserData(final Key<T> key,  final T value) {
        if (myUserData == null) {
            myUserData = ContainerUtil.newHashMap();
        }
        myUserData.put(key, value);
    }

    private static class MyTHashMap extends THashMap<String, String> {
        @Override
        public String put(String key, String value) {
            if (key == null || value == null) {
                LOG.error(new Exception("Nulls are not allowed"));
                return null;
            }
            if (key.isEmpty()) {
                // Windows: passing an environment variable with empty name causes "CreateProcess error=87, The parameter is incorrect"
                LOG.warn("Skipping environment variable with empty name, value: " + value);
                return null;
            }
            return super.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> map) {
            if (map != null) {
                super.putAll(map);
            }
        }
    }
}
