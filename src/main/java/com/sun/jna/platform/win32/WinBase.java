//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDLONG;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.util.Date;

public interface WinBase extends StdCallLibrary, WinDef, BaseTSD {
    HANDLE INVALID_HANDLE_VALUE = new HANDLE(Pointer.createConstant(Pointer.SIZE == 8 ? -1L : 4294967295L));
    int WAIT_FAILED = -1;
    int WAIT_OBJECT_0 = 0;
    int WAIT_ABANDONED = 128;
    int WAIT_ABANDONED_0 = 128;
    int MAX_COMPUTERNAME_LENGTH = Platform.isMac() ? 15 : 31;
    int LOGON32_LOGON_INTERACTIVE = 2;
    int LOGON32_LOGON_NETWORK = 3;
    int LOGON32_LOGON_BATCH = 4;
    int LOGON32_LOGON_SERVICE = 5;
    int LOGON32_LOGON_UNLOCK = 7;
    int LOGON32_LOGON_NETWORK_CLEARTEXT = 8;
    int LOGON32_LOGON_NEW_CREDENTIALS = 9;
    int LOGON32_PROVIDER_DEFAULT = 0;
    int LOGON32_PROVIDER_WINNT35 = 1;
    int LOGON32_PROVIDER_WINNT40 = 2;
    int LOGON32_PROVIDER_WINNT50 = 3;
    int HANDLE_FLAG_INHERIT = 1;
    int HANDLE_FLAG_PROTECT_FROM_CLOSE = 2;
    int STARTF_USESHOWWINDOW = 1;
    int STARTF_USESIZE = 2;
    int STARTF_USEPOSITION = 4;
    int STARTF_USECOUNTCHARS = 8;
    int STARTF_USEFILLATTRIBUTE = 16;
    int STARTF_RUNFULLSCREEN = 32;
    int STARTF_FORCEONFEEDBACK = 64;
    int STARTF_FORCEOFFFEEDBACK = 128;
    int STARTF_USESTDHANDLES = 256;
    int DEBUG_PROCESS = 1;
    int DEBUG_ONLY_THIS_PROCESS = 2;
    int CREATE_SUSPENDED = 4;
    int DETACHED_PROCESS = 8;
    int CREATE_NEW_CONSOLE = 16;
    int CREATE_NEW_PROCESS_GROUP = 512;
    int CREATE_UNICODE_ENVIRONMENT = 1024;
    int CREATE_SEPARATE_WOW_VDM = 2048;
    int CREATE_SHARED_WOW_VDM = 4096;
    int CREATE_FORCEDOS = 8192;
    int INHERIT_PARENT_AFFINITY = 65536;
    int CREATE_PROTECTED_PROCESS = 262144;
    int EXTENDED_STARTUPINFO_PRESENT = 524288;
    int CREATE_BREAKAWAY_FROM_JOB = 16777216;
    int CREATE_PRESERVE_CODE_AUTHZ_LEVEL = 33554432;
    int CREATE_DEFAULT_ERROR_MODE = 67108864;
    int CREATE_NO_WINDOW = 134217728;
    int INVALID_FILE_SIZE = -1;
    int INVALID_SET_FILE_POINTER = -1;
    int INVALID_FILE_ATTRIBUTES = -1;
    int STILL_ACTIVE = 259;
    int LMEM_FIXED = 0;
    int LMEM_MOVEABLE = 2;
    int LMEM_NOCOMPACT = 16;
    int LMEM_NODISCARD = 32;
    int LMEM_ZEROINIT = 64;
    int LMEM_MODIFY = 128;
    int LMEM_DISCARDABLE = 3840;
    int LMEM_VALID_FLAGS = 3954;
    int LMEM_INVALID_HANDLE = 32768;
    int LHND = 66;
    int LPTR = 64;
    int LMEM_DISCARDED = 16384;
    int LMEM_LOCKCOUNT = 255;
    int FORMAT_MESSAGE_ALLOCATE_BUFFER = 256;
    int FORMAT_MESSAGE_IGNORE_INSERTS = 512;
    int FORMAT_MESSAGE_FROM_STRING = 1024;
    int FORMAT_MESSAGE_FROM_HMODULE = 2048;
    int FORMAT_MESSAGE_FROM_SYSTEM = 4096;
    int FORMAT_MESSAGE_ARGUMENT_ARRAY = 8192;
    int DRIVE_UNKNOWN = 0;
    int DRIVE_NO_ROOT_DIR = 1;
    int DRIVE_REMOVABLE = 2;
    int DRIVE_FIXED = 3;
    int DRIVE_REMOTE = 4;
    int DRIVE_CDROM = 5;
    int DRIVE_RAMDISK = 6;
    int INFINITE = -1;
    int MOVEFILE_COPY_ALLOWED = 2;
    int MOVEFILE_CREATE_HARDLINK = 16;
    int MOVEFILE_DELAY_UNTIL_REBOOT = 4;
    int MOVEFILE_FAIL_IF_NOT_TRACKABLE = 32;
    int MOVEFILE_REPLACE_EXISTING = 1;
    int MOVEFILE_WRITE_THROUGH = 8;

    public static class PROCESS_INFORMATION extends Structure {
        public HANDLE hProcess;
        public HANDLE hThread;
        public DWORD dwProcessId;
        public DWORD dwThreadId;

        public PROCESS_INFORMATION() {
        }

        public PROCESS_INFORMATION(Pointer memory) {
            super(memory);
            this.read();
        }

        public static class ByReference extends WinBase.PROCESS_INFORMATION implements com.sun.jna.Structure.ByReference {
            public ByReference() {
            }

            public ByReference(Pointer memory) {
                super(memory);
            }
        }
    }

    public static class STARTUPINFO extends Structure {
        public DWORD cb = new DWORD((long)this.size());
        public String lpReserved;
        public String lpDesktop;
        public String lpTitle;
        public DWORD dwX;
        public DWORD dwY;
        public DWORD dwXSize;
        public DWORD dwYSize;
        public DWORD dwXCountChars;
        public DWORD dwYCountChars;
        public DWORD dwFillAttribute;
        public int dwFlags;
        public WORD wShowWindow;
        public WORD cbReserved2;
        public ByteByReference lpReserved2;
        public HANDLE hStdInput;
        public HANDLE hStdOutput;
        public HANDLE hStdError;

        public STARTUPINFO() {
        }
    }

    public static class SECURITY_ATTRIBUTES extends Structure {
        public DWORD dwLength = new DWORD((long)this.size());
        public Pointer lpSecurityDescriptor;
        public boolean bInheritHandle;

        public SECURITY_ATTRIBUTES() {
        }
    }

    public static class MEMORYSTATUSEX extends Structure {
        public DWORD dwLength = new DWORD((long)this.size());
        public DWORD dwMemoryLoad;
        public DWORDLONG ullTotalPhys;
        public DWORDLONG ullAvailPhys;
        public DWORDLONG ullTotalPageFile;
        public DWORDLONG ullAvailPageFile;
        public DWORDLONG ullTotalVirtual;
        public DWORDLONG ullAvailVirtual;
        public DWORDLONG ullAvailExtendedVirtual;

        public MEMORYSTATUSEX() {
        }
    }

    public static class SYSTEM_INFO extends Structure {
        public WinBase.SYSTEM_INFO.UNION processorArchitecture;
        public DWORD dwPageSize;
        public Pointer lpMinimumApplicationAddress;
        public Pointer lpMaximumApplicationAddress;
        public DWORD_PTR dwActiveProcessorMask;
        public DWORD dwNumberOfProcessors;
        public DWORD dwProcessorType;
        public DWORD dwAllocationGranularity;
        public WORD wProcessorLevel;
        public WORD wProcessorRevision;

        public SYSTEM_INFO() {
        }

        public static class UNION extends Union {
            public DWORD dwOemID;
            public WinBase.SYSTEM_INFO.PI pi;

            public UNION() {
            }

            public static class ByReference extends WinBase.SYSTEM_INFO.UNION implements com.sun.jna.Structure.ByReference {
                public ByReference() {
                }
            }
        }

        public static class PI extends Structure {
            public WORD wProcessorArchitecture;
            public WORD wReserved;

            public PI() {
            }

            public static class ByReference extends WinBase.SYSTEM_INFO.PI implements com.sun.jna.Structure.ByReference {
                public ByReference() {
                }
            }
        }
    }

    public static class OVERLAPPED extends Structure {
        public ULONG_PTR Internal;
        public ULONG_PTR InternalHigh;
        public int Offset;
        public int OffsetHigh;
        public HANDLE hEvent;

        public OVERLAPPED() {
        }
    }

    public static class SYSTEMTIME extends Structure {
        public short wYear;
        public short wMonth;
        public short wDayOfWeek;
        public short wDay;
        public short wHour;
        public short wMinute;
        public short wSecond;
        public short wMilliseconds;

        public SYSTEMTIME() {
        }
    }

    public static class FILETIME extends Structure {
        public int dwLowDateTime;
        public int dwHighDateTime;
        private static final long EPOCH_DIFF = 11644473600000L;

        public FILETIME(Date date) {
            long rawValue = dateToFileTime(date);
            this.dwHighDateTime = (int)(rawValue >> 32 & 4294967295L);
            this.dwLowDateTime = (int)(rawValue & 4294967295L);
        }

        public FILETIME() {
        }

        public FILETIME(Pointer memory) {
            super(memory);
            this.read();
        }

        public static Date filetimeToDate(int high, int low) {
            long filetime = (long)high << 32 | (long)low & 4294967295L;
            long ms_since_16010101 = filetime / 10000L;
            long ms_since_19700101 = ms_since_16010101 - 11644473600000L;
            return new Date(ms_since_19700101);
        }

        public static long dateToFileTime(Date date) {
            long ms_since_19700101 = date.getTime();
            long ms_since_16010101 = ms_since_19700101 + 11644473600000L;
            return ms_since_16010101 * 1000L * 10L;
        }

        public Date toDate() {
            return filetimeToDate(this.dwHighDateTime, this.dwLowDateTime);
        }

        public long toLong() {
            return this.toDate().getTime();
        }

        public String toString() {
            return super.toString() + ": " + this.toDate().toString();
        }

        public static class ByReference extends WinBase.FILETIME implements com.sun.jna.Structure.ByReference {
            public ByReference() {
            }

            public ByReference(Pointer memory) {
                super(memory);
            }
        }
    }
}
