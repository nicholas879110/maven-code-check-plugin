////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by Fernflower decompiler)
////
//
//package com.pty4j;
//
//import com.pty4j.unix.Pty;
//import com.pty4j.unix.UnixPtyProcess;
//import com.pty4j.util.PtyUtil;
//import com.pty4j.windows.CygwinPtyProcess;
//import com.pty4j.windows.WinPtyProcess;
//import com.sun.jna.Platform;
//import com.sun.jna.platform.win32.Advapi32Util;
//import java.io.File;
//import java.io.IOException;
//import java.util.Map;
//
//public abstract class PtyProcess extends Process {
//    public PtyProcess() {
//    }
//
//    public abstract boolean isRunning();
//
//    public abstract void setWinSize(WinSize var1);
//
//    public abstract WinSize getWinSize() throws IOException;
//
//    public static PtyProcess exec(String[] command) throws IOException {
//        return exec(command, (Map)null);
//    }
//
//    public static PtyProcess exec(String[] command, Map<String, String> environment) throws IOException {
//        return exec(command, environment, (String)null, false, false, (File)null);
//    }
//
//    public static PtyProcess exec(String[] command, Map<String, String> environment, String workingDirectory) throws IOException {
//        return exec(command, environment, workingDirectory, false, false, (File)null);
//    }
//
//    /** @deprecated */
//    @Deprecated
//    public static PtyProcess exec(String[] command, String[] environment) throws IOException {
//        return exec(command, (String[])environment, (String)null, false);
//    }
//
//    /** @deprecated */
//    @Deprecated
//    public static PtyProcess exec(String[] command, String[] environment, String workingDirectory, boolean console) throws IOException {
//        return (PtyProcess)(Platform.isWindows() ? new WinPtyProcess(command, environment, workingDirectory, console) : new UnixPtyProcess(command, environment, workingDirectory, new Pty(console), console ? new Pty() : null));
//    }
//
//    public static PtyProcess exec(String[] command, Map<String, String> environment, String workingDirectory, boolean console) throws IOException {
//        return exec(command, environment, workingDirectory, console, false, (File)null);
//    }
//
//    public static PtyProcess exec(String[] command, Map<String, String> environment, String workingDirectory, boolean console, boolean cygwin, File logFile) throws IOException {
//        if (Platform.isWindows()) {
//            return (PtyProcess)(cygwin ? new CygwinPtyProcess(command, environment, workingDirectory, logFile, console) : new WinPtyProcess(command, Advapi32Util.getEnvironmentBlock(environment), workingDirectory, console));
//        } else {
//            return new UnixPtyProcess(command, PtyUtil.toStringArray(environment), workingDirectory, new Pty(console), console ? new Pty() : null);
//        }
//    }
//}
