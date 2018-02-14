//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.GUID.ByReference;
import com.sun.jna.platform.win32.WinNT.HRESULT;

public abstract class Ole32Util {
    public Ole32Util() {
    }

    public static GUID getGUIDFromString(String guidString) {
        ByReference lpiid = new ByReference();
        HRESULT hr = Ole32.INSTANCE.IIDFromString(guidString, lpiid);
        if (!hr.equals(W32Errors.S_OK)) {
            throw new RuntimeException(hr.toString());
        } else {
            return lpiid;
        }
    }

    public static String getStringFromGUID(GUID guid) {
        ByReference pguid = new ByReference(guid.getPointer());
        int max = 39;
        char[] lpsz = new char[max];
        int len = Ole32.INSTANCE.StringFromGUID2(pguid, lpsz, max);
        if (len == 0) {
            throw new RuntimeException("StringFromGUID2");
        } else {
            lpsz[len - 1] = 0;
            return Native.toString(lpsz);
        }
    }

    public static GUID generateGUID() {
        ByReference pguid = new ByReference();
        HRESULT hr = Ole32.INSTANCE.CoCreateGuid(pguid);
        if (!hr.equals(W32Errors.S_OK)) {
            throw new RuntimeException(hr.toString());
        } else {
            return pguid;
        }
    }
}
