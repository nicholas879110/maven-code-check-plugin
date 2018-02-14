//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.win;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class DLLManager {
    public static final String LIB_DIR_OVERRIDE = "proxy_vole_lib_dir";
    static final String TEMP_FILE_PREFIX = "proxy_vole";
    static final String DLL_EXTENSION = ".dll";
    static String LIB_NAME_BASE = "proxy_util_";
    static final String DEFAULT_LIB_FOLDER = "lib";

    DLLManager() {
    }

    static File findLibFile() throws IOException {
        String libName = buildLibName();
        File libFile = getOverrideLibFile(libName);
        if (libFile == null || !libFile.exists()) {
            libFile = getDefaultLibFile(libName);
        }

        if (libFile == null || !libFile.exists()) {
            libFile = extractToTempFile(libName);
        }

        return libFile;
    }

    static void cleanupTempFiles() {
        try {
            String tempFolder = System.getProperty("java.io.tmpdir");
            if (tempFolder == null || tempFolder.trim().length() == 0) {
                return;
            }

            File fldr = new File(tempFolder);
            File[] oldFiles = fldr.listFiles(new DLLManager.TempDLLFileFilter());
            if (oldFiles == null) {
                return;
            }

            File[] arr$ = oldFiles;
            int len$ = oldFiles.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                File tmp = arr$[i$];
                tmp.delete();
            }
        } catch (Exception var7) {
            Logger.log(DLLManager.class, LogLevel.DEBUG, "Error cleaning up temporary dll files. ", new Object[]{var7});
        }

    }

    private static File getDefaultLibFile(String libName) {
        return new File("lib", libName);
    }

    private static File getOverrideLibFile(String libName) {
        String libDir = System.getProperty("proxy_vole_lib_dir");
        return libDir != null && libDir.trim().length() != 0 ? new File(libDir, libName) : null;
    }

    static File extractToTempFile(String libName) throws IOException {
        InputStream source = Win32ProxyUtils.class.getResourceAsStream("/lib/" + libName);
        File tempFile = File.createTempFile("proxy_vole", ".dll");
        tempFile.deleteOnExit();
        FileOutputStream destination = new FileOutputStream(tempFile);
        copy(source, destination);
        return tempFile;
    }

    private static void closeStream(Closeable c) {
        try {
            c.close();
        } catch (IOException var2) {
            ;
        }

    }

    static void copy(InputStream source, OutputStream dest) throws IOException {
        try {
            byte[] buffer = new byte[1024];

            for(int read = 0; read >= 0; read = source.read(buffer)) {
                dest.write(buffer, 0, read);
            }

            dest.flush();
        } finally {
            closeStream(source);
            closeStream(dest);
        }
    }

    private static String buildLibName() {
        String arch = "w32";
        if (!System.getProperty("os.arch").equals("x86")) {
            arch = System.getProperty("os.arch");
        }

        return LIB_NAME_BASE + arch + ".dll";
    }

    private static final class TempDLLFileFilter implements FileFilter {
        private TempDLLFileFilter() {
        }

        public boolean accept(File pathname) {
            String name = pathname.getName();
            return pathname.isFile() && name.startsWith("proxy_vole") && name.endsWith(".dll");
        }
    }
}
