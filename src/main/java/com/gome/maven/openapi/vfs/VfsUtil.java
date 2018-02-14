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
package com.gome.maven.openapi.vfs;

import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.application.WriteAction;
import com.gome.maven.openapi.diagnostic.Logger;
//import com.gome.maven.openapi.application.Result;
//import com.gome.maven.openapi.application.WriteAction;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.Convertor;
import gnu.trove.THashSet;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

public class VfsUtil extends VfsUtilCore {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vfs.VfsUtil");

    /** @deprecated incorrect name, use {@link #VFS_SEPARATOR_CHAR} (to be removed in IDEA 15) */
    public static final char VFS_PATH_SEPARATOR = VFS_SEPARATOR_CHAR;

    public static void saveText( VirtualFile file,  String text) throws IOException {
        Charset charset = file.getCharset();
        file.setBinaryContent(text.getBytes(charset.name()));
    }

    /**
     * Copies all files matching the <code>filter</code> from <code>fromDir</code> to <code>toDir</code>.
     * Symlinks end special files are ignored.
     *
     * @param requestor any object to control who called this method. Note that
     *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
     *                  See {@link VirtualFileEvent#getRequestor}
     * @param fromDir   the directory to copy from
     * @param toDir     the directory to copy to
     * @param filter    {@link VirtualFileFilter}
     * @throws IOException if files failed to be copied
     */
    public static void copyDirectory(Object requestor,
                                      VirtualFile fromDir,
                                      VirtualFile toDir,
                                      VirtualFileFilter filter) throws IOException {
        @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] children = fromDir.getChildren();
        for (VirtualFile child : children) {
            if (!child.is(VFileProperty.SYMLINK) && !child.is(VFileProperty.SPECIAL) && (filter == null || filter.accept(child))) {
                if (!child.isDirectory()) {
                    copyFile(requestor, child, toDir);
                }
                else {
                    VirtualFile newChild = toDir.findChild(child.getName());
                    if (newChild == null) {
                        newChild = toDir.createChildDirectory(requestor, child.getName());
                    }
                    copyDirectory(requestor, child, newChild, filter);
                }
            }
        }
    }

    /**
     * Copies content of resource to the given file
     *
     * @param file to copy to
     * @param resourceUrl url of the resource to be copied
     * @throws java.io.IOException if resource not found or copying failed
     */
    public static void copyFromResource( VirtualFile file,   String resourceUrl) throws IOException {
        InputStream out = VfsUtil.class.getResourceAsStream(resourceUrl);
        if (out == null) {
            throw new FileNotFoundException(resourceUrl);
        }
        try {
            byte[] bytes = FileUtil.adaptiveLoadBytes(out);
            file.setBinaryContent(bytes);
        } finally {
            out.close();
        }
    }

    /**
     * Makes a copy of the <code>file</code> in the <code>toDir</code> folder and returns it.
     * Handles both files and directories.
     *
     * @param requestor any object to control who called this method. Note that
     *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
     *                  See {@link VirtualFileEvent#getRequestor}
     * @param file      file or directory to make a copy of
     * @param toDir     directory to make a copy in
     * @return a copy of the file
     * @throws IOException if file failed to be copied
     */
    public static VirtualFile copy(Object requestor,  VirtualFile file,  VirtualFile toDir) throws IOException {
        if (file.isDirectory()) {
            VirtualFile newDir = toDir.createChildDirectory(requestor, file.getName());
            copyDirectory(requestor, file, newDir, null);
            return newDir;
        }
        else {
            return copyFile(requestor, file, toDir);
        }
    }

    /**
     * Gets the array of common ancestors for passed files.
     *
     * @param files array of files
     * @return array of common ancestors for passed files
     */
    
    public static VirtualFile[] getCommonAncestors( VirtualFile[] files) {
        // Separate files by first component in the path.
        HashMap<VirtualFile,Set<VirtualFile>> map = new HashMap<VirtualFile, Set<VirtualFile>>();
        for (VirtualFile aFile : files) {
            VirtualFile directory = aFile.isDirectory() ? aFile : aFile.getParent();
            if (directory == null) return VirtualFile.EMPTY_ARRAY;
            VirtualFile[] path = getPathComponents(directory);
            Set<VirtualFile> filesSet;
            final VirtualFile firstPart = path[0];
            if (map.containsKey(firstPart)) {
                filesSet = map.get(firstPart);
            }
            else {
                filesSet = new THashSet<VirtualFile>();
                map.put(firstPart, filesSet);
            }
            filesSet.add(directory);
        }
        // Find common ancestor for each set of files.
        ArrayList<VirtualFile> ancestorsList = new ArrayList<VirtualFile>();
        for (Set<VirtualFile> filesSet : map.values()) {
            VirtualFile ancestor = null;
            for (VirtualFile file : filesSet) {
                if (ancestor == null) {
                    ancestor = file;
                    continue;
                }
                ancestor = getCommonAncestor(ancestor, file);
                //assertTrue(ancestor != null);
            }
            ancestorsList.add(ancestor);
            filesSet.clear();
        }
        return toVirtualFileArray(ancestorsList);
    }

    /**
     * Gets the common ancestor for passed files, or {@code null} if the files do not have common ancestors.
     */
    
    public static VirtualFile getCommonAncestor( Collection<? extends VirtualFile> files) {
        VirtualFile ancestor = null;
        for (VirtualFile file : files) {
            if (ancestor == null) {
                ancestor = file;
            }
            else {
                ancestor = getCommonAncestor(ancestor, file);
                if (ancestor == null) return null;
            }
        }
        return ancestor;
    }

    
    public static VirtualFile findRelativeFile( VirtualFile base, String ... path) {
        VirtualFile file = base;

        for (String pathElement : path) {
            if (file == null) return null;
            if ("..".equals(pathElement)) {
                file = file.getParent();
            }
            else {
                file = file.findChild(pathElement);
            }
        }

        return file;
    }

    /**
     * Searches for the file specified by given java,net.URL.
     * Note that this method currently tested only for "file" and "jar" protocols under Unix and Windows
     *
     * @param url the URL to find file by
     * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
     */
    public static VirtualFile findFileByURL( URL url) {
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        return findFileByURL(url, virtualFileManager);
    }

    public static VirtualFile findFileByURL( URL url,  VirtualFileManager virtualFileManager) {
        String vfUrl = convertFromUrl(url);
        return virtualFileManager.findFileByUrl(vfUrl);
    }

    
    public static VirtualFile findFileByIoFile( File file, boolean refreshIfNeeded) {
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile virtualFile = fileSystem.findFileByIoFile(file);
        if (refreshIfNeeded && (virtualFile == null || !virtualFile.isValid())) {
            virtualFile = fileSystem.refreshAndFindFileByIoFile(file);
        }
        return virtualFile;
    }

    /**
     * Converts VsfUrl info java.net.URL.
     *
     * @param vfsUrl VFS url (as constructed by VfsFile.getUrl())
     * @return converted URL or null if error has occured
     * @deprecated Use {@link VfsUtilCore#convertToURL(String)} instead. To be removed in IDEA 16.
     */
    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    
    public static URL convertToURL( String vfsUrl) {
        return VfsUtilCore.convertToURL(vfsUrl);
    }

    public static VirtualFile copyFileRelative(Object requestor,  VirtualFile file,  VirtualFile toDir,  String relativePath) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(relativePath,"/");
        VirtualFile curDir = toDir;

        while (true) {
            String token = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                VirtualFile childDir = curDir.findChild(token);
                if (childDir == null) {
                    childDir = curDir.createChildDirectory(requestor, token);
                }
                curDir = childDir;
            }
            else {
                return copyFile(requestor, file, curDir, token);
            }
        }
    }

    
    public static String toIdeaUrl( String url) {
        return toIdeaUrl(url, true);
    }

    /**
     * @return correct URL, must be used only for external communication
     */
    
    public static URI toUri( VirtualFile file) {
        String path = file.getPath();
        try {
            if (file.isInLocalFileSystem()) {
                if (SystemInfo.isWindows && path.charAt(0) != '/') {
                    path = '/' + path;
                }
                return new URI(file.getFileSystem().getProtocol(), "", path, null, null);
            }
            return new URI(file.getFileSystem().getProtocol(), path, null);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @return correct URL, must be used only for external communication
     */
    
    public static URI toUri( File file) {
        String path = file.toURI().getPath();
        try {
            if (SystemInfo.isWindows && path.charAt(0) != '/') {
                path = '/' + path;
            }
            return new URI(StandardFileSystems.FILE_PROTOCOL, "", path, null, null);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * uri - may be incorrect (escaping or missed "/" before disk name under windows), may be not fully encoded,
     * may contains query and fragment
     * @return correct URI, must be used only for external communication
     */
    
    public static URI toUri(  String uri) {
        int index = uri.indexOf("://");
        if (index < 0) {
            // true URI, like mailto:
            try {
                return new URI(uri);
            }
            catch (URISyntaxException e) {
                LOG.debug(e);
                return null;
            }
        }

        if (SystemInfo.isWindows && uri.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
            int firstSlashIndex = index + "://".length();
            if (uri.charAt(firstSlashIndex) != '/') {
                uri = LocalFileSystem.PROTOCOL_PREFIX + '/' + uri.substring(firstSlashIndex);
            }
        }

        try {
            return new URI(uri);
        }
        catch (URISyntaxException e) {
            LOG.debug("uri is not fully encoded", e);
            // so, uri is not fully encoded (space)
            try {
                int fragmentIndex = uri.lastIndexOf('#');
                String path = uri.substring(index + 1, fragmentIndex > 0 ? fragmentIndex : uri.length());
                String fragment = fragmentIndex > 0 ? uri.substring(fragmentIndex + 1) : null;
                return new URI(uri.substring(0, index), path, fragment);
            }
            catch (URISyntaxException e1) {
                LOG.debug(e1);
                return null;
            }
        }
    }

    /**
     * Returns the relative path from one virtual file to another.
     *
     * @param src           the file from which the relative path is built.
     * @param dst           the file to which the path is built.
     * @param separatorChar the separator for the path components.
     * @return the relative path, or null if the files have no common ancestor.
     * @since 5.0.2
     */
    
    public static String getPath( VirtualFile src,  VirtualFile dst, char separatorChar) {
        final VirtualFile commonAncestor = getCommonAncestor(src, dst);
        if (commonAncestor != null) {
            StringBuilder buffer = new StringBuilder();
            if (!Comparing.equal(src, commonAncestor)) {
                while (!Comparing.equal(src.getParent(), commonAncestor)) {
                    buffer.append("..").append(separatorChar);
                    src = src.getParent();
                }
            }
            buffer.append(getRelativePath(dst, commonAncestor, separatorChar));
            return buffer.toString();
        }

        return null;
    }

    public static String getUrlForLibraryRoot( File libraryRoot) {
        String path = FileUtil.toSystemIndependentName(libraryRoot.getAbsolutePath());
        if (FileTypeManager.getInstance().getFileTypeByFileName(libraryRoot.getName()) == FileTypes.ARCHIVE) {
            return VirtualFileManager.constructUrl(JarFileSystem.getInstance().getProtocol(), path + JarFileSystem.JAR_SEPARATOR);
        }
        else {
            return VirtualFileManager.constructUrl(LocalFileSystem.getInstance().getProtocol(), path);
        }
    }

    public static VirtualFile createChildSequent(Object requestor,  VirtualFile dir,  String prefix,  String extension) throws IOException {
        String dotExt = PathUtil.makeFileName("", extension);
        String fileName = prefix + dotExt;
        int i = 1;
        while (dir.findChild(fileName) != null) {
            fileName = prefix + "_" + i + dotExt;
            i++;
        }
        return dir.createChildData(requestor, fileName);
    }

    
    public static String[] filterNames( String[] names) {
        int filteredCount = 0;
        for (String string : names) {
            if (isBadName(string)) filteredCount++;
        }
        if (filteredCount == 0) return names;

        String[] result = ArrayUtil.newStringArray(names.length - filteredCount);
        int count = 0;
        for (String string : names) {
            if (isBadName(string)) continue;
            result[count++] = string;
        }

        return result;
    }

    public static boolean isBadName(String name) {
        return name == null || name.isEmpty() || "/".equals(name) || "\\".equals(name);
    }

    public static VirtualFile createDirectories( final String directoryPath) throws IOException {
        return new WriteAction<VirtualFile>() {
            @Override
            protected void run(Result<VirtualFile> result) throws Throwable {
                VirtualFile res = createDirectoryIfMissing(directoryPath);
                result.setResult(res);
            }
        }.execute().throwException().getResultObject();
    }

    public static VirtualFile createDirectoryIfMissing(VirtualFile parent, String relativePath) throws IOException {
        for (String each : StringUtil.split(relativePath, "/")) {
            VirtualFile child = parent.findChild(each);
            if (child == null) {
                child = parent.createChildDirectory(LocalFileSystem.getInstance(), each);
            }
            parent = child;
        }
        return parent;
    }

    
    public static VirtualFile createDirectoryIfMissing( String directoryPath) throws IOException {
        String path = FileUtil.toSystemIndependentName(directoryPath);
        final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (file == null) {
            int pos = path.lastIndexOf('/');
            if (pos < 0) return null;
            VirtualFile parent = createDirectoryIfMissing(path.substring(0, pos));
            if (parent == null) return null;
            final String dirName = path.substring(pos + 1);
            return parent.createChildDirectory(LocalFileSystem.getInstance(), dirName);
        }
        return file;
    }

    /**
     * Returns all files in some virtual files recursively
     * @param root virtual file to get descendants
     * @return descendants
     */
    
    public static List<VirtualFile> collectChildrenRecursively( final VirtualFile root) {
        final List<VirtualFile> result = new ArrayList<VirtualFile>();
        processFilesRecursively(root, new Processor<VirtualFile>() {
            @Override
            public boolean process(final VirtualFile t) {
                result.add(t);
                return true;
            }
        });
        return result;
    }


    public static void processFileRecursivelyWithoutIgnored( final VirtualFile root,  final Processor<VirtualFile> processor) {
        final FileTypeManager ftm = FileTypeManager.getInstance();
        processFilesRecursively(root, processor, new Convertor<VirtualFile, Boolean>() {
            public Boolean convert(final VirtualFile vf) {
                return ! ftm.isFileIgnored(vf);
            }
        });
    }

    
    public static <T> T processInputStream( final VirtualFile file,  Function<InputStream, T> function) {
        InputStream stream = null;
        try {
            stream = file.getInputStream();
            return function.fun(stream);
        }
        catch (IOException e) {
            LOG.error(e);
        }
        finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
        return null;
    }

    
    public static String getReadableUrl( final VirtualFile file) {
        String url = null;
        if (file.isInLocalFileSystem()) {
            url = file.getPresentableUrl();
        }
        if (url == null) {
            url = file.getUrl();
        }
        return url;
    }

    
    public static VirtualFile getUserHomeDir() {
        final String path = SystemProperties.getUserHome();
        return LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path));
    }

    
    public static VirtualFile[] getChildren( VirtualFile dir) {
        VirtualFile[] children = dir.getChildren();
        return children == null ? VirtualFile.EMPTY_ARRAY : children;
    }

    /**
     * @param url Url for virtual file
     * @return url for parent directory of virtual file
     */
    
    public static String getParentDir( final String url) {
        if (url == null) {
            return null;
        }
        final int index = url.lastIndexOf(VfsUtil.VFS_SEPARATOR_CHAR);
        return index < 0 ? null : url.substring(0, index);
    }

    /**
     * @param urlOrPath Url for virtual file
     * @return file name
     */
    
    public static String extractFileName( final String urlOrPath) {
        if (urlOrPath == null) {
            return null;
        }
        final int index = urlOrPath.lastIndexOf(VfsUtil.VFS_SEPARATOR_CHAR);
        return index < 0 ? null : urlOrPath.substring(index+1);
    }

    
    public static List<VirtualFile> markDirty(boolean recursive, boolean reloadChildren,  VirtualFile... files) {
        List<VirtualFile> list = ContainerUtil.filter(Condition.NOT_NULL, files);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        for (VirtualFile file : list) {
            if (reloadChildren) {
                file.getChildren();
            }

            if (file instanceof NewVirtualFile) {
                if (recursive) {
                    ((NewVirtualFile)file).markDirtyRecursively();
                }
                else {
                    ((NewVirtualFile)file).markDirty();
                }
            }
        }
        return list;
    }

    public static void markDirtyAndRefresh(boolean async, boolean recursive, boolean reloadChildren,  VirtualFile... files) {
        List<VirtualFile> list = markDirty(recursive, reloadChildren, files);
        if (list.isEmpty()) return;
        LocalFileSystem.getInstance().refreshFiles(list, async, recursive, null);
    }
}