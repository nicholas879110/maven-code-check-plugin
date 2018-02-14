package com.gome.maven.log;

import org.apache.maven.plugin.logging.Log;

/**
 * @author zhangliewei
 * @date 2017/12/26 17:35
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class LogInitializer {

    //扩展maven日志
    private static CodeCheckSystemStreamLog log = null;
    private static LogInitializer instance = null;

    private static void init() {
        if (log == null) {
            log = new CodeCheckSystemStreamLog();
        }
    }

    private LogInitializer() {
    }

    public static LogInitializer getInstance() {
        if (instance == null) {
            synchronized (instance) {
                instance = new LogInitializer();
                init();
            }
        }
        return instance;
    }

    public CodeCheckSystemStreamLog getLog() {
        return log;
    }

    private void setLog(CodeCheckSystemStreamLog log) {
        LogInitializer.log = log;
    }
}
