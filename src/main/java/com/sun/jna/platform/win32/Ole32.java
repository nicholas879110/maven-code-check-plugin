//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.GUID.ByReference;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Ole32 extends StdCallLibrary {
    Ole32 INSTANCE = (Ole32)Native.loadLibrary("Ole32", Ole32.class, W32APIOptions.UNICODE_OPTIONS);

    HRESULT CoCreateGuid(ByReference var1);

    int StringFromGUID2(ByReference var1, char[] var2, int var3);

    HRESULT IIDFromString(String var1, ByReference var2);

    HRESULT CoInitializeEx(Pointer var1, int var2);

    void CoUninitialize();

    HRESULT CoCreateInstance(GUID var1, Pointer var2, int var3, GUID var4, PointerByReference var5);
}
