//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.kde;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.env.EnvProxySearchStrategy;
import com.btr.proxy.search.wpad.WpadProxySearchStrategy;
import com.btr.proxy.selector.direct.NoProxySelector;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.misc.ProtocolDispatchSelector;
import com.btr.proxy.selector.whitelist.ProxyBypassListSelector;
import com.btr.proxy.selector.whitelist.UseProxyWhiteListSelector;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.IOException;
import java.net.ProxySelector;
import java.util.Properties;

public class KdeProxySearchStrategy implements ProxySearchStrategy {
    private KdeSettingsParser settingsParser;

    public KdeProxySearchStrategy() {
        this(new KdeSettingsParser());
    }

    public KdeProxySearchStrategy(KdeSettingsParser settingsParser) {
        this.settingsParser = settingsParser;
    }

    public ProxySelector getProxySelector() throws ProxyException {
        Logger.log(this.getClass(), LogLevel.TRACE, "Detecting Kde proxy settings", new Object[0]);
        Properties settings = this.readSettings();
        if (settings == null) {
            return null;
        } else {
            ProxySelector result = null;
            int type = Integer.parseInt(settings.getProperty("ProxyType", "-1"));
            switch(type) {
                case 0:
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde uses no proxy", new Object[0]);
                    result = NoProxySelector.getInstance();
                    break;
                case 1:
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde uses manual proxy settings", new Object[0]);
                    result = this.setupFixedProxySelector(settings);
                    break;
                case 2:
                    String pacScriptUrl = settings.getProperty("Proxy Config Script", "");
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde uses autodetect script {0}", new Object[]{pacScriptUrl});
                    result = ProxyUtil.buildPacSelectorForUrl(pacScriptUrl);
                    break;
                case 3:
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde uses WPAD to detect the proxy", new Object[0]);
                    result = (new WpadProxySearchStrategy()).getProxySelector();
                    break;
                case 4:
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde reads proxy from environment", new Object[0]);
                    result = this.setupEnvVarSelector(settings);
            }

            return (ProxySelector)result;
        }
    }

    private Properties readSettings() throws ProxyException {
        try {
            return this.settingsParser.parseSettings();
        } catch (IOException var2) {
            Logger.log(this.getClass(), LogLevel.ERROR, "Can't parse settings.", new Object[]{var2});
            throw new ProxyException(var2);
        }
    }

    private ProxySelector setupEnvVarSelector(Properties settings) {
        ProxySelector result = (new EnvProxySearchStrategy(settings.getProperty("httpProxy"), settings.getProperty("httpsProxy"), settings.getProperty("ftpProxy"), settings.getProperty("NoProxyFor"))).getProxySelector();
        return result;
    }

    private ProxySelector setupFixedProxySelector(Properties settings) {
        String proxyVar = settings.getProperty("httpProxy", (String)null);
        FixedProxySelector httpPS = ProxyUtil.parseProxySettings(proxyVar);
        if (httpPS == null) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Kde http proxy is {0}", new Object[]{proxyVar});
            return null;
        } else {
            ProtocolDispatchSelector ps = new ProtocolDispatchSelector();
            ps.setSelector("http", httpPS);
            proxyVar = settings.getProperty("httpsProxy", (String)null);
            FixedProxySelector httpsPS = ProxyUtil.parseProxySettings(proxyVar);
            if (httpsPS != null) {
                Logger.log(this.getClass(), LogLevel.TRACE, "Kde https proxy is {0}", new Object[]{proxyVar});
                ps.setSelector("https", httpsPS);
            }

            proxyVar = settings.getProperty("ftpProxy", (String)null);
            FixedProxySelector ftpPS = ProxyUtil.parseProxySettings(proxyVar);
            if (ftpPS != null) {
                Logger.log(this.getClass(), LogLevel.TRACE, "Kde ftp proxy is {0}", new Object[]{proxyVar});
                ps.setSelector("ftp", ftpPS);
            }

            String noProxyList = settings.getProperty("NoProxyFor", (String)null);
            if (noProxyList != null && noProxyList.trim().length() > 0) {
                boolean reverse = "true".equals(settings.getProperty("ReversedException", "false"));
                if (reverse) {
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde proxy blacklist is {0}", new Object[]{noProxyList});
                    return new UseProxyWhiteListSelector(noProxyList, ps);
                } else {
                    Logger.log(this.getClass(), LogLevel.TRACE, "Kde proxy whitelist is {0}", new Object[]{noProxyList});
                    return new ProxyBypassListSelector(noProxyList, ps);
                }
            } else {
                return ps;
            }
        }
    }
}
