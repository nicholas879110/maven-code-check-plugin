/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.ide.browsers;

import com.gome.maven.CommonBundle;
import com.gome.maven.Patches;
import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.util.ExecUtil;
import com.gome.maven.ide.BrowserUtil;
import com.gome.maven.ide.GeneralSettings;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.PathUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserLauncherAppless extends BrowserLauncher {
    static final Logger LOG = Logger.getInstance(BrowserLauncherAppless.class);

    private static boolean isDesktopActionSupported(Desktop.Action action) {
        return !Patches.SUN_BUG_ID_6457572 && !Patches.SUN_BUG_ID_6486393 &&
                Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action);
    }

    public static boolean canUseSystemDefaultBrowserPolicy() {
        return isDesktopActionSupported(Desktop.Action.BROWSE) ||
                SystemInfo.isMac || SystemInfo.isWindows ||
                (SystemInfo.isUnix && SystemInfo.hasXdgOpen());
    }

    private static GeneralSettings getGeneralSettingsInstance() {
        if (ApplicationManager.getApplication() != null) {
            GeneralSettings settings = GeneralSettings.getInstance();
            if (settings != null) {
                return settings;
            }
        }

        return new GeneralSettings();
    }

    
    private static List<String> getDefaultBrowserCommand() {
        if (SystemInfo.isWindows) {
            return Arrays.asList(ExecUtil.getWindowsShellName(), "/c", "start", GeneralCommandLine.inescapableQuote(""));
        }
        else if (SystemInfo.isMac) {
            return Collections.singletonList(ExecUtil.getOpenCommandPath());
        }
        else if (SystemInfo.isUnix && SystemInfo.hasXdgOpen()) {
            return Collections.singletonList("xdg-open");
        }
        else {
            return null;
        }
    }

    @Override
    public void open( String url) {
        openOrBrowse(url, false, null);
    }

    @Override
    public void browse( File file) {
        browse(VfsUtil.toUri(file));
    }

    @Override
    public void browse( URI uri) {
        browse(uri, null);
    }

    public void browse( URI uri,  Project project) {
        LOG.debug("Launch browser: [" + uri + "]");

        GeneralSettings settings = getGeneralSettingsInstance();
        if (settings.isUseDefaultBrowser()) {
            boolean tryToUseCli = true;
            if (isDesktopActionSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(uri);
                    LOG.debug("Browser launched using JDK 1.6 API");
                    return;
                }
                catch (Exception e) {
                    LOG.warn("Error while using Desktop API, fallback to CLI", e);
                    // if "No application knows how to open", then we must not try to use OS open
                    tryToUseCli = !e.getMessage().contains("Error code: -10814");
                }
            }

            if (tryToUseCli) {
                List<String> command = getDefaultBrowserCommand();
                if (command != null) {
                    doLaunch(uri.toString(), command, null, project, ArrayUtil.EMPTY_STRING_ARRAY, null);
                    return;
                }
            }
        }

        browseUsingNotSystemDefaultBrowserPolicy(uri, settings, project);
    }

    protected void browseUsingNotSystemDefaultBrowserPolicy( URI uri,  GeneralSettings settings,  Project project) {
        browseUsingPath(uri.toString(), settings.getBrowserPath(), null, project, ArrayUtil.EMPTY_STRING_ARRAY);
    }

    private void openOrBrowse( String url, boolean browse,  Project project) {
        url = url.trim();

        URI uri;
        if (BrowserUtil.isAbsoluteURL(url)) {
            uri = VfsUtil.toUri(url);
        }
        else {
            File file = new File(url);
            if (!browse && isDesktopActionSupported(Desktop.Action.OPEN)) {
                if (!file.exists()) {
                    showError(IdeBundle.message("error.file.does.not.exist", file.getPath()), null, null, null, null);
                    return;
                }

                try {
                    Desktop.getDesktop().open(file);
                    return;
                }
                catch (IOException e) {
                    LOG.debug(e);
                }
            }

            browse(file);
            return;
        }

        if (uri == null) {
            showError(IdeBundle.message("error.malformed.url", url), null, project, null, null);
        }
        else {
            browse(uri, project);
        }
    }

    @Override
    public void browse( String url,  WebBrowser browser) {
        browse(url, browser, null);
    }

    @Override
    public void browse( String url,  WebBrowser browser,  Project project) {
        if (browser == null) {
            openOrBrowse(url, true, project);
        }
        else {
//            for (UrlOpener urlOpener : UrlOpener.EP_NAME.getExtensions()) {
//                if (urlOpener.openUrl(browser, url, project)) {
//                    return;
//                }
//            }
        }
    }

    @Override
    public boolean browseUsingPath( final String url,
                                    String browserPath,
                                    final WebBrowser browser,
                                    final Project project,
                                    final String[] additionalParameters) {
        Runnable launchTask = null;
        if (browserPath == null && browser != null) {
            browserPath = PathUtil.toSystemDependentName(browser.getPath());
            launchTask = new Runnable() {
                @Override
                public void run() {
                    browseUsingPath(url, null, browser, project, additionalParameters);
                }
            };
        }
        return doLaunch(url, browserPath, browser, project, additionalParameters, launchTask);
    }

    private boolean doLaunch( String url,
                              String browserPath,
                              WebBrowser browser,
                              Project project,
                              String[] additionalParameters,
                              Runnable launchTask) {
        if (!checkPath(browserPath, browser, project, launchTask)) {
            return false;
        }
        return doLaunch(url, BrowserUtil.getOpenBrowserCommand(browserPath, false), browser, project, additionalParameters, launchTask);
    }

    public boolean checkPath( String browserPath,  WebBrowser browser,  Project project,  Runnable launchTask) {
        if (!StringUtil.isEmptyOrSpaces(browserPath)) {
            return true;
        }

        String message = browser != null ? browser.getBrowserNotFoundMessage() :
                IdeBundle.message("error.please.specify.path.to.web.browser", CommonBundle.settingsActionPath());
        showError(message, browser, project, IdeBundle.message("title.browser.not.found"), launchTask);
        return false;
    }

    private boolean doLaunch( String url,
                              List<String> command,
                              WebBrowser browser,
                              Project project,
                              String[] additionalParameters,
                              Runnable launchTask) {
        GeneralCommandLine commandLine = new GeneralCommandLine(command);

        if (url != null && url.startsWith("jar:")) {
            return false;
        }

        if (url != null) {
            commandLine.addParameter(url);
        }

        final BrowserSpecificSettings browserSpecificSettings = browser == null ? null : browser.getSpecificSettings();
        if (browserSpecificSettings != null) {
            commandLine.getEnvironment().putAll(browserSpecificSettings.getEnvironmentVariables());
        }

        addArgs(commandLine, browserSpecificSettings, additionalParameters);

        try {
            Process process = commandLine.createProcess();
            checkCreatedProcess(browser, project, commandLine, process, launchTask);
            return true;
        }
        catch (ExecutionException e) {
            showError(e.getMessage(), browser, project, null, null);
            return false;
        }
    }

    protected void checkCreatedProcess( WebBrowser browser,
                                        Project project,
                                        GeneralCommandLine commandLine,
                                        Process process,
                                        Runnable launchTask) {
    }

    protected void showError( String error,  WebBrowser browser,  Project project, String title,  Runnable launchTask) {
        // Not started yet. Not able to show message up. (Could happen in License panel under Linux).
        LOG.warn(error);
    }

    private static void addArgs( GeneralCommandLine command,  BrowserSpecificSettings settings,  String[] additional) {
        List<String> specific = settings == null ? Collections.<String>emptyList() : settings.getAdditionalParameters();
        if (specific.size() + additional.length > 0) {
            if (isOpenCommandUsed(command)) {
                if (BrowserUtil.isOpenCommandSupportArgs()) {
                    command.addParameter("--args");
                }
                else {
                    LOG.warn("'open' command doesn't allow to pass command line arguments so they will be ignored: " +
                            StringUtil.join(specific, ", ") + " " + Arrays.toString(additional));
                    return;
                }
            }

            command.addParameters(specific);
            command.addParameters(additional);
        }
    }

    public static boolean isOpenCommandUsed( GeneralCommandLine command) {
        return SystemInfo.isMac && ExecUtil.getOpenCommandPath().equals(command.getExePath());
    }
}