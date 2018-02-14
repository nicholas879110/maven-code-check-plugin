//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.misc;

import com.btr.proxy.selector.direct.NoProxySelector;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolDispatchSelector extends ProxySelector {
    private Map<String, ProxySelector> selectors = new ConcurrentHashMap();
    private ProxySelector fallbackSelector = NoProxySelector.getInstance();

    public ProtocolDispatchSelector() {
    }

    public void setSelector(String protocol, ProxySelector selector) {
        if (protocol == null) {
            throw new NullPointerException("Protocol must not be null.");
        } else if (selector == null) {
            throw new NullPointerException("Selector must not be null.");
        } else {
            this.selectors.put(protocol, selector);
        }
    }

    public ProxySelector removeSelector(String protocol) {
        return (ProxySelector)this.selectors.remove(protocol);
    }

    public ProxySelector getSelector(String protocol) {
        return (ProxySelector)this.selectors.get(protocol);
    }

    public void setFallbackSelector(ProxySelector selector) {
        if (selector == null) {
            throw new NullPointerException("Selector must not be null.");
        } else {
            this.fallbackSelector = selector;
        }
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        ProxySelector selector = this.fallbackSelector;
        String protocol = uri.getScheme();
        if (protocol != null && this.selectors.get(protocol) != null) {
            selector = (ProxySelector)this.selectors.get(protocol);
        }

        selector.connectFailed(uri, sa, ioe);
    }

    public List<Proxy> select(URI uri) {
        ProxySelector selector = this.fallbackSelector;
        String protocol = uri.getScheme();
        if (protocol != null && this.selectors.get(protocol) != null) {
            selector = (ProxySelector)this.selectors.get(protocol);
        }

        return selector.select(uri);
    }
}
