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
package com.gome.maven.openapi.vfs.impl.jar;

import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.JarFile;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.VfsImplUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.containers.ContainerUtil;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class JarFileSystemImpl extends JarFileSystem {
    private final Set<String> myNoCopyJarPaths;
    private final File myNoCopyJarDir;

    public JarFileSystemImpl() {
        boolean noCopy = SystemProperties.getBooleanProperty("idea.jars.nocopy", !SystemInfo.isWindows);
        myNoCopyJarPaths = noCopy ? null : ContainerUtil.newConcurrentSet(FileUtil.PATH_HASHING_STRATEGY);

        // to prevent platform .jar files from copying
        boolean runningFromDist = new File(PathManager.getLibPath(), "openapi.jar").exists();
        myNoCopyJarDir = !runningFromDist ? null : new File(PathManager.getHomePath());
    }

    @Override
    public void setNoCopyJarForPath(String pathInJar) {
        if (myNoCopyJarPaths == null || pathInJar == null) return;
        int index = pathInJar.indexOf(JAR_SEPARATOR);
        if (index < 0) return;
        String path = FileUtil.toSystemIndependentName(pathInJar.substring(0, index));
        myNoCopyJarPaths.add(path);
    }

    
    public File getMirroredFile( VirtualFile vFile) {
        VirtualFile root = getRootByLocal(vFile);
        return root == null ? null : getHandler(root).getFileToUse();
    }

    public boolean isMakeCopyOfJar( File originalJar) {
        if (myNoCopyJarPaths == null || myNoCopyJarPaths.contains(originalJar.getPath())) return false;
        if (myNoCopyJarDir != null && FileUtil.isAncestor(myNoCopyJarDir, originalJar, false)) return false;
        return true;
    }

    @Override
    
    public String getProtocol() {
        return PROTOCOL;
    }

    
    @Override
    public String extractPresentableUrl( String path) {
        return super.extractPresentableUrl(StringUtil.trimEnd(path, JAR_SEPARATOR));
    }

    @Override
    protected String normalize( String path) {
        final int jarSeparatorIndex = path.indexOf(JAR_SEPARATOR);
        if (jarSeparatorIndex > 0) {
            final String root = path.substring(0, jarSeparatorIndex);
            return FileUtil.normalize(root) + path.substring(jarSeparatorIndex);
        }
        return super.normalize(path);
    }

    
    @Override
    protected String extractRootPath( String path) {
        final int jarSeparatorIndex = path.indexOf(JAR_SEPARATOR);
        assert jarSeparatorIndex >= 0 : "Path passed to JarFileSystem must have jar separator '!/': " + path;
        return path.substring(0, jarSeparatorIndex + JAR_SEPARATOR.length());
    }

    
    @Override
    protected String extractLocalPath( String rootPath) {
        return StringUtil.trimEnd(rootPath, JAR_SEPARATOR);
    }

    
    @Override
    protected String composeRootPath( String localPath) {
        return localPath + JAR_SEPARATOR;
    }

    
    @Override
    protected JarHandler getHandler( VirtualFile entryFile) {
        return VfsImplUtil.getHandler(this, entryFile, new Function<String, JarHandler>() {
            @Override
            public JarHandler fun(String localPath) {
                return new JarHandler(localPath);
            }
        });
    }

    @Override
    public VirtualFile findFileByPath( String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    @Override
    public VirtualFile findFileByPathIfCached( String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Override
    public VirtualFile refreshAndFindFileByPath( String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    @Override
    public void refresh(boolean asynchronous) {
        VfsImplUtil.refresh(this, asynchronous);
    }

    /** @deprecated to be removed in IDEA 15 */
    @SuppressWarnings("deprecation")
    @Override
    public JarFile getJarFile( VirtualFile entryVFile) throws IOException {
        return getHandler(entryVFile).getJar();
    }
}
