//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.desktop.gnome.GnomeProxySearchStrategy;
import com.btr.proxy.search.desktop.kde.KdeProxySearchStrategy;
import com.btr.proxy.search.desktop.osx.OsxProxySearchStrategy;
import com.btr.proxy.search.desktop.win.WinProxySearchStrategy;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.PlatformUtil;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.Logger.LogLevel;
import com.btr.proxy.util.PlatformUtil.Desktop;
import com.btr.proxy.util.PlatformUtil.Platform;
import java.net.ProxySelector;

public class DesktopProxySearchStrategy implements ProxySearchStrategy {
    public DesktopProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() throws ProxyException {
        ProxySearchStrategy strategy = this.findDesktopSpecificStrategy();
        return strategy == null ? null : strategy.getProxySelector();
    }

    private ProxySearchStrategy findDesktopSpecificStrategy() {
        Platform pf = PlatformUtil.getCurrentPlattform();
        Desktop dt = PlatformUtil.getCurrentDesktop();
        Logger.log(this.getClass(), LogLevel.TRACE, "Detecting system settings.", new Object[0]);
        ProxySearchStrategy strategy = null;
        if (pf == Platform.WIN) {
            Logger.log(this.getClass(), LogLevel.TRACE, "We are running on Windows.", new Object[0]);
            strategy = new WinProxySearchStrategy();
        } else if (dt == Desktop.KDE) {
            Logger.log(this.getClass(), LogLevel.TRACE, "We are running on KDE.", new Object[0]);
            strategy = new KdeProxySearchStrategy();
        } else if (dt == Desktop.GNOME) {
            Logger.log(this.getClass(), LogLevel.TRACE, "We are running on Gnome.", new Object[0]);
            strategy = new GnomeProxySearchStrategy();
        } else if (dt == Desktop.MAC_OS) {
            Logger.log(this.getClass(), LogLevel.TRACE, "We are running on Mac OSX.", new Object[0]);
            strategy = new OsxProxySearchStrategy();
        }

        return (ProxySearchStrategy)strategy;
    }
}
