//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.gnome;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.selector.direct.NoProxySelector;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.misc.ProtocolDispatchSelector;
import com.btr.proxy.selector.whitelist.ProxyBypassListSelector;
import com.btr.proxy.util.EmptyXMLResolver;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.ProxyException;
import com.btr.proxy.util.ProxyUtil;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.File;
import java.io.IOException;
import java.net.ProxySelector;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GnomeProxySearchStrategy implements ProxySearchStrategy {
    public GnomeProxySearchStrategy() {
    }

    public ProxySelector getProxySelector() throws ProxyException {
        Logger.log(this.getClass(), LogLevel.TRACE, "Detecting Gnome proxy settings", new Object[0]);
        Properties settings = this.readSettings();
        String type = settings.getProperty("/system/proxy/mode");
        ProxySelector result = null;
        String noProxyList;
        if (type == null) {
            noProxyList = settings.getProperty("/system/http_proxy/use_http_proxy");
            if (noProxyList == null) {
                return null;
            }

            type = Boolean.parseBoolean(noProxyList) ? "manual" : "none";
        }

        if ("none".equals(type)) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome uses no proxy", new Object[0]);
            result = NoProxySelector.getInstance();
        }

        if ("manual".equals(type)) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome uses manual proxy settings", new Object[0]);
            result = this.setupFixedProxySelector(settings);
        }

        if ("auto".equals(type)) {
            noProxyList = settings.getProperty("/system/proxy/autoconfig_url", "");
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome uses autodetect script {0}", new Object[]{noProxyList});
            result = ProxyUtil.buildPacSelectorForUrl(noProxyList);
        }

        noProxyList = settings.getProperty("/system/http_proxy/ignore_hosts", (String)null);
        if (result != null && noProxyList != null && noProxyList.trim().length() > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome uses proxy bypass list: {0}", new Object[]{noProxyList});
            result = new ProxyBypassListSelector(noProxyList, (ProxySelector)result);
        }

        return (ProxySelector)result;
    }

    public Properties readSettings() throws ProxyException {
        Properties settings = new Properties();

        try {
            this.parseSettings("/system/proxy/", settings);
            this.parseSettings("/system/http_proxy/", settings);
            return settings;
        } catch (IOException var3) {
            Logger.log(this.getClass(), LogLevel.ERROR, "Gnome settings file error.", new Object[]{var3});
            throw new ProxyException(var3);
        }
    }

    private File findSettingsFile(String context) {
        File userDir = new File(System.getProperty("user.home"));
        StringBuilder path = new StringBuilder();
        String[] parts = context.split("/");
        String[] arr$ = parts;
        int len$ = parts.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String part = arr$[i$];
            path.append(part);
            path.append(File.separator);
        }

        File settingsFile = new File(userDir, ".gconf" + File.separator + path.toString() + "%gconf.xml");
        if (!settingsFile.exists()) {
            Logger.log(this.getClass(), LogLevel.WARNING, "Gnome settings: {0} not found.", new Object[]{settingsFile});
            return null;
        } else {
            return settingsFile;
        }
    }

    private ProxySelector setupFixedProxySelector(Properties settings) {
        if (!this.hasProxySettings(settings)) {
            return null;
        } else {
            ProtocolDispatchSelector ps = new ProtocolDispatchSelector();
            this.installHttpSelector(settings, ps);
            if (this.useForAllProtocols(settings)) {
                ps.setFallbackSelector(ps.getSelector("http"));
            } else {
                this.installSecureSelector(settings, ps);
                this.installFtpSelector(settings, ps);
                this.installSocksSelector(settings, ps);
            }

            return ps;
        }
    }

    private boolean useForAllProtocols(Properties settings) {
        return Boolean.parseBoolean(settings.getProperty("/system/http_proxy/use_same_proxy", "false"));
    }

    private boolean hasProxySettings(Properties settings) {
        String proxyHost = settings.getProperty("/system/http_proxy/host", (String)null);
        return proxyHost != null && proxyHost.length() > 0;
    }

    private void installHttpSelector(Properties settings, ProtocolDispatchSelector ps) throws NumberFormatException {
        String proxyHost = settings.getProperty("/system/http_proxy/host", (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("/system/http_proxy/port", "0").trim());
        if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome http proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector("http", new FixedProxySelector(proxyHost.trim(), proxyPort));
        }

    }

    private void installSocksSelector(Properties settings, ProtocolDispatchSelector ps) throws NumberFormatException {
        String proxyHost = settings.getProperty("/system/proxy/socks_host", (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("/system/proxy/socks_port", "0").trim());
        if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome socks proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector("socks", new FixedProxySelector(proxyHost.trim(), proxyPort));
        }

    }

    private void installFtpSelector(Properties settings, ProtocolDispatchSelector ps) throws NumberFormatException {
        String proxyHost = settings.getProperty("/system/proxy/ftp_host", (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("/system/proxy/ftp_port", "0").trim());
        if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome ftp proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector("ftp", new FixedProxySelector(proxyHost.trim(), proxyPort));
        }

    }

    private void installSecureSelector(Properties settings, ProtocolDispatchSelector ps) throws NumberFormatException {
        String proxyHost = settings.getProperty("/system/proxy/secure_host", (String)null);
        int proxyPort = Integer.parseInt(settings.getProperty("/system/proxy/secure_port", "0").trim());
        if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
            Logger.log(this.getClass(), LogLevel.TRACE, "Gnome secure proxy is {0}:{1}", new Object[]{proxyHost, proxyPort});
            ps.setSelector("https", new FixedProxySelector(proxyHost.trim(), proxyPort));
            ps.setSelector("sftp", new FixedProxySelector(proxyHost.trim(), proxyPort));
        }

    }

    private Properties parseSettings(String context, Properties settings) throws IOException {
        File settingsFile = this.findSettingsFile(context);
        if (settingsFile == null) {
            return settings;
        } else {
            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                documentBuilder.setEntityResolver(new EmptyXMLResolver());
                Document doc = documentBuilder.parse(settingsFile);
                Element root = doc.getDocumentElement();

                for(Node entry = root.getFirstChild(); entry != null; entry = entry.getNextSibling()) {
                    if ("entry".equals(entry.getNodeName()) && entry instanceof Element) {
                        String entryName = ((Element)entry).getAttribute("name");
                        settings.setProperty(context + entryName, this.getEntryValue((Element)entry));
                    }
                }

                return settings;
            } catch (SAXException var9) {
                Logger.log(this.getClass(), LogLevel.ERROR, "Gnome settings parse error", new Object[]{var9});
                throw new IOException(var9.getMessage());
            } catch (ParserConfigurationException var10) {
                Logger.log(this.getClass(), LogLevel.ERROR, "Gnome settings parse error", new Object[]{var10});
                throw new IOException(var10.getMessage());
            }
        }
    }

    private String getEntryValue(Element entry) {
        String type = entry.getAttribute("type");
        if (!"int".equals(type) && !"bool".equals(type)) {
            if ("string".equals(type)) {
                NodeList list = entry.getElementsByTagName("stringvalue");
                if (list.getLength() > 0) {
                    return list.item(0).getTextContent();
                }
            }

            if ("list".equals(type)) {
                StringBuilder result = new StringBuilder();
                NodeList list = entry.getElementsByTagName("li");

                for(int i = 0; i < list.getLength(); ++i) {
                    if (result.length() > 0) {
                        result.append(",");
                    }

                    result.append(this.getEntryValue((Element)list.item(i)));
                }

                return result.toString();
            } else {
                return null;
            }
        } else {
            return entry.getAttribute("value");
        }
    }
}
