//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sun.jna.platform.win32;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.win32.StdCallLibrary;

public interface Tlhelp32 extends StdCallLibrary {
    DWORD TH32CS_SNAPHEAPLIST = new DWORD(1L);
    DWORD TH32CS_SNAPPROCESS = new DWORD(2L);
    DWORD TH32CS_SNAPTHREAD = new DWORD(4L);
    DWORD TH32CS_SNAPMODULE = new DWORD(8L);
    DWORD TH32CS_SNAPMODULE32 = new DWORD(16L);
    DWORD TH32CS_SNAPALL = new DWORD((long)(TH32CS_SNAPHEAPLIST.intValue() | TH32CS_SNAPPROCESS.intValue() | TH32CS_SNAPTHREAD.intValue() | TH32CS_SNAPMODULE.intValue()));
    DWORD TH32CS_INHERIT = new DWORD(-2147483648L);

    public static class PROCESSENTRY32 extends Structure {
        public DWORD dwSize;
        public DWORD cntUsage;
        public DWORD th32ProcessID;
        public ULONG_PTR th32DefaultHeapID;
        public DWORD th32ModuleID;
        public DWORD cntThreads;
        public DWORD th32ParentProcessID;
        public LONG pcPriClassBase;
        public DWORD dwFlags;
        public char[] szExeFile = new char[260];

        public PROCESSENTRY32() {
            this.dwSize = new DWORD((long)this.size());
        }

        public PROCESSENTRY32(Pointer memory) {
            super(memory);
            this.read();
        }

        public static class ByReference extends Tlhelp32.PROCESSENTRY32 implements com.sun.jna.Structure.ByReference {
            public ByReference() {
            }

            public ByReference(Pointer memory) {
                super(memory);
            }
        }
    }
}
