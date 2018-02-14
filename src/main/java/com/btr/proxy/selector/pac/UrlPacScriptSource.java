//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class UrlPacScriptSource implements PacScriptSource {
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    private static final int DEFAULT_READ_TIMEOUT = 20000;
    public static final String OVERRIDE_CONNECT_TIMEOUT = "com.btr.proxy.url.connectTimeout";
    public static final String OVERRIDE_READ_TIMEOUT = "com.btr.proxy.url.readTimeout";
    private final String scriptUrl;
    private String scriptContent;
    private long expireAtMillis = 0L;

    public UrlPacScriptSource(String url) {
        this.scriptUrl = url;
    }

    public synchronized String getScriptContent() throws IOException {
        if (this.scriptContent == null || this.expireAtMillis > 0L && this.expireAtMillis > System.currentTimeMillis()) {
            try {
                if (!this.scriptUrl.startsWith("file:/") && this.scriptUrl.indexOf(":/") != -1) {
                    this.scriptContent = this.downloadPacContent(this.scriptUrl);
                } else {
                    this.scriptContent = this.readPacFileContent(this.scriptUrl);
                }
            } catch (IOException var2) {
                Logger.log(this.getClass(), LogLevel.ERROR, "Loading script failed from: {0} with error {1}", new Object[]{this.scriptUrl, var2});
                this.scriptContent = "";
                throw var2;
            }
        }

        return this.scriptContent;
    }

    private String readPacFileContent(String scriptUrl) throws IOException {
        try {
            File file = null;
            if (scriptUrl.indexOf(":/") == -1) {
                file = new File(scriptUrl);
            } else {
                file = new File((new URL(scriptUrl)).toURI());
            }

            BufferedReader r = new BufferedReader(new FileReader(file));
            StringBuilder result = new StringBuilder();

            String line;
            try {
                while((line = r.readLine()) != null) {
                    result.append(line).append("\n");
                }
            } finally {
                r.close();
            }

            return result.toString();
        } catch (Exception var10) {
            Logger.log(this.getClass(), LogLevel.ERROR, "File reading error.", new Object[]{var10});
            throw new IOException(var10.getMessage());
        }
    }

    private String downloadPacContent(String url) throws IOException {
        if (url == null) {
            throw new IOException("Invalid PAC script URL: null");
        } else {
            this.setPacProxySelectorEnabled(false);
            HttpURLConnection con = null;

            String var5;
            try {
                con = this.setupHTTPConnection(url);
                if (con.getResponseCode() != 200) {
                    throw new IOException("Server returned: " + con.getResponseCode() + " " + con.getResponseMessage());
                }

                this.expireAtMillis = con.getExpiration();
                BufferedReader r = this.getReader(con);
                String result = this.readAllContent(r);
                r.close();
                var5 = result;
            } finally {
                this.setPacProxySelectorEnabled(true);
                if (con != null) {
                    con.disconnect();
                }

            }

            return var5;
        }
    }

    private void setPacProxySelectorEnabled(boolean enable) {
        PacProxySelector.setEnabled(enable);
    }

    private String readAllContent(BufferedReader r) throws IOException {
        StringBuilder result = new StringBuilder();

        String line;
        while((line = r.readLine()) != null) {
            result.append(line).append("\n");
        }

        return result.toString();
    }

    private BufferedReader getReader(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
        String charsetName = this.parseCharsetFromHeader(con.getContentType());
        BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), charsetName));
        return r;
    }

    private HttpURLConnection setupHTTPConnection(String url) throws IOException, MalformedURLException {
        HttpURLConnection con = (HttpURLConnection)(new URL(url)).openConnection(Proxy.NO_PROXY);
        con.setConnectTimeout(this.getTimeOut("com.btr.proxy.url.connectTimeout", 15000));
        con.setReadTimeout(this.getTimeOut("com.btr.proxy.url.readTimeout", 20000));
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("accept", "application/x-ns-proxy-autoconfig, */*;q=0.8");
        return con;
    }

    protected int getTimeOut(String overrideProperty, int defaultValue) {
        int timeout = defaultValue;
        String prop = System.getProperty(overrideProperty);
        if (prop != null && prop.trim().length() > 0) {
            try {
                timeout = Integer.parseInt(prop.trim());
            } catch (NumberFormatException var6) {
                Logger.log(this.getClass(), LogLevel.DEBUG, "Invalid override property : {0}={1}", new Object[]{overrideProperty, prop});
            }
        }

        return timeout;
    }

    String parseCharsetFromHeader(String contentType) {
        String result = "ISO-8859-1";
        if (contentType != null) {
            String[] paramList = contentType.split(";");
            String[] arr$ = paramList;
            int len$ = paramList.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String param = arr$[i$];
                if (param.toLowerCase().trim().startsWith("charset") && param.indexOf("=") != -1) {
                    result = param.substring(param.indexOf("=") + 1).trim();
                }
            }
        }

        return result;
    }

    public String toString() {
        return this.scriptUrl;
    }

    public boolean isScriptValid() {
        try {
            String script = this.getScriptContent();
            if (script != null && script.trim().length() != 0) {
                if (script.indexOf("FindProxyForURL") == -1) {
                    Logger.log(this.getClass(), LogLevel.DEBUG, "PAC script entry point FindProxyForURL not found. Skipping script!", new Object[0]);
                    return false;
                } else {
                    return true;
                }
            } else {
                Logger.log(this.getClass(), LogLevel.DEBUG, "PAC script is empty. Skipping script!", new Object[0]);
                return false;
            }
        } catch (IOException var2) {
            Logger.log(this.getClass(), LogLevel.DEBUG, "File reading error: {0}", new Object[]{var2});
            return false;
        }
    }
}
