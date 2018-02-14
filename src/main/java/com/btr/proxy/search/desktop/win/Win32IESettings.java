//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.win;

public class Win32IESettings {
    private boolean autoDetect;
    private String autoConfigUrl;
    private String proxy;
    private String proxyBypass;

    public Win32IESettings(boolean autoDetect, String autoConfigUrl, String proxy, String proxyBypass) {
        this.autoDetect = autoDetect;
        this.autoConfigUrl = autoConfigUrl;
        this.proxy = proxy;
        this.proxyBypass = proxyBypass;
    }

    public boolean isAutoDetect() {
        return this.autoDetect;
    }

    public String getAutoConfigUrl() {
        return this.autoConfigUrl;
    }

    public String getProxy() {
        return this.proxy;
    }

    public String getProxyBypass() {
        return this.proxyBypass;
    }
}
