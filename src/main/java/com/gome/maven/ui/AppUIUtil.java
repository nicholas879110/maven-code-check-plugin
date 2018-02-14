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
package com.gome.maven.ui;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.ex.ApplicationInfoEx;
import com.gome.maven.openapi.application.impl.ApplicationInfoImpl;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.Balloon;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.util.PlatformUtils;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * @author yole
 */
public class AppUIUtil {
    private static final String VENDOR_PREFIX = "jetbrains-";

    public static void updateWindowIcon( Window window) {
        window.setIconImages(getAppIconImages());
    }

    @SuppressWarnings({"UnnecessaryFullyQualifiedName", "deprecation"})
    private static List<Image> getAppIconImages() {
        ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();
        List<Image> images = ContainerUtil.newArrayListWithCapacity(3);

        if (SystemInfo.isXWindow) {
            String bigIconUrl = appInfo.getBigIconUrl();
            if (bigIconUrl != null) {
                images.add(com.gome.maven.util.ImageLoader.loadFromResource(bigIconUrl));
            }
        }

        images.add(com.gome.maven.util.ImageLoader.loadFromResource(appInfo.getIconUrl()));
        images.add(com.gome.maven.util.ImageLoader.loadFromResource(appInfo.getSmallIconUrl()));

        return images;
    }

    public static void invokeLaterIfProjectAlive( final Project project,  final Runnable runnable) {
        final Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            runnable.run();
        }
        else {
            application.invokeLater(runnable, new Condition() {
                @Override
                public boolean value(Object o) {
                    return !project.isOpen() || project.isDisposed();
                }
            });
        }
    }

    public static void invokeOnEdt(Runnable runnable) {
        invokeOnEdt(runnable, null);
    }

    public static void invokeOnEdt(Runnable runnable,  Condition expired) {
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            //noinspection unchecked
            if (expired == null || !expired.value(null)) {
                runnable.run();
            }
        }
        else if (expired == null) {
            application.invokeLater(runnable);
        }
        else {
            application.invokeLater(runnable, expired);
        }
    }

    public static void updateFrameClass() {
        try {
            final Toolkit toolkit = Toolkit.getDefaultToolkit();
            final Class<? extends Toolkit> aClass = toolkit.getClass();
            if ("sun.awt.X11.XToolkit".equals(aClass.getName())) {
                ReflectionUtil.setField(aClass, toolkit, null, "awtAppClassName", getFrameClass());
            }
        }
        catch (Exception ignore) { }
    }

    public static String getFrameClass() {
        String name = ApplicationNamesInfo.getInstance().getProductName().toLowerCase(Locale.US);
        String wmClass = VENDOR_PREFIX + StringUtil.replaceChar(name, ' ', '-');
        if ("true".equals(System.getProperty("idea.debug.mode"))) {
            wmClass += "-debug";
        }
        return PlatformUtils.isCommunityEdition() ? wmClass + "-ce" : wmClass;
    }

    public static void registerBundledFonts() {
        if (Registry.is("ide.register.bundled.fonts")) {
            registerFont("/fonts/Inconsolata.ttf");
            registerFont("/fonts/SourceCodePro-Regular.ttf");
            registerFont("/fonts/SourceCodePro-Bold.ttf");
        }
    }

    private static void registerFont( String name) {
        URL url = AppUIUtil.class.getResource(name);
        if (url == null) {
            Logger.getInstance(AppUIUtil.class).warn("Resource missing: " + name);
            return;
        }

        try {
            InputStream is = url.openStream();
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            }
            finally {
                is.close();
            }
        }
        catch (Throwable t) {
            Logger.getInstance(AppUIUtil.class).warn("Cannot register font: " + url, t);
        }
    }

    public static void hideToolWindowBalloon( final String id,  final Project project) {
        invokeLaterIfProjectAlive(project, new Runnable() {
            @Override
            public void run() {
                Balloon balloon = ToolWindowManager.getInstance(project).getToolWindowBalloon(id);
                if (balloon != null) {
                    balloon.hide();
                }
            }
        });
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 14
     */
    public static JTextField createUndoableTextField() {
        return GuiUtils.createUndoableTextField();
    }

    private static final int MIN_ICON_SIZE = 32;

    
    public static String findIcon(final String iconsPath) {
        final File iconsDir = new File(iconsPath);

        // 1. look for .svg icon
        for (String child : iconsDir.list()) {
            if (child.endsWith(".svg")) {
                return iconsPath + '/' + child;
            }
        }

        // 2. look for .png icon of max size
        int max = 0;
        String iconPath = null;
        for (String child : iconsDir.list()) {
            if (!child.endsWith(".png")) continue;
            final String path = iconsPath + '/' + child;
            final Icon icon = new ImageIcon(path);
            final int size = icon.getIconHeight();
            if (size >= MIN_ICON_SIZE && size > max && size == icon.getIconWidth()) {
                max = size;
                iconPath = path;
            }
        }

        return iconPath;
    }
}
