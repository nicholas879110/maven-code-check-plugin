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
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.ImageIcon;

public final class AyatanaDesktop {
    public AyatanaDesktop() {
    }

    private static String toHexadecimal(byte[] digest) {
        String hash = "";
        byte[] arr$ = digest;
        int len$ = digest.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            byte aux = arr$[i$];
            int b = aux & 255;
            if (Integer.toHexString(b).length() == 1) {
                hash = hash + "0";
            }

            hash = hash + Integer.toHexString(b);
        }

        return hash;
    }

    public static String getMD5Checksum(InputStream input) {
        try {
            byte[] buff = new byte[1024];
            MessageDigest md = MessageDigest.getInstance("MD5");

            int read;
            while((read = input.read(buff)) > 0) {
                md.update(buff, 0, read);
            }

            return toHexadecimal(md.digest());
        } catch (NoSuchAlgorithmException var4) {
            throw new RuntimeException(var4);
        } catch (IOException var5) {
            throw new RuntimeException(var5);
        }
    }

    public static boolean isSupported() {
        if (!"true".equals(System.getProperty("jayatana.force")) && !"true".equals(System.getenv("JAYATANA_FORCE"))) {
            if (!System.getProperty("os.name").contains("Linux")) {
                return false;
            }

            if (!"Unity".equals(System.getenv("XDG_CURRENT_DESKTOP"))) {
                return false;
            }

            String version = System.getProperty("java.version");
            version = version.substring(0, version.indexOf(".", version.indexOf(".") + 1));

            try {
                float iversion = Float.parseFloat(version);
                if (System.getProperty("java.vm.name").contains("OpenJDK") && iversion < 1.7F) {
                    return false;
                }

                if (iversion < 1.6F) {
                    return false;
                }
            } catch (NumberFormatException var2) {
                return false;
            }
        }

        return true;
    }

    public static boolean tryInstallIcon(String name, URL urlIcon) {
        return tryInstallIcon(name, "hicolor", urlIcon);
    }

    public static boolean tryInstallIcon(String name, String theme, URL urlIcon) {
        if (!isSupported()) {
            return false;
        } else {
            ImageIcon icon = new ImageIcon(urlIcon);
            if (icon.getIconHeight() != icon.getIconWidth()) {
                throw new RuntimeException("the icon is not 1:1");
            } else {
                switch(icon.getIconWidth()) {
                    case 16:
                    case 24:
                    case 32:
                    case 48:
                    case 128:
                    case 256:
                        String urlIconName = urlIcon.toString();
                        String extensionIconName = urlIconName.substring(urlIconName.lastIndexOf(".") + 1);
                        File iconFile = new File(System.getProperty("user.home"), ".local/share/icons/" + theme + "/" + icon.getIconWidth() + "x" + icon.getIconWidth() + "/apps/" + name + "." + extensionIconName);
                        if (iconFile.exists() && iconFile.isFile()) {
                            String iconTargetMD5;
                            try {
                                FileInputStream fis = new FileInputStream(iconFile);
                                iconTargetMD5 = getMD5Checksum(fis);
                                fis.close();
                            } catch (IOException var15) {
                                throw new RuntimeException(var15);
                            }

                            String iconSourceMD5;
                            InputStream input;
                            try {
                                input = urlIcon.openStream();
                                iconSourceMD5 = getMD5Checksum(input);
                                input.close();
                            } catch (IOException var14) {
                                throw new RuntimeException(var14);
                            }

                            if (!iconSourceMD5.equals(iconTargetMD5)) {
                                iconFile.getParentFile().mkdirs();

                                try {
                                    input = urlIcon.openStream();
                                    FileOutputStream fos = new FileOutputStream(iconFile);
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
                                } catch (IOException var17) {
                                    throw new RuntimeException(var17);
                                }

                                try {
                                    Runtime.getRuntime().exec("xdg-icon-resource forceupdate");
                                } catch (IOException var13) {
                                    ;
                                }

                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            iconFile.getParentFile().mkdirs();

                            try {
                                InputStream input = urlIcon.openStream();
                                FileOutputStream fos = new FileOutputStream(iconFile);
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
                            } catch (IOException var18) {
                                throw new RuntimeException(var18);
                            }

                            try {
                                Runtime.getRuntime().exec("xdg-icon-resource forceupdate");
                            } catch (IOException var16) {
                                ;
                            }

                            return true;
                        }
                    default:
                        throw new RuntimeException("invalid size icon, only support 16x16 24x24 32x32 48x48 128x128 256x256");
                }
            }
        }
    }
}
