package com.gome.maven.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:31
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static Throwable getRootCause(Throwable e) {
        while (true) {
            if (e.getCause() == null) return e;
            e = e.getCause();
        }
    }

    public static <T> T findCause(Throwable e, Class<T> klass) {
        while (e != null && !klass.isInstance(e)) {
            e = e.getCause();
        }
        return (T)e;
    }

    public static boolean causedBy(Throwable e, Class klass) {
        return findCause(e, klass) != null;
    }

    
    public static Throwable makeStackTraceRelative( Throwable th,  Throwable relativeTo) {
        StackTraceElement[] trace = th.getStackTrace();
        StackTraceElement[] rootTrace = relativeTo.getStackTrace();
        for (int i=0, len = Math.min(trace.length, rootTrace.length); i < len; i++) {
            if (trace[trace.length - i - 1].equals(rootTrace[rootTrace.length - i - 1])) continue;
            int newDepth = trace.length - i;
            th.setStackTrace(Arrays.asList(trace).subList(0, newDepth).toArray(new StackTraceElement[newDepth]));
            break;
        }
        return th;
    }

    
    public static String getThrowableText( Throwable aThrowable) {
        StringWriter stringWriter = new StringWriter();
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        PrintWriter writer = new PrintWriter(stringWriter);
        aThrowable.printStackTrace(writer);
        return stringWriter.getBuffer().toString();
    }

    
    public static String getThrowableText( Throwable aThrowable,   final String stackFrameSkipPattern) {
         final String prefix = "\tat ";
         final String prefixProxy = prefix + "$Proxy";
        final String prefixRemoteUtil = prefix + "com.gome.maven.execution.rmi.RemoteUtil";
        final String skipPattern = prefix + stackFrameSkipPattern;

        final StringWriter stringWriter = new StringWriter();
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        final PrintWriter writer = new PrintWriter(stringWriter) {
            boolean skipping = false;
            @Override
            public void println(final String x) {
                boolean curSkipping = skipping;
                if (x != null) {
                    if (!skipping && x.startsWith(skipPattern)) curSkipping = true;
                    else if (skipping && !x.startsWith(prefix)) curSkipping = false;
                    if (curSkipping && !skipping) {
                        super.println("\tin "+ stripPackage(x, skipPattern.length()));
                    }
                    skipping = curSkipping;
                    if (skipping) {
                        skipping = !x.startsWith(prefixRemoteUtil);
                        return;
                    }
                    if (x.startsWith(prefixProxy)) return;
                    super.println(x);
                }
            }
        };
        aThrowable.printStackTrace(writer);
        return stringWriter.getBuffer().toString();
    }

    private static String stripPackage(String x, int offset) {
        int idx = offset;
        while (idx > 0 && idx < x.length() && !Character.isUpperCase(x.charAt(idx))) {
            idx = x.indexOf('.', idx) + 1;
        }
        return x.substring(Math.max(idx, offset));
    }

    
//    public static String getUserStackTrace( Throwable aThrowable, Logger logger) {
//        final String result = getThrowableText(aThrowable, "com.gome.maven.");
//        if (!result.contains("\n\tat")) {
//            // no stack frames found
//            logger.error(aThrowable);
//        }
//        return result;
//    }


    public static String getMessage( Throwable e) {
        String result = e.getMessage();
         final String exceptionPattern = "Exception: ";
         final String errorPattern = "Error: ";

        while ((result == null || result.contains(exceptionPattern) || result.contains(errorPattern)) && e.getCause() != null) {
            e = e.getCause();
            result = e.getMessage();
        }

        if (result != null) {
            result = extractMessage(result, exceptionPattern);
            result = extractMessage(result, errorPattern);
        }

        return result;
    }

    
    private static String extractMessage( String result,  final String errorPattern) {
        if (result.lastIndexOf(errorPattern) >= 0) {
            result = result.substring(result.lastIndexOf(errorPattern) + errorPattern.length());
        }
        return result;
    }

    public static void rethrowUnchecked( Throwable t) {
        if (t != null) {
            if (t instanceof Error) throw (Error)t;
            if (t instanceof RuntimeException) throw (RuntimeException)t;
        }
    }

    public static void rethrowAll(Throwable t) throws Exception {
        if (t != null) {
            if (t instanceof Error) throw (Error)t;
            if (t instanceof RuntimeException) throw (RuntimeException)t;
            throw (Exception)t;
        }
    }
}

