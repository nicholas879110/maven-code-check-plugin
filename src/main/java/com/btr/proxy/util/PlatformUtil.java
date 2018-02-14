//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.util;

import com.btr.proxy.util.Logger.LogLevel;

public class PlatformUtil {
    public PlatformUtil() {
    }

    public static PlatformUtil.Platform getCurrentPlattform() {
        String osName = System.getProperty("os.name");
        Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detecting platform. Name is: {0}", new Object[]{osName});
        if (osName.toLowerCase().contains("windows")) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Windows platform: {0}", new Object[]{osName});
            return PlatformUtil.Platform.WIN;
        } else if (osName.toLowerCase().contains("linux")) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Linux platform: {0}", new Object[]{osName});
            return PlatformUtil.Platform.LINUX;
        } else if (osName.startsWith("Mac OS")) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Mac OS platform: {0}", new Object[]{osName});
            return PlatformUtil.Platform.MAC_OS;
        } else if (osName.startsWith("SunOS")) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Solaris platform: {0}", new Object[]{osName});
            return PlatformUtil.Platform.SOLARIS;
        } else {
            return PlatformUtil.Platform.OTHER;
        }
    }

    public static PlatformUtil.Browser getDefaultBrowser() {
        if (getCurrentPlattform() == PlatformUtil.Platform.WIN) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Browser is InternetExplorer", new Object[0]);
            return PlatformUtil.Browser.IE;
        } else {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Browser Firefox. Fallback?", new Object[0]);
            return PlatformUtil.Browser.FIREFOX;
        }
    }

    public static PlatformUtil.Desktop getCurrentDesktop() {
        PlatformUtil.Platform platf = getCurrentPlattform();
        if (platf == PlatformUtil.Platform.WIN) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Windows desktop", new Object[0]);
            return PlatformUtil.Desktop.WIN;
        } else if (platf == PlatformUtil.Platform.MAC_OS) {
            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Mac OS desktop", new Object[0]);
            return PlatformUtil.Desktop.MAC_OS;
        } else {
            if (platf == PlatformUtil.Platform.LINUX || platf == PlatformUtil.Platform.SOLARIS || platf == PlatformUtil.Platform.OTHER) {
                if (isKDE()) {
                    Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected KDE desktop", new Object[0]);
                    return PlatformUtil.Desktop.KDE;
                }

                if (isGnome()) {
                    Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Gnome desktop", new Object[0]);
                    return PlatformUtil.Desktop.GNOME;
                }
            }

            Logger.log(PlatformUtil.class, LogLevel.TRACE, "Detected Unknown desktop", new Object[0]);
            return PlatformUtil.Desktop.OTHER;
        }
    }

    private static boolean isGnome() {
        return System.getenv("GNOME_DESKTOP_SESSION_ID") != null;
    }

    private static boolean isKDE() {
        return System.getenv("KDE_SESSION_VERSION") != null;
    }

    public static enum Browser {
        IE,
        FIREFOX;

        private Browser() {
        }
    }

    public static enum Desktop {
        WIN,
        KDE,
        GNOME,
        MAC_OS,
        OTHER;

        private Desktop() {
        }
    }

    public static enum Platform {
        WIN,
        LINUX,
        MAC_OS,
        SOLARIS,
        OTHER;

        private Platform() {
        }
    }
}
