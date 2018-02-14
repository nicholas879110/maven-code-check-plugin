//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.java.ayatana;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AyatanaLibrary {
    public static final String LIB_VERSION = "1.2.4";
    public static final String JNI_VERSION = "1.2.0";
    private static boolean loaded = false;
    private static boolean successful = false;

    public AyatanaLibrary() {
    }

    private static String getUbuntuVersion() throws IOException {
        Properties prop = new Properties();
        File frel = new File("/etc/lsb-release");
        if (frel.exists()) {
            FileInputStream fis = new FileInputStream(frel);

            try {
                prop.load(fis);
            } finally {
                fis.close();
            }
        }

        return prop.getProperty("DISTRIB_RELEASE", "UNKNOW");
    }

    public static boolean load() {
        if (!loaded) {
            try {
                File targetLibrary = new File("/usr/lib/jayatana/libjayatana.so.1.2.0");
                if (!targetLibrary.exists()) {
                    File targetDirectory = new File(System.getProperty("user.home"), ".java/jayatana/1.2.0/" + System.getProperty("os.arch"));
                    targetLibrary = new File(targetDirectory, "libjayatana.so");
                    String sourceLibrary = "/native/" + getUbuntuVersion() + "/" + System.getProperty("os.arch") + "/libjayatana.so";
                    if (AyatanaLibrary.class.getResource(sourceLibrary) == null) {
                        sourceLibrary = "/native/UNKNOW/" + System.getProperty("os.arch") + "/libjayatana.so";
                    }

                    if (targetLibrary.exists()) {
                        FileInputStream fis = new FileInputStream(targetLibrary);
                        String chksum = AyatanaDesktop.getMD5Checksum(fis);
                        fis.close();
                        InputStream input = AyatanaLibrary.class.getResourceAsStream(sourceLibrary);
                        if (input == null) {
                            throw new Exception("not library exists");
                        }

                        String chksumint = AyatanaDesktop.getMD5Checksum(input);
                        input.close();
                        if (!chksumint.equals(chksum)) {
                            targetLibrary.delete();
                            input = AyatanaLibrary.class.getResourceAsStream(sourceLibrary);
                            if (input == null) {
                                throw new Exception("not library exists");
                            }

                            FileOutputStream fos = new FileOutputStream(targetLibrary);
                            byte[] buff = new byte[1024];

                            while(true) {
                                int read;
                                if ((read = input.read(buff)) <= 0) {
                                    fos.flush();
                                    fos.close();
                                    input.close();
                                    break;
                                }

                                fos.write(buff, 0, read);
                            }
                        }
                    } else {
                        targetDirectory.mkdirs();
                        InputStream input = AyatanaLibrary.class.getResourceAsStream(sourceLibrary);
                        if (input == null) {
                            throw new Exception("not library exists");
                        }

                        FileOutputStream fos = new FileOutputStream(targetLibrary);
                        byte[] buff = new byte[1024];

                        int read;
                        while((read = input.read(buff)) > 0) {
                            fos.write(buff, 0, read);
                        }

                        fos.flush();
                        fos.close();
                        input.close();
                    }
                }

                try {
                    System.loadLibrary("awt");
                } catch (UnsatisfiedLinkError var10) {
                    if (!var10.getMessage().contains("loaded in another classloader")) {
                        throw var10;
                    }
                }

                try {
                    System.loadLibrary("jawt");
                } catch (UnsatisfiedLinkError var11) {
                    if (!var11.getMessage().contains("loaded in another classloader")) {
                        throw var11;
                    }
                }

                System.load(targetLibrary.getCanonicalPath());
                successful = true;
            } catch (Exception var12) {
                successful = false;
            }

            loaded = true;
        }

        return successful;
    }
}
