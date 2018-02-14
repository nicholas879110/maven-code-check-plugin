//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.util;

public class Logger {
    private static Logger.LogBackEnd backend;

    public Logger() {
    }

    public static Logger.LogBackEnd getBackend() {
        return backend;
    }

    public static void setBackend(Logger.LogBackEnd backend) {
        backend = backend;
    }

    public static void log(Class<?> clazz, Logger.LogLevel loglevel, String msg, Object... params) {
        if (backend != null) {
            backend.log(clazz, loglevel, msg, params);
        }

    }

    public static boolean isLogginEnabled(Logger.LogLevel logLevel) {
        return backend != null ? backend.isLogginEnabled(logLevel) : false;
    }

    public interface LogBackEnd {
        void log(Class<?> var1, Logger.LogLevel var2, String var3, Object... var4);

        boolean isLogginEnabled(Logger.LogLevel var1);
    }

    public static enum LogLevel {
        ERROR,
        WARNING,
        INFO,
        TRACE,
        DEBUG;

        private LogLevel() {
        }
    }
}
