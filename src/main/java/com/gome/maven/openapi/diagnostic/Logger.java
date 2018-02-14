package com.gome.maven.openapi.diagnostic;

import com.gome.maven.diagnostic.DefaultLogger;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.ExceptionUtil;
import org.apache.log4j.Level;

import java.lang.reflect.Constructor;

/**
 * @author zhangliewei
 * @date 2017/12/26 17:48
 * @opyright(c) gome inc Gome Co.,LTD
 */
public abstract class Logger {
    public interface Factory {
        Logger getLoggerInstance(String category);
    }

    private static class DefaultFactory implements Factory {
        @Override
        public Logger getLoggerInstance(String category) {
            return new DefaultLogger(category);
        }
    }

    private static Factory ourFactory = new DefaultFactory();

    public static void setFactory(Class<? extends Factory> factory) {
        if (isInitialized()) {
            if (factory.isInstance(ourFactory)) {
                return;
            }

            //noinspection UseOfSystemOutOrSystemErr
            System.out.println("Changing log factory\n" + ExceptionUtil.getThrowableText(new Throwable()));
        }

        try {
            Constructor<? extends Factory> constructor = factory.getDeclaredConstructor();
            constructor.setAccessible(true);
            ourFactory = constructor.newInstance();
        }
        catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static boolean isInitialized() {
        return !(ourFactory instanceof DefaultFactory);
    }

    public static Logger getInstance(String category) {
        return ourFactory.getLoggerInstance(category);
    }


    public static Logger getInstance(Class cl) {
        return getInstance("#" + cl.getName());
    }

    public abstract boolean isDebugEnabled();

    public abstract void debug( String message);

    public abstract void debug( Throwable t);

    public abstract void debug( String message,  Throwable t);

    public void debug( String message, Object... details) {
        if (isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            for (Object detail : details) {
                sb.append(String.valueOf(detail));
            }
            debug(sb.toString());
        }
    }

    public void info( Throwable t) {
        info(t.getMessage(), t);
    }

    public abstract void info( String message);

    public abstract void info( String message,  Throwable t);

    public void warn( String message) {
        warn(message, null);
    }

    public void warn( Throwable t) {
        warn(t.getMessage(), t);
    }

    public abstract void warn( String message,  Throwable t);

    public void error( String message) {
        error(message, new Throwable(), ArrayUtil.EMPTY_STRING_ARRAY);
    }
    public void error(Object message) {
        error(String.valueOf(message));
    }

    public void error( String message, Attachment... attachments) {
        error(message);
    }

    public void error( String message,  String... details) {
        error(message, new Throwable(), details);
    }

    public void error( String message,  Throwable e) {
        error(message, e, ArrayUtil.EMPTY_STRING_ARRAY);
    }

    public void error( Throwable t) {
        error(t.getMessage(), t, ArrayUtil.EMPTY_STRING_ARRAY);
    }

    public abstract void error( String message,  Throwable t,   String... details);

     // wrong, but avoid quite a few warnings in the code
    public boolean assertTrue(boolean value,   Object message) {
        if (!value) {
             String resultMessage = "Assertion failed";
            if (message != null) resultMessage += ": " + message;
            error(resultMessage, new Throwable());
        }

        //noinspection Contract
        return value;
    }

     // wrong, but avoid quite a few warnings in the code
    public boolean assertTrue(boolean value) {
        return value || assertTrue(false, null);
    }

    public abstract void setLevel(Level level);


}
