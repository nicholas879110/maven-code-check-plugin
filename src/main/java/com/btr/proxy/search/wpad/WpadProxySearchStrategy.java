//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.wpad;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

public class WpadProxySearchStrategy implements ProxySearchStrategy {
    public WpadProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() throws ProxyException {
        try {
            Logger.log(this.getClass(), LogLevel.TRACE, "Using WPAD to find a proxy", new Object[0]);
            String pacScriptUrl = this.detectScriptUrlPerDHCP();
            if (pacScriptUrl == null) {
                pacScriptUrl = this.detectScriptUrlPerDNS();
            }

            if (pacScriptUrl == null) {
                return null;
            } else {
                Logger.log(this.getClass(), LogLevel.TRACE, "PAC script url found: {0}", new Object[]{pacScriptUrl});
                return ProxyUtil.buildPacSelectorForUrl(pacScriptUrl);
            }
        } catch (IOException var2) {
            Logger.log(this.getClass(), LogLevel.ERROR, "Error during WPAD search.", new Object[]{var2});
            throw new ProxyException(var2);
        }
    }

    public Properties readSettings() {
        try {
            String pacScriptUrl = this.detectScriptUrlPerDHCP();
            if (pacScriptUrl == null) {
                pacScriptUrl = this.detectScriptUrlPerDNS();
            }

            if (pacScriptUrl == null) {
                return null;
            } else {
                Properties result = new Properties();
                result.setProperty("url", pacScriptUrl);
                return result;
            }
        } catch (IOException var3) {
            return new Properties();
        }
    }

    private String detectScriptUrlPerDNS() throws IOException {
        String result = null;
        String fqdn = InetAddress.getLocalHost().getCanonicalHostName();
        Logger.log(this.getClass(), LogLevel.TRACE, "Searching per DNS guessing.", new Object[0]);

        for(int index = fqdn.indexOf(46); index != -1 && result == null; index = fqdn.indexOf(46)) {
            fqdn = fqdn.substring(index + 1);
            if (fqdn.indexOf(46) == -1) {
                break;
            }

            try {
                URL lookupURL = new URL("http://wpad." + fqdn + "/wpad.dat");
                Logger.log(this.getClass(), LogLevel.TRACE, "Trying url: {0}", new Object[]{lookupURL});
                HttpURLConnection con = (HttpURLConnection)lookupURL.openConnection(Proxy.NO_PROXY);
                con.setInstanceFollowRedirects(true);
                con.setRequestProperty("accept", "application/x-ns-proxy-autoconfig");
                if (con.getResponseCode() == 200) {
                    result = lookupURL.toString();
                }

                con.disconnect();
            } catch (UnknownHostException var6) {
                Logger.log(this.getClass(), LogLevel.DEBUG, "Not available!", new Object[0]);
            }
        }

        return result;
    }

    private String detectScriptUrlPerDHCP() {
        Logger.log(this.getClass(), LogLevel.DEBUG, "Searching per DHCP not supported yet.", new Object[0]);
        return null;
    }
}
