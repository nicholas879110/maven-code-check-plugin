//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.win;

import java.io.File;
import java.io.IOException;

public class Win32ProxyUtils {
    public static final int WINHTTP_AUTO_DETECT_TYPE_DHCP = 1;
    public static final int WINHTTP_AUTO_DETECT_TYPE_DNS_A = 2;

    public Win32ProxyUtils() {
    }

    public native String winHttpDetectAutoProxyConfigUrl(int var1);

    native String winHttpGetDefaultProxyConfiguration();

    public native Win32IESettings winHttpGetIEProxyConfigForCurrentUser();

    public native String readUserHomedir();

    static {
        try {
            File libFile = DLLManager.findLibFile();
            System.load(libFile.getAbsolutePath());
            DLLManager.cleanupTempFiles();
        } catch (IOException var1) {
            throw new RuntimeException("Error loading dll" + var1.getMessage(), var1);
        }
    }
}
