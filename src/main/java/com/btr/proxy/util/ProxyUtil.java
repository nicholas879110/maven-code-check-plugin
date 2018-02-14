//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.util;

import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.PacScriptSource;
import com.btr.proxy.selector.pac.UrlPacScriptSource;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyUtil {
    public static final int DEFAULT_PROXY_PORT = 80;
    private static List<Proxy> noProxyList;
    private static Pattern pattern = Pattern.compile("\\w*?:?/*([^:/]+):?(\\d*)/?");

    public ProxyUtil() {
    }

    public static FixedProxySelector parseProxySettings(String proxyVar) {
        if (proxyVar != null && proxyVar.trim().length() != 0) {
            Matcher matcher = pattern.matcher(proxyVar);
            if (matcher.matches()) {
                String host = matcher.group(1);
                int port;
                if (!"".equals(matcher.group(2))) {
                    port = Integer.parseInt(matcher.group(2));
                } else {
                    port = 80;
                }

                return new FixedProxySelector(host.trim(), port);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static synchronized List<Proxy> noProxyList() {
        if (noProxyList == null) {
            ArrayList<Proxy> list = new ArrayList(1);
            list.add(Proxy.NO_PROXY);
            noProxyList = Collections.unmodifiableList(list);
        }

        return noProxyList;
    }

    public static PacProxySelector buildPacSelectorForUrl(String url) {
        PacProxySelector result = null;
        PacScriptSource pacSource = new UrlPacScriptSource(url);
        if (pacSource.isScriptValid()) {
            result = new PacProxySelector(pacSource);
        }

        return result;
    }
}
