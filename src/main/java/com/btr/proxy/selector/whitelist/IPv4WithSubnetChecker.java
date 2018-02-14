//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.whitelist;

import java.util.regex.Pattern;

public class IPv4WithSubnetChecker {
    private static Pattern IP_SUB_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])/(\\d|([12]\\d|3[0-2]))$");

    public IPv4WithSubnetChecker() {
    }

    public static boolean isValid(String possibleIPAddress) {
        return IP_SUB_PATTERN.matcher(possibleIPAddress).matches();
    }
}
