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
package com.gome.maven.ide.plugins;

import com.gome.maven.diagnostic.PluginException;
import com.gome.maven.ide.ClassUtilCore;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.idea.IdeaApplication;
import com.gome.maven.idea.Main;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.components.ComponentConfig;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.openapi.extensions.impl.PicoPluginExtensionInitializationException;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.openapi.wm.ex.WindowManagerEx;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author mike
 */
public class PluginManager extends PluginManagerCore {
     public static final String INSTALLED_TXT = "installed.txt";

    public static long startupStart;

    /**
     * Called via reflection
     */
    @SuppressWarnings({"UnusedDeclaration", "HardCodedStringLiteral"})
    protected static void start(final String mainClass, final String methodName, final String[] args) {
        startupStart = System.nanoTime();

        Main.setFlags(args);

        if (!Main.isHeadless()) {
            UIUtil.initDefaultLAF();
        }

        ThreadGroup threadGroup = new ThreadGroup("Idea Thread Group") {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                processException(e);
            }
        };

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ClassUtilCore.clearJarURLCache();

                    Class<?> aClass = Class.forName(mainClass);
                    Method method = aClass.getDeclaredMethod(methodName, ArrayUtil.EMPTY_STRING_ARRAY.getClass());
                    method.setAccessible(true);
                    Object[] argsArray = {args};
                    method.invoke(null, argsArray);
                }
                catch (Throwable t) {
                    throw new StartupAbortedException(t);
                }
            }
        };

        new Thread(threadGroup, runnable, "Idea Main Thread").start();
    }

    public static void processException(Throwable t) {
        if (!IdeaApplication.isLoaded()) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") StartupAbortedException se = findCause(t, StartupAbortedException.class);
            if (se == null) se = new StartupAbortedException(t);
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") PluginException pe = findCause(t, PluginException.class);
            PluginId pluginId = pe != null ? pe.getPluginId() : null;

            if (Logger.isInitialized() && !(t instanceof ProcessCanceledException)) {
                try {
                    getLogger().error(t);
                }
                catch (Throwable ignore) { }
            }

            if (pluginId != null && !CORE_PLUGIN_ID.equals(pluginId.getIdString())) {
                disablePlugin(pluginId.getIdString());

                StringWriter message = new StringWriter();
                message.append("Plugin '").append(pluginId.getIdString()).append("' failed to initialize and will be disabled. ");
                message.append(" Please restart ").append(ApplicationNamesInfo.getInstance().getFullProductName()).append('.');
                message.append("\n\n");
                pe.getCause().printStackTrace(new PrintWriter(message));

                Main.showMessage("Plugin Error", message.toString(), false);
                System.exit(Main.PLUGIN_ERROR);
            }
            else {
                Main.showMessage("Start Failed", t);
                System.exit(se.exitCode());
            }
        }
        else if (!(t instanceof ProcessCanceledException)) {
            getLogger().error(t);
        }
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> clazz) {
        while (t != null) {
            if (clazz.isInstance(t)) {
                return clazz.cast(t);
            }
            t = t.getCause();
        }
        return null;
    }

    private static Thread.UncaughtExceptionHandler HANDLER = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            processException(e);
        }
    };

    public static void installExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(HANDLER);
    }

    public static void reportPluginError() {
        if (myPluginError != null) {
            String title = IdeBundle.message("title.plugin.error");
            Notifications.Bus.notify(new Notification(title, title, myPluginError, NotificationType.ERROR, new NotificationListener() {
                @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
                @Override
                public void hyperlinkUpdate( Notification notification,  HyperlinkEvent event) {
                    notification.expire();

                    String description = event.getDescription();
                    if (EDIT.equals(description)) {
                        PluginManagerConfigurable configurable = new PluginManagerConfigurable(PluginManagerUISettings.getInstance());
                        IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
                        ShowSettingsUtil.getInstance().editConfigurable((JFrame)ideFrame, configurable);
                        return;
                    }

                    List<String> disabledPlugins = getDisabledPlugins();
                    if (myPlugins2Disable != null && DISABLE.equals(description)) {
                        for (String pluginId : myPlugins2Disable) {
                            if (!disabledPlugins.contains(pluginId)) {
                                disabledPlugins.add(pluginId);
                            }
                        }
                    }
                    else if (myPlugins2Enable != null && ENABLE.equals(description)) {
                        disabledPlugins.removeAll(myPlugins2Enable);
                        PluginManagerMain.notifyPluginsUpdated(null);
                    }

                    try {
                        saveDisabledPlugins(disabledPlugins, false);
                    }
                    catch (IOException ignore) { }

                    myPlugins2Enable = null;
                    myPlugins2Disable = null;
                }
            }));
            myPluginError = null;
        }
    }

    public static boolean isPluginInstalled(PluginId id) {
        return getPlugin(id) != null;
    }

    
    public static IdeaPluginDescriptor getPlugin( PluginId id) {
        final IdeaPluginDescriptor[] plugins = getPlugins();
        for (final IdeaPluginDescriptor plugin : plugins) {
            if (Comparing.equal(id, plugin.getPluginId())) {
                return plugin;
            }
        }
        return null;
    }

    public static void handleComponentError(Throwable t,  String componentClassName,  ComponentConfig config) {
        Application app = ApplicationManager.getApplication();
        if (app != null && app.isUnitTestMode()) {
            if (t instanceof Error) throw (Error)t;
            if (t instanceof RuntimeException) throw (RuntimeException)t;
            throw new RuntimeException(t);
        }

        if (t instanceof StartupAbortedException) {
            throw (StartupAbortedException)t;
        }

        PluginId pluginId = null;
        if (config != null) {
            pluginId = config.getPluginId();
        }
        if (pluginId == null || CORE_PLUGIN_ID.equals(pluginId.getIdString())) {
            if (componentClassName != null) {
                pluginId = getPluginByClassName(componentClassName);
            }
        }
        if (pluginId == null || CORE_PLUGIN_ID.equals(pluginId.getIdString())) {
            if (t instanceof PicoPluginExtensionInitializationException) {
                pluginId = ((PicoPluginExtensionInitializationException)t).getPluginId();
            }
        }

        if (pluginId != null && !CORE_PLUGIN_ID.equals(pluginId.getIdString())) {
            throw new StartupAbortedException(new PluginException(t, pluginId));
        }
        else {
            throw new StartupAbortedException("Fatal error initializing '" + componentClassName + "'", t);
        }
    }

    private static class StartupAbortedException extends RuntimeException {
        private int exitCode = Main.STARTUP_EXCEPTION;

        public StartupAbortedException(Throwable cause) {
            super(cause);
        }

        public StartupAbortedException(String message, Throwable cause) {
            super(message, cause);
        }

        public int exitCode() {
            return exitCode;
        }

        public StartupAbortedException exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }
    }
}
