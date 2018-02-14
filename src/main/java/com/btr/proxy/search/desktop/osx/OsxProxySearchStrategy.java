//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.osx;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.browser.ie.IELocalByPassFilter;
import com.btr.proxy.search.wpad.WpadProxySearchStrategy;
import com.btr.proxy.selector.direct.NoProxySelector;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.fixed.FixedSocksSelector;
import com.btr.proxy.selector.misc.ProtocolDispatchSelector;
import com.btr.proxy.selector.whitelist.ProxyBypassListSelector;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.PListParser;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.UriFilter;
import com.btr.proxy.util.Logger.LogLevel;
import com.btr.proxy.util.PListParser.Dict;
import com.btr.proxy.util.PListParser.XmlParseException;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.ProxySelector;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class OsxProxySearchStrategy implements ProxySearchStrategy {
    public static final String OVERRIDE_SETTINGS_FILE = "com.btr.proxy.osx.settingsFile";
    public static final String OVERRIDE_ACCEPTED_DEVICES = "com.btr.proxy.osx.acceptedDevices";
    private static final String SETTINGS_FILE = "/Library/Preferences/SystemConfiguration/preferences.plist";

    public OsxProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() throws ProxyException {
        Logger.log(this.getClass(), LogLevel.TRACE, "Detecting OSX proxy settings", new Object[0]);

        try {
            List<String> acceptedInterfaces = this.getNetworkInterfaces();
            Dict settings = PListParser.load(this.getSettingsFile());
            Object currentSet = settings.getAtPath("/CurrentSet");
            if (currentSet == null) {
                throw new ProxyException("CurrentSet not defined");
            } else {
                Dict networkSet = (Dict)settings.getAtPath(String.valueOf(currentSet));
                List<?> serviceOrder = (List)networkSet.getAtPath("/Network/Global/IPv4/ServiceOrder");
                if (serviceOrder != null && serviceOrder.size() != 0) {
                    Dict proxySettings = null;

                    for(int i = 0; i < serviceOrder.size() && proxySettings == null; ++i) {
                        Object candidateService = serviceOrder.get(i);
                        Object networkService = networkSet.getAtPath("/Network/Service/" + candidateService + "/__LINK__");
                        if (networkService == null) {
                            throw new ProxyException("NetworkService not defined.");
                        }

                        Dict selectedServiceSettings = (Dict)settings.getAtPath("" + networkService);
                        String interfaceName = (String)selectedServiceSettings.getAtPath("/Interface/DeviceName");
                        if (acceptedInterfaces.contains(interfaceName)) {
                            Logger.log(this.getClass(), LogLevel.TRACE, "Looking up proxies for device " + interfaceName, new Object[0]);
                            proxySettings = (Dict)selectedServiceSettings.getAtPath("/Proxies");
                        }
                    }

                    return (ProxySelector)(proxySettings == null ? NoProxySelector.getInstance() : this.buildSelector(proxySettings));
                } else {
                    throw new ProxyException("ServiceOrder not defined");
                }
            }
        } catch (XmlParseException var12) {
            throw new ProxyException(var12);
        } catch (IOException var13) {
            throw new ProxyException(var13);
        }
    }

    private ProxySelector buildSelector(Dict proxySettings) throws ProxyException {
        ProtocolDispatchSelector ps = new ProtocolDispatchSelector();
        this.installSelectorForProtocol(proxySettings, ps, "HTTP");
        this.installSelectorForProtocol(proxySettings, ps, "HTTPS");
        this.installSelectorForProtocol(proxySettings, ps, "FTP");
        this.installSelectorForProtocol(proxySettings, ps, "Gopher");
        this.installSelectorForProtocol(proxySettings, ps, "RTSP");
        this.installSocksProxy(proxySettings, ps);
        ProxySelector result = this.installPacProxyIfAvailable(proxySettings, ps);
        result = this.autodetectProxyIfAvailable(proxySettings, result);
        result = this.installExceptionList(proxySettings, result);
        result = this.installSimpleHostFilter(proxySettings, result);
        return result;
    }

    private List<String> getNetworkInterfaces() throws SocketException {
        String override = System.getProperty("com.btr.proxy.osx.acceptedDevices");
        if (override != null && override.length() > 0) {
            return Arrays.asList(override.split(";"));
        } else {
            List<String> acceptedInterfaces = new ArrayList();
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();

            while(interfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface)interfaces.nextElement();
                if (this.isInterfaceAllowed(ni)) {
                    acceptedInterfaces.add(ni.getName());
                }
            }

            return acceptedInterfaces;
        }
    }

    private boolean isInterfaceAllowed(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && !ni.isVirtual() && ni.isUp();
    }

    private File getSettingsFile() {
        File result = new File("/Library/Preferences/SystemConfiguration/preferences.plist");
        String overrideFile = System.getProperty("com.btr.proxy.osx.settingsFile");
        return overrideFile != null ? new File(overrideFile) : result;
    }

    private ProxySelector installSimpleHostFilter(Dict proxySettings, ProxySelector result) {
        if (this.isActive(proxySettings.get("ExcludeSimpleHostnames"))) {
            List<UriFilter> localBypassFilter = new ArrayList();
            localBypassFilter.add(new IELocalByPassFilter());
            result = new ProxyBypassListSelector(localBypassFilter, (ProxySelector)result);
        }

        return (ProxySelector)result;
    }

    private ProxySelector installExceptionList(Dict proxySettings, ProxySelector result) {
        List<?> proxyExceptions = (List)proxySettings.get("ExceptionsList");
        if (proxyExceptions != null && proxyExceptions.size() > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "OSX uses proxy bypass list: {0}", new Object[]{proxyExceptions});
            String noProxyList = this.toCommaSeparatedString(proxyExceptions);
            result = new ProxyBypassListSelector(noProxyList, (ProxySelector)result);
        }

        return (ProxySelector)result;
    }

    private String toCommaSeparatedString(List<?> proxyExceptions) {
        StringBuilder result = new StringBuilder();

        Object object;
        for(Iterator i$ = proxyExceptions.iterator(); i$.hasNext(); result.append(object)) {
            object = i$.next();
            if (result.length() > 0) {
                result.append(",");
            }
        }

        return result.toString();
    }

    private ProxySelector autodetectProxyIfAvailable(Dict proxySettings, ProxySelector result) throws ProxyException {
        if (this.isActive(proxySettings.get("ProxyAutoDiscoveryEnable"))) {
            ProxySelector wp = (new WpadProxySearchStrategy()).getProxySelector();
            if (wp != null) {
                result = wp;
            }
        }

        return result;
    }

    private ProxySelector installPacProxyIfAvailable(Dict proxySettings, ProxySelector result) {
        if (this.isActive(proxySettings.get("ProxyAutoConfigEnable"))) {
            String url = (String)proxySettings.get("ProxyAutoConfigURLString");
            result = ProxyUtil.buildPacSelectorForUrl(url);
        }

        return (ProxySelector)result;
    }

    private void installSocksProxy(Dict proxySettings, ProtocolDispatchSelector ps) {
        if (this.isActive(proxySettings.get("SOCKSEnable"))) {
            String proxyHost = (String)proxySettings.get("SOCKSProxy");
            int proxyPort = ((Integer)proxySettings.get("SOCKSPort")).intValue();
            ps.setSelector("socks", new FixedSocksSelector(proxyHost, proxyPort));
            Logger.log(this.getClass(), LogLevel.TRACE, "OSX socks proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
        }

    }

    private void installSelectorForProtocol(Dict proxySettings, ProtocolDispatchSelector ps, String protocol) {
        String prefix = protocol.trim();
        if (this.isActive(proxySettings.get(prefix + "Enable"))) {
            String proxyHost = (String)proxySettings.get(prefix + "Proxy");
            int proxyPort = ((Integer)proxySettings.get(prefix + "Port")).intValue();
            FixedProxySelector fp = new FixedProxySelector(proxyHost, proxyPort);
            ps.setSelector(protocol.toLowerCase(), fp);
            Logger.log(this.getClass(), LogLevel.TRACE, "OSX uses for {0} the proxy {1}:{2}", new Object[]{protocol, proxyHost, proxyPort});
        }

    }

    private boolean isActive(Object value) {
        return Integer.valueOf(1).equals(value);
    }
}
