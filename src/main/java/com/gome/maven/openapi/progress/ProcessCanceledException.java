package com.gome.maven.openapi.progress;

import com.gome.maven.util.SystemProperties;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:31
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class ProcessCanceledException extends RuntimeException {
    private static final boolean ourHasStackTraces = SystemProperties.getBooleanProperty("idea.is.internal", false) || SystemProperties.getBooleanProperty("idea.is.unit.test", false);

    public ProcessCanceledException() {
    }

    public ProcessCanceledException(Throwable cause) {
        super(cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        if (ourHasStackTraces) return super.fillInStackTrace();
        // https://wikis.oracle.com/display/HotSpotInternals/PerformanceTechniques
        // http://www.javaspecialists.eu/archive/Issue129.html
        // http://java-performance.info/throwing-an-exception-in-java-is-very-slow/
        return this;
    }
}
