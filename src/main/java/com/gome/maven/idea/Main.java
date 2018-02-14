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
package com.gome.maven.idea;

import com.gome.maven.ide.Bootstrap;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.SystemInfoRt;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.Restarter;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.io.File.pathSeparator;

public class Main {
    public static final int NO_GRAPHICS = 1;
    public static final int UPDATE_FAILED = 2;
    public static final int STARTUP_EXCEPTION = 3;
    public static final int JDK_CHECK_FAILED = 4;
    public static final int DIR_CHECK_FAILED = 5;
    public static final int INSTANCE_CHECK_FAILED = 6;
    public static final int LICENSE_ERROR = 7;
    public static final int PLUGIN_ERROR = 8;

    private static final String AWT_HEADLESS = "java.awt.headless";
    private static final String PLATFORM_PREFIX_PROPERTY = "idea.platform.prefix";
    private static final String[] NO_ARGS = {};

    private static boolean isHeadless;
    private static boolean isCommandLine;

    private Main() { }

    @SuppressWarnings("MethodNamesDifferingOnlyByCase")
    public static void main(String[] args) {
        if (args.length == 1 && "%f".equals(args[0])) {
            args = NO_ARGS;
        }

//        setFlags(args);
//
//        if (isHeadless()) {
//            System.setProperty(AWT_HEADLESS, Boolean.TRUE.toString());
//        }
//        else if (GraphicsEnvironment.isHeadless()) {
//            showMessage("Startup Error", "Unable to detect graphics environment", true);
//            System.exit(NO_GRAPHICS);
//        }
//        else if (args.length == 0) {
//            try {
//                installPatch();
//            }
//            catch (Throwable t) {
//                showMessage("Update Failed", t);
//                System.exit(UPDATE_FAILED);
//            }
//        }

        try {
            Bootstrap.main(args, Main.class.getName() + "Impl", "start");
        }
        catch (Throwable t) {
            showMessage("Start Failed", t);
            System.exit(STARTUP_EXCEPTION);
        }
    }

    public static boolean isHeadless() {
        return isHeadless;
    }

    public static boolean isCommandLine() {
        return isCommandLine;
    }

    public static void setFlags(String[] args) {
        isHeadless = isHeadless(args);
        isCommandLine = isCommandLine(args);
    }

    private static boolean isHeadless(String[] args) {
        if (Boolean.valueOf(System.getProperty(AWT_HEADLESS))) {
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        String firstArg = args[0];
        return Comparing.strEqual(firstArg, "ant") ||
                Comparing.strEqual(firstArg, "duplocate") ||
                Comparing.strEqual(firstArg, "traverseUI") ||
                (firstArg.length() < 20 && firstArg.endsWith("inspect"));
    }

    private static boolean isCommandLine(String[] args) {
        if (isHeadless()) return true;
        return args.length > 0 && Comparing.strEqual(args[0], "diff");
    }

    public static boolean isUITraverser(final String[] args) {
        return args.length > 0 && Comparing.strEqual(args[0], "traverseUI");
    }

    private static boolean checkBundledJava(File java) throws Exception {
        String[] command = new String[]{java.getPath(), "-version"};
        try {
            Process process = Runtime.getRuntime().exec(command);
            String line = (new BufferedReader(new InputStreamReader(process.getErrorStream()))).readLine();
            if (line != null && (line.toLowerCase().startsWith("java version") || (line.toLowerCase().startsWith("openjdk version")))){
                int pos = line.indexOf('.');
                if (pos > 0){
                    int majorVersion = Integer.parseInt(line.substring(pos-1, pos));
                    int minorVersion = Integer.parseInt(line.substring(pos+1, pos+2));
                    if (majorVersion > 1 || minorVersion > 5) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("updater: the java: " + command[0] + " is invalid.");
        }
        return false;
    }

    private static String getBundledJava(String javaHome) throws Exception {
        String javaHomeCopy = System.getProperty("user.home") + "/." + System.getProperty("idea.paths.selector") + "/restart/jre";
        File javaCopy = SystemInfoRt.isWindows ? new File(javaHomeCopy + "/bin/java.exe") : new File(javaHomeCopy + "/bin/java");
        if (javaCopy != null && javaCopy.isFile() && checkBundledJava(javaCopy)) {
            javaHome = javaHomeCopy;
        }
        if (javaHome != javaHomeCopy) {
            File javaHomeCopyDir = new File(javaHomeCopy);
            if (javaHomeCopyDir.exists()) FileUtil.delete(javaHomeCopyDir);
            System.out.println("Updater: java: " + javaHome + " copied to " + javaHomeCopy);
            FileUtil.copyDir(new File(javaHome), javaHomeCopyDir);
            javaHome = javaHomeCopy;
        }
        return javaHome;
    }

    private static String getJava() throws Exception {
        String javaHome = System.getProperty("java.home");
        if (javaHome.toLowerCase().startsWith(PathManager.getHomePath().toLowerCase())) {
            System.out.println("Updater: uses bundled java.");
            javaHome = getBundledJava(javaHome);
        }
        return javaHome + "/bin/java";
    }

    private static void installPatch() throws Exception {
        String platform = System.getProperty(PLATFORM_PREFIX_PROPERTY, "idea");
        String patchFileName = ("jetbrains.patch.jar." + platform).toLowerCase(Locale.US);
        String tempDir = System.getProperty("java.io.tmpdir");

        // always delete previous patch copy
        File patchCopy = new File(tempDir, patchFileName + "_copy");
        File log4jCopy = new File(tempDir, "log4j.jar." + platform + "_copy");
        File jnaUtilsCopy = new File(tempDir, "jna-utils.jar." + platform + "_copy");
        File jnaCopy = new File(tempDir, "jna.jar." + platform + "_copy");
        if (!FileUtilRt.delete(patchCopy) || !FileUtilRt.delete(log4jCopy) || !FileUtilRt.delete(jnaUtilsCopy) || !FileUtilRt.delete(jnaCopy)) {
            throw new IOException("Cannot delete temporary files in " + tempDir);
        }

        File patch = new File(tempDir, patchFileName);
        if (!patch.exists()) return;

        File log4j = new File(PathManager.getLibPath(), "log4j.jar");
        if (!log4j.exists()) throw new IOException("Log4J is missing: " + log4j);

        File jnaUtils = new File(PathManager.getLibPath(), "jna-utils.jar");
        if (!jnaUtils.exists()) throw new IOException("jna-utils.jar is missing: " + jnaUtils);

        File jna = new File(PathManager.getLibPath(), "jna.jar");
        if (!jna.exists()) throw new IOException("jna is missing: " + jna);

        copyFile(patch, patchCopy, true);
        copyFile(log4j, log4jCopy, false);
        copyFile(jna, jnaCopy, false);
        copyFile(jnaUtils, jnaUtilsCopy, false);

        int status = 0;
        if (Restarter.isSupported()) {
            List<String> args = new ArrayList<String>();

            if (SystemInfoRt.isWindows) {
                File launcher = new File(PathManager.getBinPath(), "VistaLauncher.exe");
                args.add(Restarter.createTempExecutable(launcher).getPath());
                Restarter.createTempExecutable(new File(PathManager.getBinPath(), "restarter.exe"));
            }

            //noinspection SpellCheckingInspection
            String java = getJava();
            Collections.addAll(args,
                    java,
                    "-Xmx750m",
                    "-Djna.nosys=true",
                    "-Djna.boot.library.path=",
                    "-Djna.debug_load=true",
                    "-Djna.debug_load.jna=true",
                    "-classpath",
                    patchCopy.getPath() + pathSeparator + log4jCopy.getPath() + pathSeparator + jnaCopy.getPath() + pathSeparator + jnaUtilsCopy.getPath(),
                    "-Djava.io.tmpdir=" + tempDir,
                    "-Didea.updater.log=" + PathManager.getLogPath(),
                    "-Dswing.defaultlaf=" + UIManager.getSystemLookAndFeelClassName(),
                    "com.gome.maven.updater.Runner",
                    "install",
                    PathManager.getHomePath());

            status = Restarter.scheduleRestart(ArrayUtilRt.toStringArray(args));
        }
        else {
            String message = "Patch update is not supported - please do it manually";
            showMessage("Update Error", message, true);
        }

        System.exit(status);
    }

    private static void copyFile(File original, File copy, boolean move) throws IOException {
        if (move) {
            if (!original.renameTo(copy) || !FileUtilRt.delete(original)) {
                throw new IOException("Cannot create temporary file: " + copy);
            }
        }
        else {
            FileUtilRt.copy(original, copy);
            if (!copy.exists()) {
                throw new IOException("Cannot create temporary file: " + copy);
            }
        }
    }

    public static void showMessage(String title, Throwable t) {
        StringWriter message = new StringWriter();
        message.append("Internal error. Please report to https://");
        boolean studio = "AndroidStudio".equalsIgnoreCase(System.getProperty(PLATFORM_PREFIX_PROPERTY));
        message.append(studio ? "code.google.com/p/android/issues" : "youtrack.jetbrains.com");
        message.append("\n\n");
        t.printStackTrace(new PrintWriter(message));
        showMessage(title, message.toString(), true);
    }

    @SuppressWarnings({"UseJBColor", "UndesirableClassUsage", "UseOfSystemOutOrSystemErr"})
    public static void showMessage(String title, String message, boolean error) {
        PrintStream stream = error ? System.err : System.out;
        stream.println("\n" + title + ": " + message);

        boolean headless = isCommandLine() || GraphicsEnvironment.isHeadless();
        if (!headless) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Throwable ignore) { }

            try {
                JTextPane textPane = new JTextPane();
                textPane.setEditable(false);
                textPane.setText(message.replaceAll("\t", "    "));
                textPane.setBackground(UIUtil.getPanelBackground());
                textPane.setCaretPosition(0);
                JScrollPane scrollPane = new JScrollPane(
                        textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setBorder(null);

                int maxHeight = Math.min(JBUI.scale(600), Toolkit.getDefaultToolkit().getScreenSize().height - 150);
                Dimension component = scrollPane.getPreferredSize();
                if (component.height >= maxHeight) {
                    Object setting = UIManager.get("ScrollBar.width");
                    int width = setting instanceof Integer ? ((Integer)setting).intValue() : 20;
                    scrollPane.setPreferredSize(new Dimension(component.width + width, maxHeight));
                }

                int type = error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE;
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), scrollPane, title, type);
            }
            catch (Throwable t) {
                stream.println("\nAlso, an UI exception occurred on attempt to show above message:");
                t.printStackTrace(stream);
            }
        }
    }
}
