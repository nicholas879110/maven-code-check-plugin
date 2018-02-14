//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.misc;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyListFallbackSelector extends ProxySelector {
    private static final int DEFAULT_RETRY_DELAY = 600000;
    private ProxySelector delegate;
    private ConcurrentHashMap<SocketAddress, Long> failedDelayCache;
    private long retryAfterMs;

    public ProxyListFallbackSelector(ProxySelector delegate) {
        this(600000L, delegate);
    }

    public ProxyListFallbackSelector(long retryAfterMs, ProxySelector delegate) {
        this.failedDelayCache = new ConcurrentHashMap();
        this.delegate = delegate;
        this.retryAfterMs = retryAfterMs;
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        this.failedDelayCache.put(sa, System.currentTimeMillis());
    }

    public List<Proxy> select(URI uri) {
        this.cleanupCache();
        List<Proxy> proxyList = this.delegate.select(uri);
        List<Proxy> result = this.filterUnresponsiveProxiesFromList(proxyList);
        return result;
    }

    private void cleanupCache() {
        Iterator it = this.failedDelayCache.entrySet().iterator();

        while(it.hasNext()) {
            Entry<SocketAddress, Long> e = (Entry)it.next();
            Long lastFailTime = (Long)e.getValue();
            if (this.retryDelayHasPassedBy(lastFailTime)) {
                it.remove();
            }
        }

    }

    private List<Proxy> filterUnresponsiveProxiesFromList(List<Proxy> proxyList) {
        if (this.failedDelayCache.isEmpty()) {
            return proxyList;
        } else {
            List<Proxy> result = new ArrayList(proxyList.size());
            Iterator i$ = proxyList.iterator();

            while(true) {
                Proxy proxy;
                do {
                    if (!i$.hasNext()) {
                        return result;
                    }

                    proxy = (Proxy)i$.next();
                } while(!this.isDirect(proxy) && !this.isNotUnresponsive(proxy));

                result.add(proxy);
            }
        }
    }

    private boolean isDirect(Proxy proxy) {
        return Proxy.NO_PROXY.equals(proxy);
    }

    private boolean isNotUnresponsive(Proxy proxy) {
        Long lastFailTime = (Long)this.failedDelayCache.get(proxy.address());
        return this.retryDelayHasPassedBy(lastFailTime);
    }

    private boolean retryDelayHasPassedBy(Long lastFailTime) {
        return lastFailTime == null || lastFailTime.longValue() + this.retryAfterMs < System.currentTimeMillis();
    }

    final void setRetryAfterMs(long retryAfterMs) {
        this.retryAfterMs = retryAfterMs;
    }
}
