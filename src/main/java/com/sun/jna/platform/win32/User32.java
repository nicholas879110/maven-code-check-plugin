//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure.ByReference;
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HICON;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HRGN;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.BLENDFUNCTION;
import com.sun.jna.platform.win32.WinUser.FLASHWINFO;
import com.sun.jna.platform.win32.WinUser.GUITHREADINFO;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.POINT;
import com.sun.jna.platform.win32.WinUser.SIZE;
import com.sun.jna.platform.win32.WinUser.WINDOWINFO;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface User32 extends StdCallLibrary, WinUser {
    User32 INSTANCE = (User32)Native.loadLibrary("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

    HDC GetDC(HWND var1);

    int ReleaseDC(HWND var1, HDC var2);

    HWND FindWindow(String var1, String var2);

    int GetClassName(HWND var1, char[] var2, int var3);

    boolean GetGUIThreadInfo(int var1, GUITHREADINFO var2);

    boolean GetWindowInfo(HWND var1, WINDOWINFO var2);

    boolean GetWindowRect(HWND var1, RECT var2);

    int GetWindowText(HWND var1, char[] var2, int var3);

    int GetWindowTextLength(HWND var1);

    int GetWindowModuleFileName(HWND var1, char[] var2, int var3);

    int GetWindowThreadProcessId(HWND var1, IntByReference var2);

    boolean EnumWindows(WNDENUMPROC var1, Pointer var2);

    boolean EnumChildWindows(HWND var1, WNDENUMPROC var2, Pointer var3);

    boolean EnumThreadWindows(int var1, WNDENUMPROC var2, Pointer var3);

    boolean FlashWindowEx(FLASHWINFO var1);

    HICON LoadIcon(HINSTANCE var1, String var2);

    HANDLE LoadImage(HINSTANCE var1, String var2, int var3, int var4, int var5, int var6);

    boolean DestroyIcon(HICON var1);

    int GetWindowLong(HWND var1, int var2);

    int SetWindowLong(HWND var1, int var2, int var3);

    Pointer SetWindowLong(HWND var1, int var2, Pointer var3);

    LONG_PTR GetWindowLongPtr(HWND var1, int var2);

    LONG_PTR SetWindowLongPtr(HWND var1, int var2, LONG_PTR var3);

    Pointer SetWindowLongPtr(HWND var1, int var2, Pointer var3);

    boolean SetLayeredWindowAttributes(HWND var1, int var2, byte var3, int var4);

    boolean GetLayeredWindowAttributes(HWND var1, IntByReference var2, ByteByReference var3, IntByReference var4);

    boolean UpdateLayeredWindow(HWND var1, HDC var2, POINT var3, SIZE var4, HDC var5, POINT var6, int var7, BLENDFUNCTION var8, int var9);

    int SetWindowRgn(HWND var1, HRGN var2, boolean var3);

    boolean GetKeyboardState(byte[] var1);

    short GetAsyncKeyState(int var1);

    HHOOK SetWindowsHookEx(int var1, HOOKPROC var2, HINSTANCE var3, int var4);

    LRESULT CallNextHookEx(HHOOK var1, int var2, WPARAM var3, LPARAM var4);

    LRESULT CallNextHookEx(HHOOK var1, int var2, WPARAM var3, Pointer var4);

    boolean UnhookWindowsHookEx(HHOOK var1);

    int GetMessage(MSG var1, HWND var2, int var3, int var4);

    boolean PeekMessage(MSG var1, HWND var2, int var3, int var4, int var5);

    boolean TranslateMessage(MSG var1);

    LRESULT DispatchMessage(MSG var1);

    void PostMessage(HWND var1, int var2, WPARAM var3, LPARAM var4);

    void PostQuitMessage(int var1);

    int GetSystemMetrics(int var1);

    HWND SetParent(HWND var1, HWND var2);

    boolean IsWindowVisible(HWND var1);

    boolean MoveWindow(HWND var1, int var2, int var3, int var4, int var5, boolean var6);

    boolean SetWindowPos(HWND var1, HWND var2, int var3, int var4, int var5, int var6, int var7);

    boolean AttachThreadInput(DWORD var1, DWORD var2, boolean var3);

    boolean SetForegroundWindow(HWND var1);

    HWND GetForegroundWindow();

    HWND SetFocus(HWND var1);

    DWORD SendInput(DWORD var1, INPUT[] var2, int var3);

    DWORD WaitForInputIdle(HANDLE var1, DWORD var2);

    boolean InvalidateRect(HWND var1, ByReference var2, boolean var3);

    boolean RedrawWindow(HWND var1, ByReference var2, HRGN var3, DWORD var4);

    HWND GetWindow(HWND var1, DWORD var2);

    boolean UpdateWindow(HWND var1);

    boolean ShowWindow(HWND var1, int var2);

    boolean CloseWindow(HWND var1);

    boolean RegisterHotKey(HWND var1, int var2, int var3, int var4);

    boolean UnregisterHotKey(Pointer var1, int var2);
}
