/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.ide;

import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.util.ExecUtil;
import com.gome.maven.ide.browsers.BrowserLauncher;
import com.gome.maven.ide.browsers.BrowserLauncherAppless;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gome.maven.util.containers.ContainerUtilRt.newArrayList;

public class BrowserUtil {
    // The pattern for 'scheme' mainly according to RFC1738.
    // We have to violate the RFC since we need to distinguish
    // real schemes from local Windows paths; The only difference
    // with RFC is that we do not allow schemes with length=1 (in other case
    // local paths like "C:/temp/index.html" would be erroneously interpreted as
    // external URLs.)
    private static final Pattern ourExternalPrefix = Pattern.compile("^[\\w\\+\\.\\-]{2,}:");
    private static final Pattern ourAnchorSuffix = Pattern.compile("#(.*)$");

    private BrowserUtil() { }

    public static boolean isAbsoluteURL(String url) {
        return ourExternalPrefix.matcher(url.toLowerCase(Locale.ENGLISH)).find();
    }

    public static String getDocURL(String url) {
        Matcher anchorMatcher = ourAnchorSuffix.matcher(url);
        return anchorMatcher.find() ? anchorMatcher.reset().replaceAll("") : url;
    }

    
    public static URL getURL(String url) throws MalformedURLException {
        return isAbsoluteURL(url) ? VfsUtilCore.convertToURL(url) : new URL("file", "", url);
    }

    public static void browse( VirtualFile file) {
        browse(VfsUtil.toUri(file));
    }

    public static void browse( File file) {
        getBrowserLauncher().browse(file);
    }

    public static void browse( URL url) {
        browse(url.toExternalForm());
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * @deprecated Use {@link #browse(String)}
     */
    public static void launchBrowser(  String url) {
        browse(url);
    }

    public static void browse(  String url) {
        getBrowserLauncher().browse(url, null);
    }

    private static BrowserLauncher getBrowserLauncher() {
        BrowserLauncher launcher = ApplicationManager.getApplication() == null ? null : BrowserLauncher.getInstance();
        return launcher == null ? new BrowserLauncherAppless() : launcher;
    }

    public static void open(  String url) {
        getBrowserLauncher().open(url);
    }

    /**
     * Main method: tries to launch a browser using every possible way
     */
    public static void browse( URI uri) {
        getBrowserLauncher().browse(uri);
    }

    public static void browse( String url,  Project project) {
        getBrowserLauncher().browse(url, null, project);
    }

    @SuppressWarnings("UnusedDeclaration")
    
    @Deprecated
    public static List<String> getOpenBrowserCommand(  String browserPathOrName) {
        return getOpenBrowserCommand(browserPathOrName, false);
    }

    
    public static List<String> getOpenBrowserCommand(  String browserPathOrName, boolean newWindowIfPossible) {
        if (new File(browserPathOrName).isFile()) {
            return Collections.singletonList(browserPathOrName);
        }
        else if (SystemInfo.isMac) {
            List<String> command = newArrayList(ExecUtil.getOpenCommandPath(), "-a", browserPathOrName);
            if (newWindowIfPossible) {
                command.add("-n");
            }
            return command;
        }
        else if (SystemInfo.isWindows) {
            return Arrays.asList(ExecUtil.getWindowsShellName(), "/c", "start", GeneralCommandLine.inescapableQuote(""), browserPathOrName);
        }
        else {
            return Collections.singletonList(browserPathOrName);
        }
    }

    public static boolean isOpenCommandSupportArgs() {
        return SystemInfo.isMacOSSnowLeopard;
    }

    
    public static String getDefaultAlternativeBrowserPath() {
        if (SystemInfo.isWindows) {
            return "C:\\Program Files\\Internet Explorer\\IExplore.exe";
        }
        else if (SystemInfo.isMac) {
            return "open";
        }
        else if (SystemInfo.isUnix) {
            return "/usr/bin/firefox";
        }
        else {
            return "";
        }
    }
}
