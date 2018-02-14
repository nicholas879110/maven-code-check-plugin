//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HRGN;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFOHEADER;
import com.sun.jna.platform.win32.WinGDI.RGNDATA;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.POINT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface GDI32 extends StdCallLibrary {
    GDI32 INSTANCE = (GDI32)Native.loadLibrary("gdi32", GDI32.class, W32APIOptions.DEFAULT_OPTIONS);

    HRGN ExtCreateRegion(Pointer var1, int var2, RGNDATA var3);

    int CombineRgn(HRGN var1, HRGN var2, HRGN var3, int var4);

    HRGN CreateRectRgn(int var1, int var2, int var3, int var4);

    HRGN CreateRoundRectRgn(int var1, int var2, int var3, int var4, int var5, int var6);

    HRGN CreatePolyPolygonRgn(POINT[] var1, int[] var2, int var3, int var4);

    boolean SetRectRgn(HRGN var1, int var2, int var3, int var4, int var5);

    int SetPixel(HDC var1, int var2, int var3, int var4);

    HDC CreateCompatibleDC(HDC var1);

    boolean DeleteDC(HDC var1);

    HBITMAP CreateDIBitmap(HDC var1, BITMAPINFOHEADER var2, int var3, Pointer var4, BITMAPINFO var5, int var6);

    HBITMAP CreateDIBSection(HDC var1, BITMAPINFO var2, int var3, PointerByReference var4, Pointer var5, int var6);

    HBITMAP CreateCompatibleBitmap(HDC var1, int var2, int var3);

    HANDLE SelectObject(HDC var1, HANDLE var2);

    boolean DeleteObject(HANDLE var1);

    int GetDeviceCaps(HDC var1, int var2);

    int GetDIBits(HDC var1, HBITMAP var2, int var3, int var4, Pointer var5, BITMAPINFO var6, int var7);
}
