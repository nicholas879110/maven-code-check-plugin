//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.platform.win32.WinNT.HRESULT;

public class Win32Exception extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private HRESULT _hr;

    public HRESULT getHR() {
        return this._hr;
    }

    public Win32Exception(HRESULT hr) {
        super(Kernel32Util.formatMessageFromHR(hr));
        this._hr = hr;
    }

    public Win32Exception(int code) {
        this(W32Errors.HRESULT_FROM_WIN32(code));
    }
}
