//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class PacScriptMethods implements ScriptMethods {
    public static final String OVERRIDE_LOCAL_IP = "com.btr.proxy.pac.overrideLocalIP";
    private static final String GMT = "GMT";
    private static final List<String> DAYS = Collections.unmodifiableList(Arrays.asList("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"));
    private static final List<String> MONTH = Collections.unmodifiableList(Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"));
    private Calendar currentTime;

    public PacScriptMethods() {
    }

    public boolean isPlainHostName(String host) {
        return host.indexOf(".") < 0;
    }

    public boolean dnsDomainIs(String host, String domain) {
        return host.endsWith(domain);
    }

    public boolean localHostOrDomainIs(String host, String domain) {
        return domain.startsWith(host);
    }

    public boolean isResolvable(String host) {
        try {
            InetAddress.getByName(host).getHostAddress();
            return true;
        } catch (UnknownHostException var3) {
            Logger.log(JavaxPacScriptParser.class, LogLevel.DEBUG, "Hostname not resolveable {0}.", new Object[]{host});
            return false;
        }
    }

    public boolean isInNet(String host, String pattern, String mask) {
        host = this.dnsResolve(host);
        if (host != null && host.length() != 0) {
            long lhost = this.parseIpAddressToLong(host);
            long lpattern = this.parseIpAddressToLong(pattern);
            long lmask = this.parseIpAddressToLong(mask);
            return (lhost & lmask) == lpattern;
        } else {
            return false;
        }
    }

    private long parseIpAddressToLong(String address) {
        long result = 0L;
        String[] parts = address.split("\\.");
        long shift = 24L;
        String[] arr$ = parts;
        int len$ = parts.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String part = arr$[i$];
            long lpart = Long.parseLong(part);
            result |= lpart << (int)shift;
            shift -= 8L;
        }

        return result;
    }

    public String dnsResolve(String host) {
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException var3) {
            Logger.log(JavaxPacScriptParser.class, LogLevel.DEBUG, "DNS name not resolvable {0}.", new Object[]{host});
            return "";
        }
    }

    public String myIpAddress() {
        return this.getLocalAddressOfType(Inet4Address.class);
    }

    private String getLocalAddressOfType(Class<? extends InetAddress> cl) {
        try {
            String overrideIP = System.getProperty("com.btr.proxy.pac.overrideLocalIP");
            if (overrideIP != null && overrideIP.trim().length() > 0) {
                return overrideIP.trim();
            } else {
                Enumeration interfaces = NetworkInterface.getNetworkInterfaces();

                while(true) {
                    NetworkInterface current;
                    do {
                        do {
                            do {
                                if (!interfaces.hasMoreElements()) {
                                    return "";
                                }

                                current = (NetworkInterface)interfaces.nextElement();
                            } while(!current.isUp());
                        } while(current.isLoopback());
                    } while(current.isVirtual());

                    Enumeration addresses = current.getInetAddresses();

                    while(addresses.hasMoreElements()) {
                        InetAddress adr = (InetAddress)addresses.nextElement();
                        if (cl.isInstance(adr)) {
                            Logger.log(JavaxPacScriptParser.class, LogLevel.TRACE, "Local address resolved to {0}", new Object[]{adr});
                            return adr.getHostAddress();
                        }
                    }
                }
            }
        } catch (IOException var7) {
            Logger.log(JavaxPacScriptParser.class, LogLevel.DEBUG, "Local address not resolvable.", new Object[0]);
            return "";
        }
    }

    public int dnsDomainLevels(String host) {
        int count = 0;

        for(int startPos = 0; (startPos = host.indexOf(".", startPos + 1)) > -1; ++count) {
            ;
        }

        return count;
    }

    public boolean shExpMatch(String str, String shexp) {
        StringTokenizer tokenizer = new StringTokenizer(shexp, "*");

        String token;
        int temp;
        for(int startPos = 0; tokenizer.hasMoreTokens(); startPos = temp + token.length()) {
            token = tokenizer.nextToken();
            temp = str.indexOf(token, startPos);
            if (temp == -1) {
                return false;
            }
        }

        return true;
    }

    public boolean weekdayRange(String wd1, String wd2, String gmt) {
        boolean useGmt = "GMT".equalsIgnoreCase(wd2) || "GMT".equalsIgnoreCase(gmt);
        Calendar cal = this.getCurrentTime(useGmt);
        int currentDay = cal.get(7) - 1;
        int from = DAYS.indexOf(wd1 == null ? null : wd1.toUpperCase());
        int to = DAYS.indexOf(wd2 == null ? null : wd2.toUpperCase());
        if (to == -1) {
            to = from;
        }

        if (to < from) {
            return currentDay >= from || currentDay <= to;
        } else {
            return currentDay >= from && currentDay <= to;
        }
    }

    public void setCurrentTime(Calendar cal) {
        this.currentTime = cal;
    }

    private Calendar getCurrentTime(boolean useGmt) {
        return this.currentTime != null ? (Calendar)this.currentTime.clone() : Calendar.getInstance(useGmt ? TimeZone.getTimeZone("GMT") : TimeZone.getDefault());
    }

    public boolean dateRange(Object day1, Object month1, Object year1, Object day2, Object month2, Object year2, Object gmt) {
        Map<String, Integer> params = new HashMap();
        this.parseDateParam(params, day1);
        this.parseDateParam(params, month1);
        this.parseDateParam(params, year1);
        this.parseDateParam(params, day2);
        this.parseDateParam(params, month2);
        this.parseDateParam(params, year2);
        this.parseDateParam(params, gmt);
        boolean useGmt = params.get("gmt") != null;
        Calendar cal = this.getCurrentTime(useGmt);
        Date current = cal.getTime();
        if (params.get("day1") != null) {
            cal.set(5, ((Integer)params.get("day1")).intValue());
        }

        if (params.get("month1") != null) {
            cal.set(2, ((Integer)params.get("month1")).intValue());
        }

        if (params.get("year1") != null) {
            cal.set(1, ((Integer)params.get("year1")).intValue());
        }

        Date from = cal.getTime();
        if (params.get("day2") != null) {
            cal.set(5, ((Integer)params.get("day2")).intValue());
        }

        if (params.get("month2") != null) {
            cal.set(2, ((Integer)params.get("month2")).intValue());
        }

        if (params.get("year2") != null) {
            cal.set(1, ((Integer)params.get("year2")).intValue());
        }

        Date to = cal.getTime();
        if (to.before(from)) {
            cal.add(2, 1);
            to = cal.getTime();
        }

        if (to.before(from)) {
            cal.add(1, 1);
            cal.add(2, -1);
            to = cal.getTime();
        }

        return current.compareTo(from) >= 0 && current.compareTo(to) <= 0;
    }

    private void parseDateParam(Map<String, Integer> params, Object value) {
        int n;
        if (value instanceof Number) {
            n = ((Number)value).intValue();
            if (n <= 31) {
                if (params.get("day1") == null) {
                    params.put("day1", n);
                } else {
                    params.put("day2", n);
                }
            } else if (params.get("year1") == null) {
                params.put("year1", n);
            } else {
                params.put("year2", n);
            }
        }

        if (value instanceof String) {
            n = MONTH.indexOf(((String)value).toUpperCase());
            if (n > -1) {
                if (params.get("month1") == null) {
                    params.put("month1", n);
                } else {
                    params.put("month2", n);
                }
            }
        }

        if ("GMT".equalsIgnoreCase(String.valueOf(value))) {
            params.put("gmt", Integer.valueOf(1));
        }

    }

    public boolean timeRange(Object hour1, Object min1, Object sec1, Object hour2, Object min2, Object sec2, Object gmt) {
        boolean useGmt = "GMT".equalsIgnoreCase(String.valueOf(min1)) || "GMT".equalsIgnoreCase(String.valueOf(sec1)) || "GMT".equalsIgnoreCase(String.valueOf(min2)) || "GMT".equalsIgnoreCase(String.valueOf(gmt));
        Calendar cal = this.getCurrentTime(useGmt);
        cal.set(14, 0);
        Date current = cal.getTime();
        Date from;
        Date to;
        if (sec2 instanceof Number) {
            cal.set(11, ((Number)hour1).intValue());
            cal.set(12, ((Number)min1).intValue());
            cal.set(13, ((Number)sec1).intValue());
            from = cal.getTime();
            cal.set(11, ((Number)hour2).intValue());
            cal.set(12, ((Number)min2).intValue());
            cal.set(13, ((Number)sec2).intValue());
            to = cal.getTime();
        } else if (hour2 instanceof Number) {
            cal.set(11, ((Number)hour1).intValue());
            cal.set(12, ((Number)min1).intValue());
            cal.set(13, 0);
            from = cal.getTime();
            cal.set(11, ((Number)sec1).intValue());
            cal.set(12, ((Number)hour2).intValue());
            cal.set(13, 59);
            to = cal.getTime();
        } else if (min1 instanceof Number) {
            cal.set(11, ((Number)hour1).intValue());
            cal.set(12, 0);
            cal.set(13, 0);
            from = cal.getTime();
            cal.set(11, ((Number)min1).intValue());
            cal.set(12, 59);
            cal.set(13, 59);
            to = cal.getTime();
        } else {
            cal.set(11, ((Number)hour1).intValue());
            cal.set(12, 0);
            cal.set(13, 0);
            from = cal.getTime();
            cal.set(11, ((Number)hour1).intValue());
            cal.set(12, 59);
            cal.set(13, 59);
            to = cal.getTime();
        }

        if (to.before(from)) {
            cal.setTime(to);
            cal.add(5, 1);
            to = cal.getTime();
        }

        return current.compareTo(from) >= 0 && current.compareTo(to) <= 0;
    }

    public boolean isResolvableEx(String host) {
        return this.isResolvable(host);
    }

    public boolean isInNetEx(String ipAddress, String ipPrefix) {
        return false;
    }

    public String dnsResolveEx(String host) {
        StringBuilder result = new StringBuilder();

        try {
            InetAddress[] list = InetAddress.getAllByName(host);
            InetAddress[] arr$ = list;
            int len$ = list.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                InetAddress inetAddress = arr$[i$];
                result.append(inetAddress.getHostAddress());
                result.append("; ");
            }
        } catch (UnknownHostException var8) {
            Logger.log(JavaxPacScriptParser.class, LogLevel.DEBUG, "DNS name not resolvable {0}.", new Object[]{host});
        }

        return result.toString();
    }

    public String myIpAddressEx() {
        return this.getLocalAddressOfType(Inet6Address.class);
    }

    public String sortIpAddressList(String ipAddressList) {
        if (ipAddressList != null && ipAddressList.trim().length() != 0) {
            String[] ipAddressToken = ipAddressList.split(";");
            List<InetAddress> parsedAddresses = new ArrayList();
            String[] arr$ = ipAddressToken;
            int len$ = ipAddressToken.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String ip = arr$[i$];

                try {
                    parsedAddresses.add(InetAddress.getByName(ip));
                } catch (UnknownHostException var9) {
                    var9.printStackTrace();
                }
            }

            Collections.sort(parsedAddresses, (Comparator)null);
            return ipAddressList;
        } else {
            return "";
        }
    }

    public String getClientVersion() {
        return "1.0";
    }
}
