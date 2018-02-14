//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.fixed;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

public class FixedSocksSelector extends FixedProxySelector {
    public FixedSocksSelector(String proxyHost, int proxyPort) {
        super(new Proxy(Type.SOCKS, InetSocketAddress.createUnresolved(proxyHost, proxyPort)));
    }
}
