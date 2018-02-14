//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.browser.firefox;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.File;

class LinuxFirefoxProfileSource implements FirefoxProfileSource {
    LinuxFirefoxProfileSource() {
    }

    public File getProfileFolder() {
        File userDir = new File(System.getProperty("user.home"));
        File cfgDir = new File(userDir, ".mozilla" + File.separator + "firefox" + File.separator);
        if (!cfgDir.exists()) {
            Logger.log(this.getClass(), LogLevel.DEBUG, "Firefox settings folder not found!", new Object[0]);
            return null;
        } else {
            File[] profiles = cfgDir.listFiles();
            if (profiles != null && profiles.length != 0) {
                File[] arr$ = profiles;
                int len$ = profiles.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    File p = arr$[i$];
                    if (p.getName().endsWith(".default")) {
                        Logger.log(this.getClass(), LogLevel.TRACE, "Firefox settings folder is {0}", new Object[]{p});
                        return p;
                    }
                }

                Logger.log(this.getClass(), LogLevel.TRACE, "Firefox settings folder is {0}", new Object[]{profiles[0]});
                return profiles[0];
            } else {
                Logger.log(this.getClass(), LogLevel.DEBUG, "Firefox settings folder not found!", new Object[0]);
                return null;
            }
        }
    }
}
