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
package com.gome.maven.util.lang;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.io.ZipFileCache;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.io.URLUtil;
import com.gome.maven.openapi.util.io.FileUtil;
import sun.misc.Resource;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class JarLoader extends Loader {
    private final URL myURL;
    private final boolean myCanLockJar;
    private SoftReference<JarMemoryLoader> myMemoryLoader;

    JarLoader(URL url, boolean canLockJar, int index, boolean preloadJarContents) throws IOException {
        super(new URL(URLUtil.JAR_PROTOCOL, "", -1, url + "!/"), index);
        myURL = url;
        myCanLockJar = canLockJar;

        ZipFile zipFile = acquireZipFile();
        try {
            if (preloadJarContents) {
                JarMemoryLoader loader = JarMemoryLoader.load(zipFile, getBaseURL());
                if (loader != null) {
                    myMemoryLoader = new SoftReference<JarMemoryLoader>(loader);
                }
            }
        }
        finally {
            releaseZipFile(zipFile);
        }
    }

    private ZipFile acquireZipFile() throws IOException {
        String path = FileUtil.unquote(myURL.getFile());
        //noinspection IOResourceOpenedButNotSafelyClosed
        return myCanLockJar ? ZipFileCache.acquire(path) : new ZipFile(path);
    }

    private void releaseZipFile(ZipFile zipFile) throws IOException {
        if (myCanLockJar) {
            ZipFileCache.release(zipFile);
        }
        else {
            zipFile.close();
        }
    }
    
    @Override
    public ClasspathCache.LoaderData buildData() throws IOException {
        ZipFile zipFile = acquireZipFile();
        try {
            ClasspathCache.LoaderData loaderData = new ClasspathCache.LoaderData();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                loaderData.addResourceEntry(name);
                loaderData.addNameEntry(name);
            }
            return loaderData;
        }
        finally {
            releaseZipFile(zipFile);
        }
    }

    @Override
    
    Resource getResource(String name, boolean flag) {
        JarMemoryLoader loader =SoftReference.dereference(myMemoryLoader);
        if (loader != null) {
            Resource resource = loader.getResource(name);
            if (resource != null) return resource;
        }

        try {
            ZipFile zipFile = acquireZipFile();
            try {
                ZipEntry entry = zipFile.getEntry(name);
                if (entry != null) {
                    return MemoryResource.load(getBaseURL(), zipFile, entry);
                }
            }
            finally {
                releaseZipFile(zipFile);
            }
        }
        catch (Exception e) {
            Logger.getInstance(JarLoader.class).error("url: " + myURL, e);
        }

        return null;
    }

    @Override
    public String toString() {
        return "JarLoader [" + myURL + "]";
    }
}
