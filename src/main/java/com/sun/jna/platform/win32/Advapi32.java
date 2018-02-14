//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.SECURITY_ATTRIBUTES;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinNT.LUID;
import com.sun.jna.platform.win32.WinNT.PSID;
import com.sun.jna.platform.win32.WinNT.PSIDByReference;
import com.sun.jna.platform.win32.WinNT.TOKEN_PRIVILEGES;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.platform.win32.WinReg.HKEYByReference;
import com.sun.jna.platform.win32.Winsvc.SC_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_PROCESS;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Advapi32 extends StdCallLibrary {
    Advapi32 INSTANCE = (Advapi32)Native.loadLibrary("Advapi32", Advapi32.class, W32APIOptions.UNICODE_OPTIONS);

    boolean GetUserNameW(char[] var1, IntByReference var2);

    boolean LookupAccountName(String var1, String var2, PSID var3, IntByReference var4, char[] var5, IntByReference var6, PointerByReference var7);

    boolean LookupAccountSid(String var1, PSID var2, char[] var3, IntByReference var4, char[] var5, IntByReference var6, PointerByReference var7);

    boolean ConvertSidToStringSid(PSID var1, PointerByReference var2);

    boolean ConvertStringSidToSid(String var1, PSIDByReference var2);

    int GetLengthSid(PSID var1);

    boolean IsValidSid(PSID var1);

    boolean IsWellKnownSid(PSID var1, int var2);

    boolean CreateWellKnownSid(int var1, PSID var2, PSID var3, IntByReference var4);

    boolean LogonUser(String var1, String var2, String var3, int var4, int var5, HANDLEByReference var6);

    boolean OpenThreadToken(HANDLE var1, int var2, boolean var3, HANDLEByReference var4);

    boolean OpenProcessToken(HANDLE var1, int var2, HANDLEByReference var3);

    boolean DuplicateToken(HANDLE var1, int var2, HANDLEByReference var3);

    boolean DuplicateTokenEx(HANDLE var1, int var2, SECURITY_ATTRIBUTES var3, int var4, int var5, HANDLEByReference var6);

    boolean GetTokenInformation(HANDLE var1, int var2, Structure var3, int var4, IntByReference var5);

    boolean ImpersonateLoggedOnUser(HANDLE var1);

    boolean ImpersonateSelf(int var1);

    boolean RevertToSelf();

    int RegOpenKeyEx(HKEY var1, String var2, int var3, int var4, HKEYByReference var5);

    int RegQueryValueEx(HKEY var1, String var2, int var3, IntByReference var4, char[] var5, IntByReference var6);

    int RegQueryValueEx(HKEY var1, String var2, int var3, IntByReference var4, byte[] var5, IntByReference var6);

    int RegQueryValueEx(HKEY var1, String var2, int var3, IntByReference var4, IntByReference var5, IntByReference var6);

    int RegQueryValueEx(HKEY var1, String var2, int var3, IntByReference var4, LongByReference var5, IntByReference var6);

    int RegQueryValueEx(HKEY var1, String var2, int var3, IntByReference var4, Pointer var5, IntByReference var6);

    int RegCloseKey(HKEY var1);

    int RegDeleteValue(HKEY var1, String var2);

    int RegSetValueEx(HKEY var1, String var2, int var3, int var4, char[] var5, int var6);

    int RegSetValueEx(HKEY var1, String var2, int var3, int var4, byte[] var5, int var6);

    int RegCreateKeyEx(HKEY var1, String var2, int var3, String var4, int var5, int var6, SECURITY_ATTRIBUTES var7, HKEYByReference var8, IntByReference var9);

    int RegDeleteKey(HKEY var1, String var2);

    int RegEnumKeyEx(HKEY var1, int var2, char[] var3, IntByReference var4, IntByReference var5, char[] var6, IntByReference var7, FILETIME var8);

    int RegEnumValue(HKEY var1, int var2, char[] var3, IntByReference var4, IntByReference var5, IntByReference var6, byte[] var7, IntByReference var8);

    int RegQueryInfoKey(HKEY var1, char[] var2, IntByReference var3, IntByReference var4, IntByReference var5, IntByReference var6, IntByReference var7, IntByReference var8, IntByReference var9, IntByReference var10, IntByReference var11, FILETIME var12);

    HANDLE RegisterEventSource(String var1, String var2);

    boolean DeregisterEventSource(HANDLE var1);

    HANDLE OpenEventLog(String var1, String var2);

    boolean CloseEventLog(HANDLE var1);

    boolean GetNumberOfEventLogRecords(HANDLE var1, IntByReference var2);

    boolean ReportEvent(HANDLE var1, int var2, int var3, int var4, PSID var5, int var6, int var7, String[] var8, Pointer var9);

    boolean ClearEventLog(HANDLE var1, String var2);

    boolean BackupEventLog(HANDLE var1, String var2);

    HANDLE OpenBackupEventLog(String var1, String var2);

    boolean ReadEventLog(HANDLE var1, int var2, int var3, Pointer var4, int var5, IntByReference var6, IntByReference var7);

    boolean GetOldestEventLogRecord(HANDLE var1, IntByReference var2);

    boolean QueryServiceStatusEx(SC_HANDLE var1, int var2, SERVICE_STATUS_PROCESS var3, int var4, IntByReference var5);

    boolean ControlService(SC_HANDLE var1, int var2, SERVICE_STATUS var3);

    boolean StartService(SC_HANDLE var1, int var2, String[] var3);

    boolean CloseServiceHandle(SC_HANDLE var1);

    SC_HANDLE OpenService(SC_HANDLE var1, String var2, int var3);

    SC_HANDLE OpenSCManager(String var1, String var2, int var3);

    boolean CreateProcessAsUser(HANDLE var1, String var2, String var3, SECURITY_ATTRIBUTES var4, SECURITY_ATTRIBUTES var5, boolean var6, int var7, String var8, String var9, STARTUPINFO var10, PROCESS_INFORMATION var11);

    boolean AdjustTokenPrivileges(HANDLE var1, boolean var2, TOKEN_PRIVILEGES var3, int var4, TOKEN_PRIVILEGES var5, IntByReference var6);

    boolean LookupPrivilegeName(String var1, LUID var2, char[] var3, IntByReference var4);

    boolean LookupPrivilegeValue(String var1, String var2, LUID var3);

    boolean GetFileSecurity(WString var1, int var2, Pointer var3, int var4, IntByReference var5);
}
