//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.search.desktop.kde;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class KdeSettingsParser {
    private File settingsFile;

    public KdeSettingsParser() {
        this((File)null);
    }

    public KdeSettingsParser(File settingsFile) {
        this.settingsFile = settingsFile;
    }

    public Properties parseSettings() throws IOException {
        if (this.settingsFile == null) {
            this.settingsFile = this.findSettingsFile();
        }

        if (this.settingsFile == null) {
            return null;
        } else {
            BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile)));
            Properties result = new Properties();

            Properties var4;
            try {
                String line;
                for(line = fin.readLine(); line != null && !"[Proxy Settings]".equals(line.trim()); line = fin.readLine()) {
                    ;
                }

                if (line != null) {
                    for(line = ""; line != null && !line.trim().startsWith("["); line = fin.readLine()) {
                        line = line.trim();
                        int index = line.indexOf(61);
                        if (index > 0) {
                            String key = line.substring(0, index).trim();
                            String value = line.substring(index + 1).trim();
                            result.setProperty(key, value);
                        }
                    }

                    return result;
                }

                var4 = result;
            } finally {
                fin.close();
            }

            return var4;
        }
    }

    private File findSettingsFile() {
        File userDir = new File(System.getProperty("user.home"));
        if ("4".equals(System.getenv("KDE_SESSION_VERSION"))) {
            this.settingsFile = this.findSettingsFile(new File(userDir, ".kde4" + File.separator + "share" + File.separator + "config" + File.separator + "kioslaverc"));
        }

        return this.settingsFile == null ? this.findSettingsFile(new File(userDir, ".kde" + File.separator + "share" + File.separator + "config" + File.separator + "kioslaverc")) : this.settingsFile;
    }

    private File findSettingsFile(File settingsFile) {
        Logger.log(this.getClass(), LogLevel.TRACE, "Searching Kde settings in {0}", new Object[]{settingsFile});
        if (!settingsFile.exists()) {
            Logger.log(this.getClass(), LogLevel.DEBUG, "Settings not found", new Object[0]);
            return null;
        } else {
            Logger.log(this.getClass(), LogLevel.TRACE, "Settings found", new Object[0]);
            return settingsFile;
        }
    }
}
