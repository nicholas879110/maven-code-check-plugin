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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class BufferedProxySelector extends ProxySelector {
    private ProxySelector delegate;
    private ConcurrentHashMap<String, BufferedProxySelector.CacheEntry> cache = new ConcurrentHashMap();
    private int maxSize;
    private long ttl;

    public BufferedProxySelector(int maxSize, long ttl, ProxySelector delegate) {
        this.maxSize = maxSize;
        this.delegate = delegate;
        this.ttl = ttl;
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        this.delegate.connectFailed(uri, sa, ioe);
    }

    public List<Proxy> select(URI uri) {
        String cacheKey = uri.toString();
        BufferedProxySelector.CacheEntry entry = (BufferedProxySelector.CacheEntry)this.cache.get(cacheKey);
        if (entry == null || entry.isExpired()) {
            List<Proxy> result = this.delegate.select(uri);
            entry = new BufferedProxySelector.CacheEntry(result, System.nanoTime() + this.ttl * 1000L * 1000L);
            ConcurrentHashMap var5 = this.cache;
            synchronized(this.cache) {
                if (this.cache.size() >= this.maxSize) {
                    this.purgeCache();
                }

                this.cache.put(cacheKey, entry);
            }
        }

        return entry.result;
    }

    private void purgeCache() {
        boolean removedOne = false;
        Entry<String, BufferedProxySelector.CacheEntry> oldest = null;
        Set<Entry<String, BufferedProxySelector.CacheEntry>> entries = this.cache.entrySet();
        Iterator it = entries.iterator();

        while(true) {
            while(it.hasNext()) {
                Entry<String, BufferedProxySelector.CacheEntry> entry = (Entry)it.next();
                if (((BufferedProxySelector.CacheEntry)entry.getValue()).isExpired()) {
                    it.remove();
                    removedOne = true;
                } else if (oldest == null || ((BufferedProxySelector.CacheEntry)entry.getValue()).expireAt < ((BufferedProxySelector.CacheEntry)oldest.getValue()).expireAt) {
                    oldest = entry;
                }
            }

            if (!removedOne && oldest != null) {
                this.cache.remove(oldest.getKey());
            }

            return;
        }
    }

    private static class CacheEntry {
        List<Proxy> result;
        long expireAt;

        public CacheEntry(List<Proxy> r, long expireAt) {
            this.result = new ArrayList(r.size());
            this.result.addAll(r);
            this.result = Collections.unmodifiableList(this.result);
            this.expireAt = expireAt;
        }

        public boolean isExpired() {
            return System.nanoTime() >= this.expireAt;
        }
    }
}
