//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.fixed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixedProxySelector extends ProxySelector {
    private final List<Proxy> proxyList;

    public FixedProxySelector(Proxy proxy) {
        List<Proxy> list = new ArrayList(1);
        list.add(proxy);
        this.proxyList = Collections.unmodifiableList(list);
    }

    public FixedProxySelector(String proxyHost, int proxyPort) {
        this(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, proxyPort)));
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    }

    public List<Proxy> select(URI uri) {
        return this.proxyList;
    }
}
