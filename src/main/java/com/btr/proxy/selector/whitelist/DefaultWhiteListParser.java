//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.whitelist;

import com.btr.proxy.selector.whitelist.HostnameFilter.Mode;
import com.btr.proxy.util.UriFilter;
import java.util.ArrayList;
import java.util.List;

public class DefaultWhiteListParser implements WhiteListParser {
    public DefaultWhiteListParser() {
    }

    public List<UriFilter> parseWhiteList(String whiteList) {
        List<UriFilter> result = new ArrayList();
        String[] token = whiteList.split("[, ]+");

        for(int i = 0; i < token.length; ++i) {
            String tkn = token[i].trim();
            if (this.isIP4SubnetFilter(tkn)) {
                result.add(new IpRangeFilter(tkn));
            } else if (tkn.endsWith("*")) {
                tkn = tkn.substring(0, tkn.length() - 1);
                result.add(new HostnameFilter(Mode.BEGINS_WITH, tkn));
            } else if (tkn.trim().startsWith("*")) {
                tkn = tkn.substring(1);
                result.add(new HostnameFilter(Mode.ENDS_WITH, tkn));
            } else {
                result.add(new HostnameFilter(Mode.ENDS_WITH, tkn));
            }
        }

        return result;
    }

    private boolean isIP4SubnetFilter(String token) {
        return IPv4WithSubnetChecker.isValid(token);
    }
}
