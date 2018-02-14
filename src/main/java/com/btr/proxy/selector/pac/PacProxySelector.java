//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.List;

public class PacProxySelector extends ProxySelector {
    private final boolean JAVAX_PARSER = ScriptAvailability.isJavaxScriptingAvailable();
    private static final String PAC_SOCKS = "SOCKS";
    private static final String PAC_DIRECT = "DIRECT";
    private PacScriptParser pacScriptParser;
    private static volatile boolean enabled = true;

    public PacProxySelector(PacScriptSource pacSource) {
        this.selectEngine(pacSource);
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private void selectEngine(PacScriptSource pacSource) {
        try {
            if (this.JAVAX_PARSER) {
                Logger.log(this.getClass(), LogLevel.INFO, "Using javax.script JavaScript engine.", new Object[0]);
                this.pacScriptParser = new JavaxPacScriptParser(pacSource);
            } else {
                Logger.log(this.getClass(), LogLevel.INFO, "Using Rhino JavaScript engine.", new Object[0]);
                //this.pacScriptParser = new RhinoPacScriptParser(pacSource);
            }
        } catch (Exception var3) {
            Logger.log(this.getClass(), LogLevel.ERROR, "PAC parser error.", new Object[]{var3});
        }

    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    }

    public List<Proxy> select(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI must not be null.");
        } else {
            return !enabled ? ProxyUtil.noProxyList() : this.findProxy(uri);
        }
    }

    private List<Proxy> findProxy(URI uri) {
        try {
            List<Proxy> proxies = new ArrayList();
            String parseResult = this.pacScriptParser.evaluate(uri.toString(), uri.getHost());
            String[] proxyDefinitions = parseResult.split("[;]");
            String[] arr$ = proxyDefinitions;
            int len$ = proxyDefinitions.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String proxyDef = arr$[i$];
                if (proxyDef.trim().length() > 0) {
                    proxies.add(this.buildProxyFromPacResult(proxyDef));
                }
            }

            return proxies;
        } catch (ProxyEvaluationException var9) {
            Logger.log(this.getClass(), LogLevel.ERROR, "PAC resolving error.", new Object[]{var9});
            return ProxyUtil.noProxyList();
        }
    }

    private Proxy buildProxyFromPacResult(String pacResult) {
        if (pacResult != null && pacResult.trim().length() >= 6) {
            String proxyDef = pacResult.trim();
            if (proxyDef.toUpperCase().startsWith("DIRECT")) {
                return Proxy.NO_PROXY;
            } else {
                Type type = Type.HTTP;
                if (proxyDef.toUpperCase().startsWith("SOCKS")) {
                    type = Type.SOCKS;
                }

                String host = proxyDef.substring(6);
                Integer port = Integer.valueOf(80);
                int indexOfPort = host.indexOf(58);
                if (indexOfPort != -1) {
                    port = Integer.parseInt(host.substring(indexOfPort + 1).trim());
                    host = host.substring(0, indexOfPort).trim();
                }

                SocketAddress adr = InetSocketAddress.createUnresolved(host, port.intValue());
                return new Proxy(type, adr);
            }
        } else {
            return Proxy.NO_PROXY;
        }
    }
}
