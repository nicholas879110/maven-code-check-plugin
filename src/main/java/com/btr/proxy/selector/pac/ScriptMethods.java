//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

public interface ScriptMethods {
    boolean isPlainHostName(String var1);

    boolean dnsDomainIs(String var1, String var2);

    boolean localHostOrDomainIs(String var1, String var2);

    boolean isResolvable(String var1);

    boolean isResolvableEx(String var1);

    boolean isInNet(String var1, String var2, String var3);

    boolean isInNetEx(String var1, String var2);

    String dnsResolve(String var1);

    String dnsResolveEx(String var1);

    String myIpAddress();

    String myIpAddressEx();

    int dnsDomainLevels(String var1);

    boolean shExpMatch(String var1, String var2);

    boolean weekdayRange(String var1, String var2, String var3);

    boolean dateRange(Object var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7);

    boolean timeRange(Object var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7);

    String sortIpAddressList(String var1);

    String getClientVersion();
}
