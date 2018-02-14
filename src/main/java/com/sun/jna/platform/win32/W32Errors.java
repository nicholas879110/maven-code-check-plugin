//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.platform.win32.WinNT.HRESULT;

public abstract class W32Errors implements WinError {
    public W32Errors() {
    }

    public static final boolean SUCCEEDED(int hr) {
        return hr >= 0;
    }

    public static final boolean FAILED(int hr) {
        return hr < 0;
    }

    public static final int HRESULT_CODE(int hr) {
        return hr & '\uffff';
    }

    public static final int SCODE_CODE(int sc) {
        return sc & '\uffff';
    }

    public static final int HRESULT_FACILITY(int hr) {
        return (hr >>= 16) & 8191;
    }

    public static final int SCODE_FACILITY(short sc) {
        return (sc = (short)(sc >> 16)) & 8191;
    }

    public static short HRESULT_SEVERITY(int hr) {
        return (short)((hr >>= 31) & 1);
    }

    public static short SCODE_SEVERITY(short sc) {
        return (short)((sc = (short)(sc >> 31)) & 1);
    }

    public static int MAKE_HRESULT(short sev, short fac, short code) {
        return sev << 31 | fac << 16 | code;
    }

    public static final int MAKE_SCODE(short sev, short fac, short code) {
        return sev << 31 | fac << 16 | code;
    }

    public static final HRESULT HRESULT_FROM_WIN32(int x) {
        int f = 7;
        return new HRESULT(x <= 0 ? x : x & '\uffff' | (f = f << 16) | -2147483648);
    }

    public static final int FILTER_HRESULT_FROM_FLT_NTSTATUS(int x) {
        int f = 31;
        return x & -2147418113 | (f = f << 16);
    }
}
