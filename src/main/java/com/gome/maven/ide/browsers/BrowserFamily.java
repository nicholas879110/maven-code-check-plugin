package com.gome.maven.ide.browsers;

//import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
//import com.gome.maven.ide.browsers.chrome.ChromeSettings;
//import com.gome.maven.ide.browsers.firefox.FirefoxSettings;
//import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.util.SystemInfo;

import javax.swing.*;

public enum BrowserFamily /*implements Iconable*/ {;
//    CHROME(IdeBundle.message("browsers.chrome"), "chrome", "google-chrome", "Google Chrome", AllIcons.Xml.Browsers.Chrome16) {
//        @Override
//        public BrowserSpecificSettings createBrowserSpecificSettings() {
//            return new ChromeSettings();
//        }
//    },
//    FIREFOX(IdeBundle.message("browsers.firefox"), "firefox", "firefox", "Firefox", AllIcons.Xml.Browsers.Firefox16) {
//        @Override
//        public BrowserSpecificSettings createBrowserSpecificSettings() {
//            return new FirefoxSettings();
//        }
//    },
//    EXPLORER(IdeBundle.message("browsers.explorer"), "iexplore", null, null, AllIcons.Xml.Browsers.Explorer16),
//    OPERA(IdeBundle.message("browsers.opera"), "opera", "opera", "Opera", AllIcons.Xml.Browsers.Opera16),
//    SAFARI(IdeBundle.message("browsers.safari"), "safari", null, "Safari", AllIcons.Xml.Browsers.Safari16);

    private final String myName;
    private final String myWindowsPath;
    private final String myUnixPath;
    private final String myMacPath;
    private final Icon myIcon;

    BrowserFamily( String name,
                   String windowsPath,
                  String unixPath,
                  String macPath,
                   Icon icon) {
        myName = name;
        myWindowsPath = windowsPath;
        myUnixPath = unixPath;
        myMacPath = macPath;
        myIcon = icon;
    }

   
    public BrowserSpecificSettings createBrowserSpecificSettings() {
        return null;
    }

   
    public String getExecutionPath() {
        if (SystemInfo.isWindows) {
            return myWindowsPath;
        }
        else if (SystemInfo.isMac) {
            return myMacPath;
        }
        else {
            return myUnixPath;
        }
    }

    public String getName() {
        return myName;
    }

    public Icon getIcon() {
        return myIcon;
    }

    @Override
    public String toString() {
        return myName;
    }

//    @Override
//    public Icon getIcon(@IconFlags int flags) {
//        return getIcon();
//    }
}