//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.direct;

import com.btr.proxy.util.ProxyUtil;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

public class NoProxySelector extends ProxySelector {
    private static NoProxySelector instance;

    private NoProxySelector() {
    }

    public static synchronized NoProxySelector getInstance() {
        if (instance == null) {
            instance = new NoProxySelector();
        }

        return instance;
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    }

    public List<Proxy> select(URI uri) {
        return ProxyUtil.noProxyList();
    }
}
