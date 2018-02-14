/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.util;

import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileProvider;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.io.URLUtil;

import java.io.File;

public class PathUtil {
    private PathUtil() {
    }

    
    public static String getLocalPath( VirtualFile file) {
        if (file == null || !file.isValid()) {
            return null;
        }
        if (file.getFileSystem().getProtocol().equals(URLUtil.JAR_PROTOCOL) && file.getParent() != null) {
            return null;
        }
        return getLocalPath(file.getPath());
    }

    
    public static String getLocalPath( String path) {
        return FileUtil.toSystemDependentName(StringUtil.trimEnd(path, URLUtil.JAR_SEPARATOR));
    }

    
    public static VirtualFile getLocalFile( VirtualFile file) {
        if (!file.isValid()) {
            return file;
        }
        if (file.getFileSystem() instanceof LocalFileProvider) {
            final VirtualFile localFile = ((LocalFileProvider)file.getFileSystem()).getLocalVirtualFileFor(file);
            if (localFile != null) {
                return localFile;
            }
        }
        return file;
    }

    
    public static String getJarPathForClass( Class aClass) {
        final String pathForClass = PathManager.getJarPathForClass(aClass);
        assert pathForClass != null : aClass;
        return pathForClass;
    }

    
    public static String toPresentableUrl( String url) {
        return getLocalPath(VirtualFileManager.extractPath(url));
    }

    public static String getCanonicalPath( String path) {
        return FileUtil.toCanonicalPath(path);
    }

    
    public static String getFileName( String path) {
        return PathUtilRt.getFileName(path);
    }

    
    public static String getFileExtension( String name) {
        int index = name.lastIndexOf('.');
        if (index < 0) return null;
        return name.substring(index + 1);
    }

    
    public static String getParentPath( String path) {
        return PathUtilRt.getParentPath(path);
    }

    
    public static String suggestFileName( String text) {
        return PathUtilRt.suggestFileName(text);
    }

    
    public static String suggestFileName( String text, final boolean allowDots, final boolean allowSpaces) {
        return PathUtilRt.suggestFileName(text, allowDots, allowSpaces);
    }

    public static boolean isValidFileName( String fileName) {
        return PathUtilRt.isValidFileName(fileName);
    }

    public static String toSystemIndependentName( String path) {
        return path == null ? null : FileUtilRt.toSystemIndependentName(path);
    }


    public static String toSystemDependentName( String path) {
        return path == null ? null : FileUtilRt.toSystemDependentName(path);
    }

    
    public static String driveLetterToLowerCase( String path) {
        if (!SystemInfo.isWindows) {
            return path;
        }
        File file = new File(path);
        if (file.isAbsolute() && path.length() >= 2 &&
                Character.isUpperCase(path.charAt(0)) && path.charAt(1) == ':') {
            return Character.toLowerCase(path.charAt(0)) + path.substring(1);
        }
        return path;
    }

    
    public static String makeFileName( String name,  String extension) {
        return name + (StringUtil.isEmpty(extension) ? "" : "." + extension);
    }
}
