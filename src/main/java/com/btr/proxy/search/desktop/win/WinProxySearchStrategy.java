//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.win;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.browser.ie.IEProxySearchStrategy;
import com.btr.proxy.util.ProxyException;
import java.net.ProxySelector;

public class WinProxySearchStrategy implements ProxySearchStrategy {
    public WinProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() throws ProxyException {
        return (new IEProxySearchStrategy()).getProxySelector();
    }

    public Win32IESettings readSettings() {
        return (new IEProxySearchStrategy()).readSettings();
    }
}
