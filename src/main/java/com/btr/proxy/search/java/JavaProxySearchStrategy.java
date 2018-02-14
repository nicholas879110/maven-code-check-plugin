//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.java;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.fixed.FixedSocksSelector;
import com.btr.proxy.selector.misc.ProtocolDispatchSelector;
import com.btr.proxy.selector.whitelist.ProxyBypassListSelector;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.net.ProxySelector;

public class JavaProxySearchStrategy implements ProxySearchStrategy {
    public JavaProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() {
        ProtocolDispatchSelector ps = new ProtocolDispatchSelector();
        if (!this.proxyPropertyPresent()) {
            return null;
        } else {
            Logger.log(this.getClass(), LogLevel.TRACE, "Using settings from Java System Properties", new Object[0]);
            this.setupProxyForProtocol(ps, "http", 80);
            this.setupProxyForProtocol(ps, "https", 443);
            this.setupProxyForProtocol(ps, "ftp", 80);
            this.setupProxyForProtocol(ps, "ftps", 80);
            this.setupSocktProxy(ps);
            return ps;
        }
    }

    private boolean proxyPropertyPresent() {
        return System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyHost").trim().length() > 0;
    }

    private void setupSocktProxy(ProtocolDispatchSelector ps) {
        String host = System.getProperty("socksProxyHost");
        String port = System.getProperty("socksProxyPort", "1080");
        if (host != null && host.trim().length() > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Socks proxy {0}:{1} found", new Object[]{host, port});
            ps.setSelector("socks", new FixedSocksSelector(host, Integer.parseInt(port)));
        }

    }

    private void setupProxyForProtocol(ProtocolDispatchSelector ps, String protocol, int defaultPort) {
        String host = System.getProperty(protocol + ".proxyHost");
        String port = System.getProperty(protocol + ".proxyPort", "" + defaultPort);
        String whiteList = System.getProperty(protocol + ".nonProxyHosts", "").replace('|', ',');
        if ("https".equalsIgnoreCase(protocol)) {
            whiteList = System.getProperty("http.nonProxyHosts", "").replace('|', ',');
        }

        if (host != null && host.trim().length() != 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, protocol.toUpperCase() + " proxy {0}:{1} found using whitelist: {2}", new Object[]{host, port, whiteList});
            ProxySelector protocolSelector = new FixedProxySelector(host, Integer.parseInt(port));
            if (whiteList.trim().length() > 0) {
                protocolSelector = new ProxyBypassListSelector(whiteList, (ProxySelector)protocolSelector);
            }

            ps.setSelector(protocol, (ProxySelector)protocolSelector);
        }
    }
}
