//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.whitelist;

import com.btr.proxy.util.UriFilter;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public class IpRangeFilter implements UriFilter {
    private byte[] matchTo;
    int numOfBits;

    public IpRangeFilter(String matchTo) {
        String[] parts = matchTo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("IP range is not valid:" + matchTo);
        } else {
            try {
                InetAddress address = InetAddress.getByName(parts[0].trim());
                this.matchTo = address.getAddress();
            } catch (UnknownHostException var4) {
                throw new IllegalArgumentException("IP range is not valid:" + matchTo);
            }

            this.numOfBits = Integer.parseInt(parts[1].trim());
        }
    }

    public boolean accept(URI uri) {
        if (uri != null && uri.getHost() != null) {
            try {
                InetAddress address = InetAddress.getByName(uri.getHost());
                byte[] addr = address.getAddress();
                if (addr.length != this.matchTo.length) {
                    return false;
                }

                int bit = 0;

                for(int nibble = 0; nibble < addr.length; ++nibble) {
                    for(int nibblePos = 7; nibblePos >= 0; --nibblePos) {
                        int mask = 1 << nibblePos;
                        if ((this.matchTo[nibble] & mask) != (addr[nibble] & mask)) {
                            return false;
                        }

                        ++bit;
                        if (bit >= this.numOfBits) {
                            return true;
                        }
                    }
                }
            } catch (UnknownHostException var8) {
                ;
            }

            return false;
        } else {
            return false;
        }
    }
}
