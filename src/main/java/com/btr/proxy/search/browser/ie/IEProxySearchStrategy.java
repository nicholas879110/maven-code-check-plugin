//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.browser.ie;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.desktop.win.Win32IESettings;
import com.btr.proxy.search.desktop.win.Win32ProxyUtils;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.misc.ProtocolDispatchSelector;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.whitelist.ProxyBypassListSelector;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.UriFilter;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class IEProxySearchStrategy implements ProxySearchStrategy {
    public IEProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() throws ProxyException {
        Logger.log(this.getClass(), LogLevel.TRACE, "Detecting IE proxy settings", new Object[0]);
        Win32IESettings ieSettings = this.readSettings();
        ProxySelector result = this.createPacSelector(ieSettings);
        if (result == null) {
            result = this.createFixedProxySelector(ieSettings);
        }

        return (ProxySelector)result;
    }

    public Win32IESettings readSettings() {
        Win32IESettings ieSettings = (new Win32ProxyUtils()).winHttpGetIEProxyConfigForCurrentUser();
        return ieSettings;
    }

    private PacProxySelector createPacSelector(Win32IESettings ieSettings) {
        String pacUrl = null;
        if (ieSettings.isAutoDetect()) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Autodetecting script URL.", new Object[0]);
            pacUrl = (new Win32ProxyUtils()).winHttpDetectAutoProxyConfigUrl(3);
        }

        if (pacUrl == null) {
            pacUrl = ieSettings.getAutoConfigUrl();
        }

        if (pacUrl != null && pacUrl.trim().length() > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "IE uses script: " + pacUrl, new Object[0]);
            if (pacUrl.startsWith("file://") && !pacUrl.startsWith("file:///")) {
                pacUrl = "file:///" + pacUrl.substring(7);
            }

            return ProxyUtil.buildPacSelectorForUrl(pacUrl);
        } else {
            return null;
        }
    }

    private ProxySelector createFixedProxySelector(Win32IESettings ieSettings) throws ProxyException {
        String proxyString = ieSettings.getProxy();
        String bypassList = ieSettings.getProxyBypass();
        if (proxyString == null) {
            return null;
        } else {
            Logger.log(this.getClass(), LogLevel.TRACE, "IE uses manual settings: {0} with bypass list: {1}", new Object[]{proxyString, bypassList});
            Properties p = this.parseProxyList(proxyString);
            ProtocolDispatchSelector ps = new ProtocolDispatchSelector();
            this.addSelectorForProtocol(p, "http", ps);
            this.addSelectorForProtocol(p, "https", ps);
            this.addSelectorForProtocol(p, "ftp", ps);
            this.addSelectorForProtocol(p, "gopher", ps);
            this.addSelectorForProtocol(p, "socks", ps);
            this.addFallbackSelector(p, ps);
            ProxySelector result = this.setByPassListOnSelector(bypassList, ps);
            return result;
        }
    }

    private ProxySelector setByPassListOnSelector(String bypassList, ProtocolDispatchSelector ps) {
        if (bypassList != null && bypassList.trim().length() > 0) {
            ProxyBypassListSelector result;
            if ("<local>".equals(bypassList.trim())) {
                result = this.buildLocalBypassSelector(ps);
            } else {
                bypassList = bypassList.replace(';', ',');
                result = new ProxyBypassListSelector(bypassList, ps);
            }

            return result;
        } else {
            return ps;
        }
    }

    private ProxyBypassListSelector buildLocalBypassSelector(ProtocolDispatchSelector ps) {
        List<UriFilter> localBypassFilter = new ArrayList();
        localBypassFilter.add(new IELocalByPassFilter());
        return new ProxyBypassListSelector(localBypassFilter, ps);
    }

    private void addFallbackSelector(Properties settings, ProtocolDispatchSelector ps) {
        String proxy = settings.getProperty("default");
        if (proxy != null) {
            ps.setFallbackSelector(ProxyUtil.parseProxySettings(proxy));
        }

    }

    private void addSelectorForProtocol(Properties settings, String protocol, ProtocolDispatchSelector ps) {
        String proxy = settings.getProperty(protocol);
        if (proxy != null) {
            FixedProxySelector protocolSelector = ProxyUtil.parseProxySettings(proxy);
            ps.setSelector(protocol, protocolSelector);
        }

    }

    private Properties parseProxyList(String proxyString) throws ProxyException {
        Properties p = new Properties();
        if (proxyString.indexOf(61) == -1) {
            p.setProperty("default", proxyString);
        } else {
            try {
                proxyString = proxyString.replace(';', '\n');
                p.load(new ByteArrayInputStream(proxyString.getBytes("ISO-8859-1")));
            } catch (IOException var4) {
                Logger.log(this.getClass(), LogLevel.ERROR, "Error reading IE settings as properties: {0}", new Object[]{var4});
                throw new ProxyException(var4);
            }
        }

        return p;
    }
}
