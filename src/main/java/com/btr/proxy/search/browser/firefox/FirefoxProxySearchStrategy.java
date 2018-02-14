//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.browser.firefox;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.desktop.DesktopProxySearchStrategy;
import com.btr.proxy.search.wpad.WpadProxySearchStrategy;
import com.btr.proxy.selector.direct.NoProxySelector;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.fixed.FixedSocksSelector;
import com.btr.proxy.selector.misc.ProtocolDispatchSelector;
import com.btr.proxy.selector.whitelist.ProxyBypassListSelector;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.PlatformUtil;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.Logger.LogLevel;
import com.btr.proxy.util.PlatformUtil.Platform;
import java.io.IOException;
import java.net.ProxySelector;
import java.util.Properties;

public class FirefoxProxySearchStrategy implements ProxySearchStrategy {
    private FirefoxProfileSource profileScanner;
    private FirefoxSettingParser settingsParser;

    public FirefoxProxySearchStrategy() {
        if (PlatformUtil.getCurrentPlattform() == Platform.WIN) {
            this.profileScanner = new WinFirefoxProfileSource();
        } else {
            this.profileScanner = new LinuxFirefoxProfileSource();
        }

        this.settingsParser = new FirefoxSettingParser();
    }

    public ProxySelector getProxySelector() throws ProxyException {
        Logger.log(this.getClass(), LogLevel.TRACE, "Detecting Firefox settings.", new Object[0]);
        Properties settings = this.readSettings();
        ProxySelector result = null;
        int type = Integer.parseInt(settings.getProperty("network.proxy.type", "-1"));
        String noProxyList;
        switch(type) {
            case -1:
                Logger.log(this.getClass(), LogLevel.TRACE, "Firefox uses system settings", new Object[0]);
                result = (new DesktopProxySearchStrategy()).getProxySelector();
                break;
            case 0:
                Logger.log(this.getClass(), LogLevel.TRACE, "Firefox uses no proxy", new Object[0]);
                result = NoProxySelector.getInstance();
                break;
            case 1:
                Logger.log(this.getClass(), LogLevel.TRACE, "Firefox uses manual settings", new Object[0]);
                result = this.setupFixedProxySelector(settings);
                break;
            case 2:
                noProxyList = settings.getProperty("network.proxy.autoconfig_url", "");
                Logger.log(this.getClass(), LogLevel.TRACE, "Firefox uses script (PAC) {0}", new Object[]{noProxyList});
                result = ProxyUtil.buildPacSelectorForUrl(noProxyList);
                break;
            case 3:
                Logger.log(this.getClass(), LogLevel.TRACE, "Netscape compability mode -> uses no proxy", new Object[0]);
                result = NoProxySelector.getInstance();
                break;
            case 4:
                Logger.log(this.getClass(), LogLevel.TRACE, "Firefox uses automatic detection (WPAD)", new Object[0]);
                result = (new WpadProxySearchStrategy()).getProxySelector();
        }

        noProxyList = settings.getProperty("network.proxy.no_proxies_on", (String)null);
        if (result != null && noProxyList != null && noProxyList.trim().length() > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Firefox uses proxy bypass list for: {0}", new Object[]{noProxyList});
            result = new ProxyBypassListSelector(noProxyList, (ProxySelector)result);
        }

        return (ProxySelector)result;
    }

    public Properties readSettings() throws ProxyException {
        try {
            Properties settings = this.settingsParser.parseSettings(this.profileScanner);
            return settings;
        } catch (IOException var2) {
            Logger.log(this.getClass(), LogLevel.ERROR, "Error parsing settings", new Object[]{var2});
            throw new ProxyException(var2);
        }
    }

    private ProxySelector setupFixedProxySelector(Properties settings) {
        ProtocolDispatchSelector ps = new ProtocolDispatchSelector();
        this.installHttpProxy(ps, settings);
        if (this.isProxyShared(settings)) {
            this.installSharedProxy(ps);
        } else {
            this.installFtpProxy(ps, settings);
            this.installSecureProxy(ps, settings);
            this.installSocksProxy(ps, settings);
        }

        return ps;
    }

    private void installFtpProxy(ProtocolDispatchSelector ps, Properties settings) throws NumberFormatException {
        this.installSelectorForProtocol(ps, settings, "ftp");
    }

    private void installHttpProxy(ProtocolDispatchSelector ps, Properties settings) throws NumberFormatException {
        this.installSelectorForProtocol(ps, settings, "http");
    }

    private boolean isProxyShared(Properties settings) {
        return Boolean.TRUE.toString().equals(settings.getProperty("network.proxy.share_proxy_settings", "false").toLowerCase());
    }

    private void installSharedProxy(ProtocolDispatchSelector ps) {
        ProxySelector httpProxy = ps.getSelector("http");
        if (httpProxy != null) {
            ps.setFallbackSelector(httpProxy);
        }

    }

    private void installSocksProxy(ProtocolDispatchSelector ps, Properties settings) throws NumberFormatException {
        String proxyHost = settings.getProperty("network.proxy.socks", (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("network.proxy.socks_port", "0"));
        if (proxyHost != null && proxyPort != 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Firefox socks proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector("socks", new FixedSocksSelector(proxyHost, proxyPort));
        }

    }

    private void installSecureProxy(ProtocolDispatchSelector ps, Properties settings) throws NumberFormatException {
        String proxyHost = settings.getProperty("network.proxy.ssl", (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("network.proxy.ssl_port", "0"));
        if (proxyHost != null && proxyPort != 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Firefox secure proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector("https", new FixedProxySelector(proxyHost, proxyPort));
            ps.setSelector("sftp", new FixedProxySelector(proxyHost, proxyPort));
        }

    }

    private void installSelectorForProtocol(ProtocolDispatchSelector ps, Properties settings, String protocol) throws NumberFormatException {
        String proxyHost = settings.getProperty("network.proxy." + protocol, (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("network.proxy." + protocol + "_port", "0"));
        if (proxyHost != null && proxyPort != 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Firefox " + protocol + " proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector(protocol, new FixedProxySelector(proxyHost, proxyPort));
        }

    }
}
