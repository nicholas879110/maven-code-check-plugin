//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search;

import com.btr.proxy.search.browser.firefox.FirefoxProxySearchStrategy;
import com.btr.proxy.search.browser.ie.IEProxySearchStrategy;
import com.btr.proxy.search.desktop.DesktopProxySearchStrategy;
import com.btr.proxy.search.desktop.gnome.GnomeProxySearchStrategy;
import com.btr.proxy.search.desktop.kde.KdeProxySearchStrategy;
import com.btr.proxy.search.desktop.win.WinProxySearchStrategy;
import com.btr.proxy.search.env.EnvProxySearchStrategy;
import com.btr.proxy.search.java.JavaProxySearchStrategy;
import com.btr.proxy.selector.misc.BufferedProxySelector;
import com.btr.proxy.selector.misc.ProxyListFallbackSelector;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.PlatformUtil;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.Logger.LogBackEnd;
import com.btr.proxy.util.Logger.LogLevel;
import java.awt.GraphicsEnvironment;
import java.net.ProxySelector;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProxySearch implements ProxySearchStrategy {
    private static final int DEFAULT_PAC_CACHE_SIZE = 20;
    private static final long DEFAULT_PAC_CACHE_TTL = 600000L;
    private List<ProxySearchStrategy> strategies = new ArrayList();
    private int pacCacheSize = 20;
    private long pacCacheTTL = 600000L;

    public ProxySearch() {
    }

    public static ProxySearch getDefaultProxySearch() {
        ProxySearch s = new ProxySearch();
        boolean headless = GraphicsEnvironment.isHeadless();
        if (headless) {
            s.addStrategy(ProxySearch.Strategy.JAVA);
            s.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
            s.addStrategy(ProxySearch.Strategy.ENV_VAR);
        } else {
            s.addStrategy(ProxySearch.Strategy.JAVA);
            s.addStrategy(ProxySearch.Strategy.BROWSER);
            s.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
            s.addStrategy(ProxySearch.Strategy.ENV_VAR);
        }

        Logger.log(ProxySearch.class, LogLevel.TRACE, "Using default search priority: {0}", new Object[]{s});
        return s;
    }

    public void addStrategy(ProxySearch.Strategy strategy) {
        switch(strategy) {
            case OS_DEFAULT:
                this.strategies.add(new DesktopProxySearchStrategy());
                break;
            case BROWSER:
                this.strategies.add(this.getDefaultBrowserStrategy());
                break;
            case FIREFOX:
                this.strategies.add(new FirefoxProxySearchStrategy());
                break;
            case IE:
                this.strategies.add(new IEProxySearchStrategy());
                break;
            case ENV_VAR:
                this.strategies.add(new EnvProxySearchStrategy());
                break;
            case WIN:
                this.strategies.add(new WinProxySearchStrategy());
                break;
            case KDE:
                this.strategies.add(new KdeProxySearchStrategy());
                break;
            case GNOME:
                this.strategies.add(new GnomeProxySearchStrategy());
                break;
            case JAVA:
                this.strategies.add(new JavaProxySearchStrategy());
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy code!");
        }

    }

    public void setPacCacheSettings(int size, long ttl) {
        this.pacCacheSize = size;
        this.pacCacheTTL = ttl;
    }

    private ProxySearchStrategy getDefaultBrowserStrategy() {
        switch(PlatformUtil.getDefaultBrowser()) {
            case IE:
                return new IEProxySearchStrategy();
            case FIREFOX:
                return new FirefoxProxySearchStrategy();
            default:
                return null;
        }
    }

    public ProxySelector getProxySelector() {
        Logger.log(this.getClass(), LogLevel.TRACE, "Executing search strategies to find proxy selector", new Object[0]);
        Iterator i$ = this.strategies.iterator();

        while(i$.hasNext()) {
            ProxySearchStrategy strat = (ProxySearchStrategy)i$.next();

            try {
                ProxySelector selector = strat.getProxySelector();
                if (selector != null) {
                    selector = this.installBufferingAndFallbackBehaviour(selector);
                    return selector;
                }
            } catch (ProxyException var4) {
                Logger.log(this.getClass(), LogLevel.DEBUG, "Strategy {0} failed trying next one.", new Object[]{var4});
            }
        }

        return null;
    }

    private ProxySelector installBufferingAndFallbackBehaviour(ProxySelector selector) {
        if (selector instanceof PacProxySelector) {
            if (this.pacCacheSize > 0) {
                selector = new BufferedProxySelector(this.pacCacheSize, this.pacCacheTTL, (ProxySelector)selector);
            }

            selector = new ProxyListFallbackSelector((ProxySelector)selector);
        }

        return (ProxySelector)selector;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Proxy search: ");
        Iterator i$ = this.strategies.iterator();

        while(i$.hasNext()) {
            ProxySearchStrategy strat = (ProxySearchStrategy)i$.next();
            sb.append(strat);
            sb.append(" ");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        ProxySearch ps = getDefaultProxySearch();
        Logger.setBackend(new LogBackEnd() {
            public void log(Class<?> clazz, LogLevel loglevel, String msg, Object... params) {
                System.out.println(MessageFormat.format(msg, params));
            }

            public boolean isLogginEnabled(LogLevel logLevel) {
                return true;
            }
        });
        ps.getProxySelector();
    }

    public static enum Strategy {
        OS_DEFAULT,
        BROWSER,
        FIREFOX,
        IE,
        ENV_VAR,
        WIN,
        KDE,
        GNOME,
        JAVA;

        private Strategy() {
        }
    }
}
