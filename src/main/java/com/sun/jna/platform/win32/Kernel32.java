//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase.OVERLAPPED;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import java.nio.Buffer;

public interface Kernel32 extends WinNT {
    Kernel32 INSTANCE = (Kernel32)Native.loadLibrary("kernel32", Kernel32.class, W32APIOptions.UNICODE_OPTIONS);

    int FormatMessage(int var1, Pointer var2, int var3, int var4, Buffer var5, int var6, Pointer var7);

    boolean ReadFile(HANDLE var1, Buffer var2, int var3, IntByReference var4, OVERLAPPED var5);
}
