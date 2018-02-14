//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.browser.ie;

import com.btr.proxy.util.UriFilter;
import java.net.URI;

public class IELocalByPassFilter implements UriFilter {
    public IELocalByPassFilter() {
    }

    public boolean accept(URI uri) {
        if (uri == null) {
            return false;
        } else {
            String host = uri.getAuthority();
            return host != null && !host.contains(".");
        }
    }
}
