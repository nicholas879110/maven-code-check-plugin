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
package com.gome.maven.execution.util;

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.process.CapturingProcessHandler;
import com.gome.maven.execution.process.ProcessOutput;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.Function;


import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class ExecUtil {
    private static final NotNullLazyValue<Boolean> hasGkSudo = new NotNullLazyValue<Boolean>() {
        
        @Override
        protected Boolean compute() {
            return new File("/usr/bin/gksudo").canExecute();
        }
    };

    private static final NotNullLazyValue<Boolean> hasKdeSudo = new NotNullLazyValue<Boolean>() {
        
        @Override
        protected Boolean compute() {
            return new File("/usr/bin/kdesudo").canExecute();
        }
    };

    private static final NotNullLazyValue<Boolean> hasPkExec = new NotNullLazyValue<Boolean>() {
        
        @Override
        protected Boolean compute() {
            return new File("/usr/bin/pkexec").canExecute();
        }
    };

    private static final NotNullLazyValue<Boolean> hasGnomeTerminal = new NotNullLazyValue<Boolean>() {
        
        @Override
        protected Boolean compute() {
            return new File("/usr/bin/gnome-terminal").canExecute();
        }
    };

    private static final NotNullLazyValue<Boolean> hasKdeTerminal = new NotNullLazyValue<Boolean>() {
        
        @Override
        protected Boolean compute() {
            return new File("/usr/bin/konsole").canExecute();
        }
    };

    private static final NotNullLazyValue<Boolean> hasXTerm = new NotNullLazyValue<Boolean>() {
        
        @Override
        protected Boolean compute() {
            return new File("/usr/bin/xterm").canExecute();
        }
    };

    private ExecUtil() { }

    /**
     * @deprecated Use "new GeneralCommandLine(command).createProcess().waitFor()".
     */
    @SuppressWarnings({"deprecation", "Contract"})
    public static int execAndGetResult(final String... command) throws ExecutionException, InterruptedException {
        assert command != null && command.length > 0;
        return execAndGetResult(Arrays.asList(command));
    }

    /**
     * @deprecated Use "new GeneralCommandLine(command).createProcess().waitFor()".
     */
    public static int execAndGetResult( final List<String> command) throws ExecutionException, InterruptedException {
        return new GeneralCommandLine(command).createProcess().waitFor();
    }

    
    public static String loadTemplate( ClassLoader loader,  String templateName,  Map<String, String> variables) throws IOException {
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") InputStream stream = loader.getResourceAsStream(templateName);
        if (stream == null) {
            throw new IOException("Template '" + templateName + "' not found by " + loader);
        }

        String template = FileUtil.loadTextAndClose(new InputStreamReader(stream, CharsetToolkit.UTF8));
        if (variables == null || variables.size() == 0) {
            return template;
        }

        StringBuilder buffer = new StringBuilder(template);
        for (Map.Entry<String, String> var : variables.entrySet()) {
            String name = var.getKey();
            int pos = buffer.indexOf(name);
            if (pos >= 0) {
                buffer.replace(pos, pos + name.length(), var.getValue());
            }
        }
        return buffer.toString();
    }

    
    public static File createTempExecutableScript( String prefix,  String suffix,  String content) throws IOException, ExecutionException {
        File tempDir = new File(PathManager.getTempPath());
        File tempFile = FileUtil.createTempFile(tempDir, prefix, suffix, true, true);
        FileUtil.writeToFile(tempFile, content.getBytes(CharsetToolkit.UTF8));
        if (!tempFile.setExecutable(true, true)) {
            throw new ExecutionException("Failed to make temp file executable: " + tempFile);
        }
        return tempFile;
    }

    
    public static String getOsascriptPath() {
        return "/usr/bin/osascript";
    }

    
    public static String getOpenCommandPath() {
        return "/usr/bin/open";
    }

    
    public static String getWindowsShellName() {
        return SystemInfo.isWin2kOrNewer ? "cmd.exe" : "command.com";
    }

    
    public static ProcessOutput execAndGetOutput( final GeneralCommandLine commandLine) throws ExecutionException {
        final Process process = commandLine.createProcess();
        final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
        return processHandler.runProcess();
    }

    /**
     * @deprecated Use {@link #execAndGetOutput(com.gome.maven.execution.configurations.GeneralCommandLine)} instead.
     */
    
    public static ProcessOutput execAndGetOutput( final List<String> command,
                                                  final String workDir) throws ExecutionException {
        final Process process = new GeneralCommandLine(command).withWorkDirectory(workDir).createProcess();
        final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
        return processHandler.runProcess();
    }

    
    public static String execAndReadLine( final GeneralCommandLine commandLine) {
        try {
            return readFirstLine(commandLine.createProcess().getInputStream(), commandLine.getCharset());
        }
        catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @deprecated Use {@link #execAndReadLine(com.gome.maven.execution.configurations.GeneralCommandLine)} instead.
     */
    @SuppressWarnings("deprecation")
    
    public static String execAndReadLine(final String... command) {
        return execAndReadLine(null, command);
    }

    /**
     * @deprecated Use {@link #execAndReadLine(com.gome.maven.execution.configurations.GeneralCommandLine)} instead.
     */
    
    public static String execAndReadLine( Charset charset, final String... command) {
        try {
            return readFirstLine(new GeneralCommandLine(command).createProcess().getInputStream(), charset);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    
    public static String readFirstLine( InputStream inputStream,  Charset charset) {
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        BufferedReader reader = new BufferedReader(charset == null ? new InputStreamReader(inputStream) : new InputStreamReader(inputStream, charset));
        try {
            try {
                return reader.readLine();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Run the command with superuser privileges using safe escaping and quoting.
     *
     * No shell substitutions, input/output redirects, etc. in the command are applied.
     *
     * @param commandLine the command line to execute
     * @param prompt the prompt string for the users
     * @return the results of running the process
     */
    
    public static Process sudo( final GeneralCommandLine commandLine,
                                final String prompt) throws ExecutionException,IOException {
        final File workDir = commandLine.getWorkDirectory();
        final List<String> command = new ArrayList<String>();
        command.add(commandLine.getExePath());
        command.addAll(commandLine.getParametersList().getList());

        final Map<String, String> environment = commandLine.getEnvironment();
        if (SystemInfo.isMac) {
            final String escapedCommandLine = StringUtil.join(command, new Function<String, String>() {
                @Override
                public String fun(String s) {
                    return escapeAppleScriptArgument(s);
                }
            }, " & \" \" & ");
            final String escapedScript = "do shell script " + escapedCommandLine + " with administrator privileges";
            return new GeneralCommandLine(getOsascriptPath(), "-e", escapedScript)
                    .withWorkDirectory(workDir)
                    .withEnvironment(environment)
                    .createProcess();
        }
        else if ("root".equals(System.getenv("USER"))) {
            return new GeneralCommandLine(command).withWorkDirectory(workDir).withEnvironment(environment).createProcess();
        }
        else if (hasGkSudo.getValue()) {
            final List<String> sudoCommand = new ArrayList<String>();
            sudoCommand.addAll(Arrays.asList("gksudo", "--message", prompt, "--"));
            sudoCommand.addAll(command);
            return new GeneralCommandLine(sudoCommand).withWorkDirectory(workDir).withEnvironment(environment).createProcess();
        }
        else if (hasKdeSudo.getValue()) {
            final List<String> sudoCommand = new ArrayList<String>();
            sudoCommand.addAll(Arrays.asList("kdesudo", "--comment", prompt, "--"));
            sudoCommand.addAll(command);
            return new GeneralCommandLine(sudoCommand).withWorkDirectory(workDir).withEnvironment(environment).createProcess();
        }
        else if (hasPkExec.getValue()) {
            final List<String> sudoCommand = new ArrayList<String>();
            sudoCommand.add("pkexec");
            sudoCommand.addAll(command);
            return new GeneralCommandLine(sudoCommand).withWorkDirectory(workDir).withEnvironment(environment).createProcess();
        }
        else if (SystemInfo.isUnix && hasTerminalApp()) {
            final String escapedCommandLine = StringUtil.join(command, new Function<String, String>() {
                @Override
                public String fun(String s) {
                    return escapeUnixShellArgument(s);
                }
            }, " ");
            final File script = createTempExecutableScript(
                    "sudo", ".sh",
                    "#!/bin/sh\n" +
                            "echo " + escapeUnixShellArgument(prompt) + "\n" +
                            "echo\n" +
                            "sudo -- " + escapedCommandLine + "\n" +
                            "STATUS=$?\n" +
                            "echo\n" +
                            "read -p \"Press Enter to close this window...\" TEMP\n" +
                            "exit $STATUS\n");
            return new GeneralCommandLine(getTerminalCommand("Install", script.getAbsolutePath()))
                    .withWorkDirectory(workDir)
                    .withEnvironment(environment)
                    .createProcess();
        }

        throw new UnsupportedSystemException();
    }

    
    public static ProcessOutput sudoAndGetOutput( final GeneralCommandLine commandLine,
                                                  final String prompt) throws IOException, ExecutionException {
        final Process process = sudo(commandLine, prompt);
        final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
        return processHandler.runProcess();
    }

    /**
     * @deprecated Use {@link #sudoAndGetOutput(com.gome.maven.execution.configurations.GeneralCommandLine, String)} instead.
     */
    @SuppressWarnings("UnusedDeclaration")
    
    public static ProcessOutput sudoAndGetOutput( List<String> command,  String prompt,
                                                  String workDir) throws IOException, ExecutionException {
        final Process process = sudo(new GeneralCommandLine(command).withWorkDirectory(workDir), prompt);
        final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
        return processHandler.runProcess();
    }

    
    private static String escapeAppleScriptArgument( String arg) {
        return "quoted form of \"" + arg.replace("\"", "\\\"") + "\"";
    }

    
    private static String escapeUnixShellArgument( String arg) {
        return "'" + arg.replace("'", "'\"'\"'") + "'";
    }

    /** @deprecated relies on platform-dependent escaping, use {@link #sudoAndGetOutput(List, String, String)} instead (to remove in IDEA 14) */
    @SuppressWarnings({"UnusedDeclaration", "deprecation"})
    public static ProcessOutput sudoAndGetOutput( String scriptPath,  String prompt) throws IOException, ExecutionException {
        return sudoAndGetOutput(scriptPath, prompt, null);
    }

    /** @deprecated relies on platform-dependent escaping, use {@link #sudoAndGetOutput(List, String, String)} instead (to remove in IDEA 14) */
    @SuppressWarnings("deprecation")
    
    public static ProcessOutput sudoAndGetOutput( String scriptPath,
                                                  String prompt,
                                                  String workDir) throws IOException, ExecutionException {
        if (SystemInfo.isMac) {
            final String script = "do shell script \"" + scriptPath + "\" with administrator privileges";
            return execAndGetOutput(Arrays.asList(getOsascriptPath(), "-e", script), workDir);
        }
        else if ("root".equals(System.getenv("USER"))) {
            return execAndGetOutput(Collections.singletonList(scriptPath), workDir);
        }
        else if (hasKdeSudo.getValue()) {
            return execAndGetOutput(Arrays.asList("kdesudo", "--comment", prompt, scriptPath), workDir);
        }
        else if (hasGkSudo.getValue()) {
            return execAndGetOutput(Arrays.asList("gksudo", "--message", prompt, scriptPath), workDir);
        }
        else if (hasPkExec.getValue()) {
            return execAndGetOutput(Arrays.asList("pkexec", scriptPath), workDir);
        }
        else if (SystemInfo.isUnix && hasTerminalApp()) {
            final File sudo = createTempExecutableScript("sudo", ".sh",
                    "#!/bin/sh\n" +
                            "echo \"" + prompt + "\"\n" +
                            "echo\n" +
                            "sudo " + scriptPath + "\n" +
                            "STATUS=$?\n" +
                            "echo\n" +
                            "read -p \"Press Enter to close this window...\" TEMP\n" +
                            "exit $STATUS\n");
            return execAndGetOutput(getTerminalCommand("Install", sudo.getAbsolutePath()), workDir);
        }

        throw new UnsupportedSystemException();
    }

    /** @deprecated relies on platform-dependent escaping, use {@link #sudoAndGetOutput(List, String, String)} instead (to remove in IDEA 14) */
    @SuppressWarnings({"UnusedDeclaration", "deprecation"})
    public static int sudoAndGetResult( String scriptPath,  String prompt) throws IOException, ExecutionException {
        return sudoAndGetOutput(scriptPath, prompt, null).getExitCode();
    }

    public static boolean hasTerminalApp() {
        return SystemInfo.isWindows || SystemInfo.isMac || hasKdeTerminal.getValue() || hasGnomeTerminal.getValue() || hasXTerm.getValue();
    }

    
    public static List<String> getTerminalCommand( String title,  String command) {
        if (SystemInfo.isWindows) {
            title = title != null ? title.replace("\"", "'") : "";
            return Arrays.asList(getWindowsShellName(), "/c", "start", GeneralCommandLine.inescapableQuote(title), command);
        }
        else if (SystemInfo.isMac) {
            return Arrays.asList(getOpenCommandPath(), "-a", "Terminal", command);  // todo[r.sh] title?
        }
        else if (hasKdeTerminal.getValue()) {
            return Arrays.asList("/usr/bin/konsole", "-e", command);  // todo[r.sh] title?
        }
        else if (hasGnomeTerminal.getValue()) {
            return title != null ? Arrays.asList("/usr/bin/gnome-terminal", "-t", title, "-x", command)
                    : Arrays.asList("/usr/bin/gnome-terminal", "-x", command);
        }
        else if (hasXTerm.getValue()) {
            return title != null ? Arrays.asList("/usr/bin/xterm", "-T", title, "-e", command)
                    : Arrays.asList("/usr/bin/xterm", "-e", command);
        }

        throw new UnsupportedSystemException();
    }

    public static class UnsupportedSystemException extends UnsupportedOperationException {
        public UnsupportedSystemException() {
            super("Unsupported OS/desktop: " + SystemInfo.OS_NAME + '/' + SystemInfo.SUN_DESKTOP);
        }
    }
}
