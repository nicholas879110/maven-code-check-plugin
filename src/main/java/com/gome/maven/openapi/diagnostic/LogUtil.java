//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.diagnostic;

import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.util.ExceptionUtil;
import java.io.IOException;

public class LogUtil {
    private LogUtil() {
    }

    public static String objectAndClass( Object o) {
        return o != null ? o + " (" + o.getClass().getName() + ")" : "null";
    }

    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format(format, args));
        }

    }

    public static String getProcessList() {
        try {
            Process process = (new ProcessBuilder(new String[0])).command(SystemInfo.isWindows ? new String[]{System.getenv("windir") + "\\system32\\tasklist.exe", "/v"} : new String[]{"ps", "a"}).redirectErrorStream(true).start();
            return FileUtil.loadTextAndClose(process.getInputStream());
        } catch (IOException var1) {
            return ExceptionUtil.getThrowableText(var1);
        }
    }
}
