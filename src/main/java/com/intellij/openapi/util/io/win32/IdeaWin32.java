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
package com.intellij.openapi.util.io.win32;


import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.lang.UrlClassLoader;

/**
 * Do not use this class directly.
 *
 * @author Dmitry Avdeev
 * @since 12.0
 */
public class IdeaWin32 {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.util.io.win32.IdeaWin32");
    private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();

    private static final IdeaWin32 ourInstance;

    static {
        IdeaWin32 instance = null;
        if (SystemInfo.isWin2kOrNewer && SystemProperties.getBooleanProperty("idea.use.native.fs.for.win", true)) {
            try {
                UrlClassLoader.loadPlatformLibrary("IdeaWin32");
                instance = new IdeaWin32();
                LOG.info("Native filesystem for Windows is operational");
            }
            catch (Throwable t) {
                LOG.error("Failed to initialize native filesystem for Windows", t);
            }
        }
        ourInstance = instance;
    }

    public static boolean isAvailable() {
        return ourInstance != null;
    }

    
    public static IdeaWin32 getInstance() {
        if (!isAvailable()) {
            throw new IllegalStateException("Native filesystem for Windows is not loaded");
        }
        return ourInstance;
    }

    private IdeaWin32() {
        initIDs();
    }

    private static native void initIDs();

    
    public FileInfo getInfo(String path) {
        path = path.replace('/', '\\');
        if (DEBUG_ENABLED) {
            LOG.debug("getInfo(" + path + ")");
            long t = System.nanoTime();
            FileInfo result = getInfo0(path);
            t = (System.nanoTime() - t) / 1000;
            LOG.debug("  " + t + " mks");
            return result;
        }
        else {
            return getInfo0(path);
        }
    }

    
    public String resolveSymLink( String path) {
        path = path.replace('/', '\\');
        if (DEBUG_ENABLED) {
            LOG.debug("resolveSymLink(" + path + ")");
            long t = System.nanoTime();
            String result = resolveSymLink0(path);
            t = (System.nanoTime() - t) / 1000;
            LOG.debug("  " + t + " mks");
            return result;
        }
        else {
            return resolveSymLink0(path);
        }
    }

    
    public FileInfo[] listChildren( String path) {
        path = path.replace('/', '\\');
        if (DEBUG_ENABLED) {
            LOG.debug("list(" + path + ")");
            long t = System.nanoTime();
            FileInfo[] children = listChildren0(path);
            t = (System.nanoTime() - t) / 1000;
            LOG.debug("  " + (children == null ? -1 : children.length) + " children, " + t + " mks");
            return children;
        }
        else {
            return listChildren0(path);
        }
    }

    private native FileInfo getInfo0(String path);

    private native String resolveSymLink0(String path);

    private native FileInfo[] listChildren0(String path);
}