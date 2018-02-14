package com.gome.maven.diagnostic;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author zhangliewei
 * @date 2017/12/29 17:21
 * @opyright(c) gome inc Gome Co.,LTD
 */
public abstract class LoggerRt {
    private interface Factory {
        LoggerRt getInstance(  final String category);
    }

    private static Factory ourFactory;

    private synchronized static Factory getFactory() {
        if (ourFactory == null) {
            try {
                ourFactory = new IdeaFactory();
            }
            catch (Throwable t) {
                ourFactory = new JavaFactory();
            }
        }
        return ourFactory;
    }

    
    public static LoggerRt getInstance(  final String category) {
        return getFactory().getInstance(category);
    }

    public void info(  final String message) {
        info(message, null);
    }

    public void info( final Throwable t) {
        info(t.getMessage(), t);
    }

    public void warn(  final String message) {
        warn(message, null);
    }

    public void warn( final Throwable t) {
        warn(t.getMessage(), t);
    }

    public void error(  final String message) {
        error(message, null);
    }

    public void error( final Throwable t) {
        error(t.getMessage(), t);
    }

    public abstract void info(  final String message,  final Throwable t);
    public abstract void warn(  final String message,  final Throwable t);
    public abstract void error(  final String message,  final Throwable t);

    private static class JavaFactory implements Factory {
        @Override
        public LoggerRt getInstance(  final String category) {
            final java.util.logging.Logger logger = Logger.getLogger(category);
            return new LoggerRt() {
                @Override
                public void info(  final String message,  final Throwable t) {
                    logger.log(Level.INFO, message, t);
                }

                @Override
                public void warn(  final String message,  final Throwable t) {
                    logger.log(Level.WARNING, message, t);
                }

                @Override
                public void error(  final String message,  final Throwable t) {
                    logger.log(Level.SEVERE, message, t);
                }
            };
        }
    }

    private static class IdeaFactory implements Factory {
        private final Method myGetInstance;
        private final Method myInfo;
        private final Method myWarn;
        private final Method myError;

        private IdeaFactory() throws Exception {
            final Class<?> loggerClass = Class.forName("com.gome.maven.openapi.diagnostic.Logger");
            myGetInstance = loggerClass.getMethod("getInstance", String.class);
            myGetInstance.setAccessible(true);
            myInfo = loggerClass.getMethod("info", String.class, Throwable.class);
            myInfo.setAccessible(true);
            myWarn = loggerClass.getMethod("warn", String.class, Throwable.class);
            myInfo.setAccessible(true);
            myError = loggerClass.getMethod("error", String.class, Throwable.class);
            myError.setAccessible(true);
        }

        @Override
        public LoggerRt getInstance(  final String category) {
            try {
                final Object logger = myGetInstance.invoke(null, category);
                return new LoggerRt() {
                    @Override
                    public void info(  final String message,  final Throwable t) {
                        try {
                            myInfo.invoke(logger, message, t);
                        }
                        catch (Exception ignored) { }
                    }

                    @Override
                    public void warn(  final String message,  final Throwable t) {
                        try {
                            myWarn.invoke(logger, message, t);
                        }
                        catch (Exception ignored) { }
                    }

                    @Override
                    public void error(  final String message,  final Throwable t) {
                        try {
                            myError.invoke(logger, message, t);
                        }
                        catch (Exception ignored) { }
                    }
                };
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}