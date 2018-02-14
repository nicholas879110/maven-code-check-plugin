//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.whitelist;

import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.UriFilter;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

public class ProxyBypassListSelector extends ProxySelector {
    private ProxySelector delegate;
    private List<UriFilter> whiteListFilter;

    public ProxyBypassListSelector(List<UriFilter> whiteListFilter, ProxySelector proxySelector) {
        if (whiteListFilter == null) {
            throw new NullPointerException("Whitelist must not be null.");
        } else if (proxySelector == null) {
            throw new NullPointerException("ProxySelector must not be null.");
        } else {
            this.delegate = proxySelector;
            this.whiteListFilter = whiteListFilter;
        }
    }

    public ProxyBypassListSelector(String whiteList, ProxySelector proxySelector) {
        this((new DefaultWhiteListParser()).parseWhiteList(whiteList), proxySelector);
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        this.delegate.connectFailed(uri, sa, ioe);
    }

    public List<Proxy> select(URI uri) {
        Iterator i$ = this.whiteListFilter.iterator();

        UriFilter filter;
        do {
            if (!i$.hasNext()) {
                return this.delegate.select(uri);
            }

            filter = (UriFilter)i$.next();
        } while(!filter.accept(uri));

        return ProxyUtil.noProxyList();
    }
}
