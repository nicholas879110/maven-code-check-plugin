//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.whitelist;

import com.btr.proxy.util.UriFilter;
import java.net.URI;

public class HostnameFilter implements UriFilter {
    private static final String PROTOCOL_ENDING = "://";
    private String matchTo;
    private String protocolFilter;
    private HostnameFilter.Mode mode;

    public HostnameFilter(HostnameFilter.Mode mode, String matchTo) {
        this.mode = mode;
        this.matchTo = matchTo.toLowerCase();
        this.extractProtocolFilter();
    }

    private void extractProtocolFilter() {
        int protocolIndex = this.matchTo.indexOf("://");
        if (protocolIndex != -1) {
            this.protocolFilter = this.matchTo.substring(0, protocolIndex);
            this.matchTo = this.matchTo.substring(protocolIndex + "://".length());
        }

    }

    public boolean accept(URI uri) {
        if (uri != null && uri.getAuthority() != null) {
            if (!this.isProtocolMatching(uri)) {
                return false;
            } else {
                String host = uri.getAuthority();
                int index = host.indexOf(58);
                if (index != -1) {
                    host = host.substring(0, index);
                }

                switch(this.mode) {
                    case BEGINS_WITH:
                        return host.toLowerCase().startsWith(this.matchTo);
                    case ENDS_WITH:
                        return host.toLowerCase().endsWith(this.matchTo);
                    case REGEX:
                        return host.toLowerCase().matches(this.matchTo);
                    default:
                        return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean isProtocolMatching(URI uri) {
        return this.protocolFilter == null || uri.getScheme() == null || uri.getScheme().equalsIgnoreCase(this.protocolFilter);
    }

    public static enum Mode {
        BEGINS_WITH,
        ENDS_WITH,
        REGEX;

        private Mode() {
        }
    }
}
