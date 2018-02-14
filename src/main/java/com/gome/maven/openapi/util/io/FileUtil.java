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
package com.gome.maven.openapi.util.io;

import com.gome.maven.CommonBundle;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.*;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.util.concurrency.FixedFuture;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.Convertor;
import com.gome.maven.openapi.util.text.FilePathHashingStrategy;
import com.gome.maven.util.io.URLUtil;
import com.gome.maven.util.text.StringFactory;
import com.gome.maven.openapi.util.text.StringUtil;
import gnu.trove.TObjectHashingStrategy;


import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Pattern;

@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "MethodOverridesStaticMethodOfSuperclass"})
public class FileUtil extends FileUtilRt {

     public static final String ASYNC_DELETE_EXTENSION = ".__del__";

    public static final int REGEX_PATTERN_FLAGS = SystemInfo.isFileSystemCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;

    public static final TObjectHashingStrategy<String> PATH_HASHING_STRATEGY = FilePathHashingStrategy.create();

    public static final TObjectHashingStrategy<File> FILE_HASHING_STRATEGY =
            SystemInfo.isFileSystemCaseSensitive ? ContainerUtil.<File>canonicalStrategy() : new TObjectHashingStrategy<File>() {
                @Override
                public int computeHashCode(File object) {
                    return fileHashCode(object);
                }

                @Override
                public boolean equals(File o1, File o2) {
                    return filesEqual(o1, o2);
                }
            };

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.util.io.FileUtil");

    
    public static String join( final String... parts) {
        return StringUtil.join(parts, File.separator);
    }

    
    public static String getRelativePath(File base, File file) {
        return FileUtilRt.getRelativePath(base, file);
    }

    
    public static String getRelativePath( String basePath,  String filePath, final char separator) {
        return FileUtilRt.getRelativePath(basePath, filePath, separator);
    }

    
    public static String getRelativePath( String basePath,
                                          String filePath,
                                         final char separator,
                                         final boolean caseSensitive) {
        return FileUtilRt.getRelativePath(basePath, filePath, separator, caseSensitive);
    }

    public static boolean isAbsolute( String path) {
        return new File(path).isAbsolute();
    }

    /**
     * Check if the {@code ancestor} is an ancestor of {@code file}.
     *
     * @param ancestor supposed ancestor.
     * @param file     supposed descendant.
     * @param strict   if {@code false} then this method returns {@code true} if {@code ancestor} equals to {@code file}.
     * @return {@code true} if {@code ancestor} is parent of {@code file}; {@code false} otherwise.
     */
    public static boolean isAncestor( File ancestor,  File file, boolean strict) {
        return isAncestor(ancestor.getPath(), file.getPath(), strict);
    }

    public static boolean isAncestor( String ancestor,  String file, boolean strict) {
        return !ThreeState.NO.equals(isAncestorThreeState(ancestor, file, strict));
    }

    /**
     * Checks if the {@code ancestor} is an ancestor of the {@code file}, and if it is an immediate parent or not.
     *
     * @param ancestor supposed ancestor.
     * @param file     supposed descendant.
     * @param strict   if {@code false}, the file can be ancestor of itself,
     *                 i.e. the method returns {@code ThreeState.YES} if {@code ancestor} equals to {@code file}.
     *
     * @return {@code ThreeState.YES} if ancestor is an immediate parent of the file,
     *         {@code ThreeState.UNSURE} if ancestor is not immediate parent of the file,
     *         {@code ThreeState.NO} if ancestor is not a parent of the file at all.
     */
    
    public static ThreeState isAncestorThreeState( String ancestor,  String file, boolean strict) {
        String ancestorPath = toCanonicalPath(ancestor);
        String filePath = toCanonicalPath(file);
        return startsWith(filePath, ancestorPath, strict, SystemInfo.isFileSystemCaseSensitive, true);
    }

    public static boolean startsWith( String path,  String start) {
        return !ThreeState.NO.equals(startsWith(path, start, false, SystemInfo.isFileSystemCaseSensitive, false));
    }

    public static boolean startsWith( String path,  String start, boolean caseSensitive) {
        return !ThreeState.NO.equals(startsWith(path, start, false, caseSensitive, false));
    }

    
    private static ThreeState startsWith( String path,  String prefix, boolean strict, boolean caseSensitive,
                                         boolean checkImmediateParent) {
        final int pathLength = path.length();
        final int prefixLength = prefix.length();
        if (prefixLength == 0) return pathLength == 0 ? ThreeState.YES : ThreeState.UNSURE;
        if (prefixLength > pathLength) return ThreeState.NO;
        if (!path.regionMatches(!caseSensitive, 0, prefix, 0, prefixLength)) return ThreeState.NO;
        if (pathLength == prefixLength) {
            return strict ? ThreeState.NO : ThreeState.YES;
        }
        char lastPrefixChar = prefix.charAt(prefixLength - 1);
        int slashOrSeparatorIdx = prefixLength;
        if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
            slashOrSeparatorIdx = prefixLength - 1;
        }
        char next1 = path.charAt(slashOrSeparatorIdx);
        if (next1 == '/' || next1 == File.separatorChar) {
            if (!checkImmediateParent) return ThreeState.YES;

            if (slashOrSeparatorIdx == pathLength - 1) return ThreeState.YES;
            int idxNext = path.indexOf(next1, slashOrSeparatorIdx + 1);
            idxNext = idxNext == -1 ? path.indexOf(next1 == '/' ? '\\' : '/', slashOrSeparatorIdx + 1) : idxNext;
            return idxNext == -1 ? ThreeState.YES : ThreeState.UNSURE;
        }
        else {
            return ThreeState.NO;
        }
    }

    /**
     * @param removeProcessor parent, child
     */
    public static <T> Collection<T> removeAncestors(final Collection<T> files,
                                                    final Convertor<T, String> convertor,
                                                    final PairProcessor<T, T> removeProcessor) {
        if (files.isEmpty()) return files;
        final TreeMap<String, T> paths = new TreeMap<String, T>();
        for (T file : files) {
            final String path = convertor.convert(file);
            assert path != null;
            final String canonicalPath = toCanonicalPath(path);
            paths.put(canonicalPath, file);
        }
        final List<Map.Entry<String, T>> ordered = new ArrayList<Map.Entry<String, T>>(paths.entrySet());
        final List<T> result = new ArrayList<T>(ordered.size());
        result.add(ordered.get(0).getValue());
        for (int i = 1; i < ordered.size(); i++) {
            final Map.Entry<String, T> entry = ordered.get(i);
            final String child = entry.getKey();
            boolean parentNotFound = true;
            for (int j = i - 1; j >= 0; j--) {
                // possible parents
                final String parent = ordered.get(j).getKey();
                if (parent == null) continue;
                if (startsWith(child, parent) && removeProcessor.process(ordered.get(j).getValue(), entry.getValue())) {
                    parentNotFound = false;
                    break;
                }
            }
            if (parentNotFound) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    
    public static File getParentFile( File file) {
        return FileUtilRt.getParentFile(file);
    }

    
    public static byte[] loadFileBytes( File file) throws IOException {
        byte[] bytes;
        final InputStream stream = new FileInputStream(file);
        try {
            final long len = file.length();
            if (len < 0) {
                throw new IOException("File length reported negative, probably doesn't exist");
            }

            if (isTooLarge(len)) {
                throw new FileTooBigException("Attempt to load '" + file + "' in memory buffer, file length is " + len + " bytes.");
            }

            bytes = loadBytes(stream, (int)len);
        }
        finally {
            stream.close();
        }
        return bytes;
    }

    public static boolean processFirstBytes( InputStream stream, int length,  Processor<ByteSequence> processor)
            throws IOException {
        final byte[] bytes = BUFFER.get();
        assert bytes.length >= length : "Cannot process more than " + bytes.length + " in one call, requested:" + length;

        int n = stream.read(bytes, 0, length);
        if (n <= 0) return false;

        return processor.process(new ByteSequence(bytes, 0, n));
    }

    
    public static byte[] loadFirst( InputStream stream, int maxLength) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] bytes = BUFFER.get();
        while (maxLength > 0) {
            int n = stream.read(bytes, 0, Math.min(maxLength, bytes.length));
            if (n <= 0) break;
            buffer.write(bytes, 0, n);
            maxLength -= n;
        }
        buffer.close();
        return buffer.toByteArray();
    }

    
    public static String loadTextAndClose( InputStream stream) throws IOException {
        //noinspection IOResourceOpenedButNotSafelyClosed
        return loadTextAndClose(new InputStreamReader(stream));
    }

    
    public static String loadTextAndClose( Reader reader) throws IOException {
        try {
            return StringFactory.createShared(adaptiveLoadText(reader));
        }
        finally {
            reader.close();
        }
    }

    
    public static char[] adaptiveLoadText( Reader reader) throws IOException {
        char[] chars = new char[4096];
        List<char[]> buffers = null;
        int count = 0;
        int total = 0;
        while (true) {
            int n = reader.read(chars, count, chars.length - count);
            if (n <= 0) break;
            count += n;
            if (total > 1024 * 1024 * 10) throw new FileTooBigException("File too big " + reader);
            total += n;
            if (count == chars.length) {
                if (buffers == null) {
                    buffers = new ArrayList<char[]>();
                }
                buffers.add(chars);
                int newLength = Math.min(1024 * 1024, chars.length * 2);
                chars = new char[newLength];
                count = 0;
            }
        }
        char[] result = new char[total];
        if (buffers != null) {
            for (char[] buffer : buffers) {
                System.arraycopy(buffer, 0, result, result.length - total, buffer.length);
                total -= buffer.length;
            }
        }
        System.arraycopy(chars, 0, result, result.length - total, total);
        return result;
    }

    
    public static byte[] adaptiveLoadBytes( InputStream stream) throws IOException {
        byte[] bytes = new byte[4096];
        List<byte[]> buffers = null;
        int count = 0;
        int total = 0;
        while (true) {
            int n = stream.read(bytes, count, bytes.length - count);
            if (n <= 0) break;
            count += n;
            if (total > 1024 * 1024 * 10) throw new FileTooBigException("File too big " + stream);
            total += n;
            if (count == bytes.length) {
                if (buffers == null) {
                    buffers = new ArrayList<byte[]>();
                }
                buffers.add(bytes);
                int newLength = Math.min(1024 * 1024, bytes.length * 2);
                bytes = new byte[newLength];
                count = 0;
            }
        }
        byte[] result = new byte[total];
        if (buffers != null) {
            for (byte[] buffer : buffers) {
                System.arraycopy(buffer, 0, result, result.length - total, buffer.length);
                total -= buffer.length;
            }
        }
        System.arraycopy(bytes, 0, result, result.length - total, total);
        return result;
    }

    
    public static Future<Void> asyncDelete( File file) {
        return asyncDelete(Collections.singleton(file));
    }

    
    public static Future<Void> asyncDelete( Collection<File> files) {
        List<File> tempFiles = new ArrayList<File>();
        for (File file : files) {
            final File tempFile = renameToTempFileOrDelete(file);
            if (tempFile != null) {
                tempFiles.add(tempFile);
            }
        }
        if (!tempFiles.isEmpty()) {
            return startDeletionThread(tempFiles.toArray(new File[tempFiles.size()]));
        }
        return new FixedFuture<Void>(null);
    }

    private static Future<Void> startDeletionThread( final File... tempFiles) {
        final RunnableFuture<Void> deleteFilesTask = new FutureTask<Void>(new Runnable() {
            @Override
            public void run() {
                final Thread currentThread = Thread.currentThread();
                final int priority = currentThread.getPriority();
                currentThread.setPriority(Thread.MIN_PRIORITY);
                try {
                    for (File tempFile : tempFiles) {
                        delete(tempFile);
                    }
                }
                finally {
                    currentThread.setPriority(priority);
                }
            }
        }, null);

        try {
            // attempt to execute on pooled thread
            final Class<?> aClass = Class.forName("com.gome.maven.openapi.application.ApplicationManager");
            final Method getApplicationMethod = aClass.getMethod("getApplication");
            final Object application = getApplicationMethod.invoke(null);
            final Method executeOnPooledThreadMethod = application.getClass().getMethod("executeOnPooledThread", Runnable.class);
            executeOnPooledThreadMethod.invoke(application, deleteFilesTask);
        }
        catch (Exception ignored) {
            new Thread(deleteFilesTask, "File deletion thread").start();
        }
        return deleteFilesTask;
    }

    
    private static File renameToTempFileOrDelete( File file) {
        String tempDir = getTempDirectory();
        boolean isSameDrive = true;
        if (SystemInfo.isWindows) {
            String tempDirDrive = tempDir.substring(0, 2);
            String fileDrive = file.getAbsolutePath().substring(0, 2);
            isSameDrive = tempDirDrive.equalsIgnoreCase(fileDrive);
        }

        if (isSameDrive) {
            // the optimization is reasonable only if destination dir is located on the same drive
            final String originalFileName = file.getName();
            File tempFile = getTempFile(originalFileName, tempDir);
            if (file.renameTo(tempFile)) {
                return tempFile;
            }
        }

        delete(file);

        return null;
    }

    private static File getTempFile( String originalFileName,  String parent) {
        int randomSuffix = (int)(System.currentTimeMillis() % 1000);
        for (int i = randomSuffix; ; i++) {
             String name = "___" + originalFileName + i + ASYNC_DELETE_EXTENSION;
            File tempFile = new File(parent, name);
            if (!tempFile.exists()) return tempFile;
        }
    }

    public static boolean delete( File file) {
        if (NIOReflect.IS_AVAILABLE) {
            return deleteRecursivelyNIO(file);
        }
        return deleteRecursively(file);
    }

    private static boolean deleteRecursively( File file) {
        FileAttributes attributes = FileSystemUtil.getAttributes(file);
        if (attributes == null) return true;

        if (attributes.isDirectory() && !attributes.isSymLink()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (!deleteRecursively(child)) return false;
                }
            }
        }

        return deleteFile(file);
    }

    public static boolean createParentDirs( File file) {
        return FileUtilRt.createParentDirs(file);
    }

    public static boolean createDirectory( File path) {
        return FileUtilRt.createDirectory(path);
    }

    public static boolean createIfDoesntExist( File file) {
        return FileUtilRt.createIfNotExists(file);
    }

    public static boolean ensureCanCreateFile( File file) {
        return FileUtilRt.ensureCanCreateFile(file);
    }

    public static void copy( File fromFile,  File toFile) throws IOException {
        performCopy(fromFile, toFile, true);
    }

    public static void copyContent( File fromFile,  File toFile) throws IOException {
        performCopy(fromFile, toFile, false);
    }

    private static void performCopy( File fromFile,  File toFile, final boolean syncTimestamp) throws IOException {
        final FileOutputStream fos;
        try {
            fos = openOutputStream(toFile);
        }
        catch (IOException e) {
            if (SystemInfo.isWindows && e.getMessage() != null && e.getMessage().contains("denied") &&
                    WinUACTemporaryFix.nativeCopy(fromFile, toFile, syncTimestamp)) {
                return;
            }
            throw e;
        }

        try {
            final FileInputStream fis = new FileInputStream(fromFile);
            try {
                copy(fis, fos);
            }
            finally {
                fis.close();
            }
        }
        finally {
            fos.close();
        }

        if (syncTimestamp) {
            final long timeStamp = fromFile.lastModified();
            if (timeStamp < 0) {
                LOG.warn("Invalid timestamp " + timeStamp + " of '" + fromFile + "'");
            }
            else if (!toFile.setLastModified(timeStamp)) {
                LOG.warn("Unable to set timestamp " + timeStamp + " to '" + toFile + "'");
            }
        }

        if (SystemInfo.isUnix && fromFile.canExecute()) {
            FileSystemUtil.clonePermissionsToExecute(fromFile.getPath(), toFile.getPath());
        }
    }

    private static FileOutputStream openOutputStream( final File file) throws IOException {
        try {
            return new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {
            final File parentFile = file.getParentFile();
            if (parentFile == null) {
                throw new IOException("Parent file is null for " + file.getPath(), e);
            }
            createParentDirs(file);
            return new FileOutputStream(file);
        }
    }

    public static void copy( InputStream inputStream,  OutputStream outputStream) throws IOException {
        FileUtilRt.copy(inputStream, outputStream);
    }

    public static void copy( InputStream inputStream, int maxSize,  OutputStream outputStream) throws IOException {
        final byte[] buffer = BUFFER.get();
        int toRead = maxSize;
        while (toRead > 0) {
            int read = inputStream.read(buffer, 0, Math.min(buffer.length, toRead));
            if (read < 0) break;
            toRead -= read;
            outputStream.write(buffer, 0, read);
        }
    }

    public static void copyDir( File fromDir,  File toDir) throws IOException {
        copyDir(fromDir, toDir, true);
    }

    /**
     * Copies content of {@code fromDir} to {@code toDir}.
     * It's equivalent to "cp -r fromDir/* toDir" unix command.
     *
     * @param fromDir source directory
     * @param toDir   destination directory
     * @throws IOException in case of any IO troubles
     */
    public static void copyDirContent( File fromDir,  File toDir) throws IOException {
        File[] children = ObjectUtils.notNull(fromDir.listFiles(), ArrayUtil.EMPTY_FILE_ARRAY);
        for (File child : children) {
            File target = new File(toDir, child.getName());
            if (child.isFile()) {
                copy(child, target);
            }
            else {
                copyDir(child, target, true);
            }
        }
    }

    public static void copyDir( File fromDir,  File toDir, boolean copySystemFiles) throws IOException {
        copyDir(fromDir, toDir, copySystemFiles ? null : new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !StringUtil.startsWithChar(file.getName(), '.');
            }
        });
    }

    public static void copyDir( File fromDir,  File toDir,  final FileFilter filter) throws IOException {
        ensureExists(toDir);
        if (isAncestor(fromDir, toDir, true)) {
            LOG.error(fromDir.getAbsolutePath() + " is ancestor of " + toDir + ". Can't copy to itself.");
            return;
        }
        File[] files = fromDir.listFiles();
        if (files == null) throw new IOException(CommonBundle.message("exception.directory.is.invalid", fromDir.getPath()));
        if (!fromDir.canRead()) throw new IOException(CommonBundle.message("exception.directory.is.not.readable", fromDir.getPath()));
        for (File file : files) {
            if (filter != null && !filter.accept(file)) {
                continue;
            }
            if (file.isDirectory()) {
                copyDir(file, new File(toDir, file.getName()), filter);
            }
            else {
                copy(file, new File(toDir, file.getName()));
            }
        }
    }

    public static void ensureExists( File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException(CommonBundle.message("exception.directory.can.not.create", dir.getPath()));
        }
    }

    
    public static String getNameWithoutExtension( File file) {
        return getNameWithoutExtension(file.getName());
    }

    
    public static String getNameWithoutExtension( String name) {
        return FileUtilRt.getNameWithoutExtension(name);
    }

    public static String createSequentFileName( File aParentFolder,   String aFilePrefix,  String aExtension) {
        return findSequentNonexistentFile(aParentFolder, aFilePrefix, aExtension).getName();
    }

    public static File findSequentNonexistentFile( File parentFolder,   String filePrefix,  String extension) {
        int postfix = 0;
        String ext = extension.isEmpty() ? "" : '.' + extension;
        File candidate = new File(parentFolder, filePrefix + ext);
        while (candidate.exists()) {
            postfix++;
            candidate = new File(parentFolder, filePrefix + Integer.toString(postfix) + ext);
        }
        return candidate;
    }

    
    public static String toSystemDependentName(  String aFileName) {
        return FileUtilRt.toSystemDependentName(aFileName);
    }

    
    public static String toSystemIndependentName(  String aFileName) {
        return FileUtilRt.toSystemIndependentName(aFileName);
    }

    
    public static String nameToCompare(  String name) {
        return (SystemInfo.isFileSystemCaseSensitive ? name : name.toLowerCase()).replace('\\', '/');
    }

    /**
     * Converts given path to canonical representation by eliminating '.'s, traversing '..'s, and omitting duplicate separators.
     * Please note that this method is symlink-unfriendly (i.e. result of "/path/to/link/../next" most probably will differ from
     * what {@link java.io.File#getCanonicalPath()} will return) - so use with care.<br>
     * <br>
     * If the path may contain symlinks, use {@link FileUtil#toCanonicalPath(String, boolean)} instead.
     */
    public static String toCanonicalPath( String path) {
        return toCanonicalPath(path, File.separatorChar, true);
    }

    /**
     * When relative ../ parts do not escape outside of symlinks, the links are not expanded.<br>
     * That is, in the best-case scenario the original non-expanded path is preserved.<br>
     * <br>
     * Otherwise, returns a fully resolved path using {@link java.io.File#getCanonicalPath()}.<br>
     * <br>
     * Consider the following case:
     * <pre>
     * root/
     *   dir1/
     *     link_to_dir1
     *   dir2/
     * </pre>
     * 'root/dir1/link_to_dir1/../dir2' should be resolved to 'root/dir2'
     */
    public static String toCanonicalPath( String path, boolean resolveSymlinksIfNecessary) {
        return toCanonicalPath(path, File.separatorChar, true, resolveSymlinksIfNecessary);
    }

    public static String toCanonicalPath( String path, char separatorChar) {
        return toCanonicalPath(path, separatorChar, true);
    }

    
    public static String toCanonicalUriPath( String path) {
        return toCanonicalPath(path, '/', false);
    }

    
    private static String toCanonicalPath( String path, char separatorChar, boolean removeLastSlash) {
        return toCanonicalPath(path, separatorChar, removeLastSlash, false);
    }

    private static String toCanonicalPath( String path,
                                          final char separatorChar,
                                          final boolean removeLastSlash,
                                          final boolean resolveSymlinksIfNecessary) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        else if (".".equals(path)) {
            return "";
        }

        path = path.replace(separatorChar, '/');
        if (path.indexOf('/') == -1) {
            return path;
        }

        final String finalPath = path;
        NotNullProducer<String> realCanonicalPath = !resolveSymlinksIfNecessary ? null : new NotNullProducer<String>() {
            
            @Override
            public String produce() {
                try {
                    return new File(finalPath).getCanonicalPath().replace(separatorChar, '/');
                }
                catch (IOException ignore) {
                    // fall back to the default behavior
                    return toCanonicalPath(finalPath, separatorChar, removeLastSlash, false);
                }
            }
        };

        StringBuilder result = new StringBuilder(path.length());
        int start = processRoot(path, result), dots = 0;
        boolean separator = true;

        for (int i = start; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '/') {
                if (!separator) {
                    if (!processDots(result, dots, start, resolveSymlinksIfNecessary)) {
                        return realCanonicalPath.produce();
                    }
                    dots = 0;
                }
                separator = true;
            }
            else if (c == '.') {
                if (separator || dots > 0) {
                    ++dots;
                }
                else {
                    result.append('.');
                }
                separator = false;
            }
            else {
                if (dots > 0) {
                    StringUtil.repeatSymbol(result, '.', dots);
                    dots = 0;
                }
                result.append(c);
                separator = false;
            }
        }

        if (dots > 0) {
            if (!processDots(result, dots, start, resolveSymlinksIfNecessary)) {
                return realCanonicalPath.produce();
            }
        }

        int lastChar = result.length() - 1;
        if (removeLastSlash && lastChar >= 0 && result.charAt(lastChar) == '/' && lastChar > start) {
            result.deleteCharAt(lastChar);
        }

        return result.toString();
    }

    private static int processRoot(String path, StringBuilder result) {
        if (SystemInfo.isWindows && path.length() > 1 && path.charAt(0) == '/' && path.charAt(1) == '/') {
            result.append("//");

            int hostStart = 2;
            while (hostStart < path.length() && path.charAt(hostStart) == '/') hostStart++;
            if (hostStart == path.length()) return hostStart;
            int hostEnd = path.indexOf('/', hostStart);
            if (hostEnd < 0) hostEnd = path.length();
            result.append(path, hostStart, hostEnd);
            result.append('/');

            int shareStart = hostEnd;
            while (shareStart < path.length() && path.charAt(shareStart) == '/') shareStart++;
            if (shareStart == path.length()) return shareStart;
            int shareEnd = path.indexOf('/', shareStart);
            if (shareEnd < 0) shareEnd = path.length();
            result.append(path, shareStart, shareEnd);
            result.append('/');

            return shareEnd;
        }
        else if (path.length() > 0 && path.charAt(0) == '/') {
            result.append('/');
            return 1;
        }
        else if (path.length() > 2 && path.charAt(1) == ':' && path.charAt(2) == '/') {
            result.append(path, 0, 3);
            return 3;
        }
        else {
            return 0;
        }
    }

    private static boolean processDots( StringBuilder result, int dots, int start, boolean resolveSymlinksIfNecessary) {
        if (dots == 2) {
            int pos = -1;
            if (!StringUtil.endsWith(result, "/../") && !StringUtil.equals(result, "../")) {
                pos = StringUtil.lastIndexOf(result, '/', start, result.length() - 1);
                if (pos >= 0) {
                    ++pos;  // separator found, trim to next char
                }
                else if (start > 0) {
                    pos = start;  // path is absolute, trim to root ('/..' -> '/')
                }
                else if (result.length() > 0) {
                    pos = 0;  // path is relative, trim to default ('a/..' -> '')
                }
            }
            if (pos >= 0) {
                if (resolveSymlinksIfNecessary && FileSystemUtil.isSymLink(new File(result.toString()))) {
                    return false;
                }
                result.delete(pos, result.length());
            }
            else {
                result.append("../");  // impossible to traverse, keep as-is
            }
        }
        else if (dots != 1) {
            StringUtil.repeatSymbol(result, '.', dots);
            result.append('/');
        }
        return true;
    }

    /**
     * converts back slashes to forward slashes
     * removes double slashes inside the path, e.g. "x/y//z" => "x/y/z"
     */
    
    public static String normalize( String path) {
        int start = 0;
        boolean separator = false;
        if (SystemInfo.isWindows) {
            if (path.startsWith("//")) {
                start = 2;
                separator = true;
            }
            else if (path.startsWith("\\\\")) {
                return normalizeTail(0, path, false);
            }
        }

        for (int i = start; i < path.length(); ++i) {
            final char c = path.charAt(i);
            if (c == '/') {
                if (separator) {
                    return normalizeTail(i, path, true);
                }
                separator = true;
            }
            else if (c == '\\') {
                return normalizeTail(i, path, separator);
            }
            else {
                separator = false;
            }
        }

        return path;
    }

    
    private static String normalizeTail(int prefixEnd,  String path, boolean separator) {
        final StringBuilder result = new StringBuilder(path.length());
        result.append(path, 0, prefixEnd);
        int start = prefixEnd;
        if (start==0 && SystemInfo.isWindows && (path.startsWith("//") || path.startsWith("\\\\"))) {
            start = 2;
            result.append("//");
            separator = true;
        }

        for (int i = start; i < path.length(); ++i) {
            final char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                if (!separator) result.append('/');
                separator = true;
            }
            else {
                result.append(c);
                separator = false;
            }
        }

        return result.toString();
    }

    
    public static String unquote( String urlString) {
        urlString = urlString.replace('/', File.separatorChar);
        return URLUtil.unescapePercentSequences(urlString);
    }

    public static boolean isFilePathAcceptable( File root,  FileFilter fileFilter) {
        if (fileFilter == null) return true;
        File file = root;
        do {
            if (!fileFilter.accept(file)) return false;
            file = file.getParentFile();
        }
        while (file != null);
        return true;
    }

    public static void rename( File source,  File target) throws IOException {
        if (source.renameTo(target)) return;
        if (!source.exists()) return;

        copy(source, target);
        delete(source);
    }

    public static boolean filesEqual( File file1,  File file2) {
        // on MacOS java.io.File.equals() is incorrectly case-sensitive
        return pathsEqual(file1 == null ? null : file1.getPath(),
                file2 == null ? null : file2.getPath());
    }

    public static boolean pathsEqual( String path1,  String path2) {
        if (path1 == path2) return true;
        if (path1 == null || path2 == null) return false;

        path1 = toCanonicalPath(path1);
        path2 = toCanonicalPath(path2);
        return PATH_HASHING_STRATEGY.equals(path1, path2);
    }

    /**
     * optimized version of pathsEqual - it only compares pure names, without file separators
     */
    public static boolean namesEqual( String name1,  String name2) {
        if (name1 == name2) return true;
        if (name1 == null || name2 == null) return false;

        return PATH_HASHING_STRATEGY.equals(name1, name2);
    }

    public static int compareFiles( File file1,  File file2) {
        return comparePaths(file1 == null ? null : file1.getPath(), file2 == null ? null : file2.getPath());
    }

    public static int comparePaths( String path1,  String path2) {
        path1 = path1 == null ? null : toSystemIndependentName(path1);
        path2 = path2 == null ? null : toSystemIndependentName(path2);
        return StringUtil.compare(path1, path2, !SystemInfo.isFileSystemCaseSensitive);
    }

    public static int fileHashCode( File file) {
        return pathHashCode(file == null ? null : file.getPath());
    }

    public static int pathHashCode( String path) {
        return StringUtil.isEmpty(path) ? 0 : PATH_HASHING_STRATEGY.computeHashCode(toCanonicalPath(path));
    }

    /**
     * @deprecated this method returns extension converted to lower case, this may not be correct for case-sensitive FS.
     *             Use {@link FileUtilRt#getExtension(String)} instead to get the unchanged extension.
     *             If you need to check whether a file has a specified extension use {@link FileUtilRt#extensionEquals(String, String)}
     */
    
    public static String getExtension( String fileName) {
        return FileUtilRt.getExtension(fileName).toLowerCase();
    }

    
    public static String resolveShortWindowsName( String path) throws IOException {
        return SystemInfo.isWindows && StringUtil.containsChar(path, '~') ? new File(path).getCanonicalPath() : path;
    }

    public static void collectMatchedFiles( File root,  Pattern pattern,  List<File> outFiles) {
        collectMatchedFiles(root, root, pattern, outFiles);
    }

    private static void collectMatchedFiles( File absoluteRoot,
                                             File root,
                                             Pattern pattern,
                                             List<File> files) {
        final File[] dirs = root.listFiles();
        if (dirs == null) return;
        for (File dir : dirs) {
            if (dir.isFile()) {
                final String relativePath = getRelativePath(absoluteRoot, dir);
                if (relativePath != null) {
                    final String path = toSystemIndependentName(relativePath);
                    if (pattern.matcher(path).matches()) {
                        files.add(dir);
                    }
                }
            }
            else {
                collectMatchedFiles(absoluteRoot, dir, pattern, files);
            }
        }
    }


    public static String convertAntToRegexp( String antPattern) {
        return convertAntToRegexp(antPattern, true);
    }

    /**
     * @param antPattern ant-style path pattern
     * @return java regexp pattern.
     *         Note that no matter whether forward or backward slashes were used in the antPattern
     *         the returned regexp pattern will use forward slashes ('/') as file separators.
     *         Paths containing windows-style backslashes must be converted before matching against the resulting regexp
     * @see com.gome.maven.openapi.util.io.FileUtil#toSystemIndependentName
     */

    public static String convertAntToRegexp( String antPattern, boolean ignoreStartingSlash) {
        final StringBuilder builder = new StringBuilder();
        int asteriskCount = 0;
        boolean recursive = true;
        final int start =
                ignoreStartingSlash && (StringUtil.startsWithChar(antPattern, '/') || StringUtil.startsWithChar(antPattern, '\\')) ? 1 : 0;
        for (int idx = start; idx < antPattern.length(); idx++) {
            final char ch = antPattern.charAt(idx);

            if (ch == '*') {
                asteriskCount++;
                continue;
            }

            final boolean foundRecursivePattern = recursive && asteriskCount == 2 && (ch == '/' || ch == '\\');
            final boolean asterisksFound = asteriskCount > 0;

            asteriskCount = 0;
            recursive = ch == '/' || ch == '\\';

            if (foundRecursivePattern) {
                builder.append("(?:[^/]+/)*?");
                continue;
            }

            if (asterisksFound) {
                builder.append("[^/]*?");
            }

            if (ch == '(' ||
                    ch == ')' ||
                    ch == '[' ||
                    ch == ']' ||
                    ch == '^' ||
                    ch == '$' ||
                    ch == '.' ||
                    ch == '{' ||
                    ch == '}' ||
                    ch == '+' ||
                    ch == '|') {
                // quote regexp-specific symbols
                builder.append('\\').append(ch);
                continue;
            }
            if (ch == '?') {
                builder.append("[^/]{1}");
                continue;
            }
            if (ch == '\\') {
                builder.append('/');
                continue;
            }
            builder.append(ch);
        }

        // handle ant shorthand: mypackage/test/ is interpreted as if it were mypackage/test/**
        final boolean isTrailingSlash = builder.length() > 0 && builder.charAt(builder.length() - 1) == '/';
        if (asteriskCount == 0 && isTrailingSlash || recursive && asteriskCount == 2) {
            if (isTrailingSlash) {
                builder.setLength(builder.length() - 1);
            }
            if (builder.length() == 0) {
                builder.append(".*");
            }
            else {
                builder.append("(?:$|/.+)");
            }
        }
        else if (asteriskCount > 0) {
            builder.append("[^/]*?");
        }
        return builder.toString();
    }

    public static boolean moveDirWithContent( File fromDir,  File toDir) {
        if (!toDir.exists()) return fromDir.renameTo(toDir);

        File[] files = fromDir.listFiles();
        if (files == null) return false;

        boolean success = true;

        for (File fromFile : files) {
            File toFile = new File(toDir, fromFile.getName());
            success = success && fromFile.renameTo(toFile);
        }
        //noinspection ResultOfMethodCallIgnored
        fromDir.delete();

        return success;
    }

    /**
     * Has duplicate: {@link com.gome.maven.coverage.listeners.CoverageListener#sanitize(java.lang.String, java.lang.String)}
     * as FileUtil is not available in client's vm
     */
    
    public static String sanitizeFileName( String name) {
        return sanitizeFileName(name, true);
    }

    /**
     * Difference - not only letter or digit allowed, but space, @, -
     */
    
    public static String sanitizeName( String name) {
        return sanitizeFileName(name, false);
    }

    
    private static String sanitizeFileName( String name, boolean strict) {
        StringBuilder result = null;
        int last = 0;
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            boolean appendReplacement = true;
            if (c > 0 && c < 255) {
                if (strict ? (Character.isLetterOrDigit(c) || c == '_') : (Character.isJavaIdentifierPart(c) || c == ' ' || c == '@' || c == '-')) {
                    continue;
                }
            }
            else {
                appendReplacement = false;
            }

            if (result == null) {
                result = new StringBuilder();
            }
            if (last < i) {
                result.append(name, last, i);
            }
            if (appendReplacement) {
                result.append('_');
            }
            last = i + 1;
        }

        if (result == null) {
            return name;
        }

        if (last < length) {
            result.append(name, last, length);
        }
        return result.toString();
    }

    public static boolean canExecute( File file) {
        return file.canExecute();
    }

    public static void setReadOnlyAttribute( String path, boolean readOnlyFlag) {
        final boolean writableFlag = !readOnlyFlag;
        final File file = new File(path);
        if (!file.setWritable(writableFlag) && file.canWrite() != writableFlag) {
            LOG.warn("Can't set writable attribute of '" + path + "' to " + readOnlyFlag);
        }
    }

    public static void appendToFile( File file,  String text) throws IOException {
        writeToFile(file, text.getBytes(CharsetToolkit.UTF8_CHARSET), true);
    }

    public static void writeToFile( File file,  byte[] text) throws IOException {
        writeToFile(file, text, false);
    }

    public static void writeToFile( File file,  String text) throws IOException {
        writeToFile(file, text.getBytes(CharsetToolkit.UTF8_CHARSET), false);
    }

    public static void writeToFile( File file,  byte[] text, int off, int len) throws IOException {
        writeToFile(file, text, off, len, false);
    }

    public static void writeToFile( File file,  byte[] text, boolean append) throws IOException {
        writeToFile(file, text, 0, text.length, append);
    }

    private static void writeToFile( File file,  byte[] text, int off, int len, boolean append) throws IOException {
        createParentDirs(file);

        OutputStream stream = new FileOutputStream(file, append);
        try {
            stream.write(text, off, len);
        }
        finally {
            stream.close();
        }
    }

    public static boolean processFilesRecursively( File root,  Processor<File> processor) {
        return processFilesRecursively(root, processor, null);
    }

    public static boolean processFilesRecursively( File root,  Processor<File> processor,
                                                   final Processor<File> directoryFilter) {
        final LinkedList<File> queue = new LinkedList<File>();
        queue.add(root);
        while (!queue.isEmpty()) {
            final File file = queue.removeFirst();
            if (!processor.process(file)) return false;
            if (directoryFilter != null && (!file.isDirectory() || !directoryFilter.process(file))) continue;

            final File[] children = file.listFiles();
            if (children != null) {
                ContainerUtil.addAll(queue, children);
            }
        }
        return true;
    }

    
    public static File findFirstThatExist( String... paths) {
        for (String path : paths) {
            if (!StringUtil.isEmptyOrSpaces(path)) {
                File file = new File(toSystemDependentName(path));
                if (file.exists()) return file;
            }
        }

        return null;
    }

    
    public static List<File> findFilesByMask( Pattern pattern,  File dir) {
        final ArrayList<File> found = new ArrayList<File>();
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    found.addAll(findFilesByMask(pattern, file));
                }
                else if (pattern.matcher(file.getName()).matches()) {
                    found.add(file);
                }
            }
        }
        return found;
    }

    
    public static List<File> findFilesOrDirsByMask( Pattern pattern,  File dir) {
        final ArrayList<File> found = new ArrayList<File>();
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (pattern.matcher(file.getName()).matches()) {
                    found.add(file);
                }
                if (file.isDirectory()) {
                    found.addAll(findFilesOrDirsByMask(pattern, file));
                }
            }
        }
        return found;
    }

    /**
     * Returns empty string for empty path.
     * First checks whether provided path is a path of a file with sought-for name.
     * Unless found, checks if provided file was a directory. In this case checks existence
     * of child files with given names in order "as provided". Finally checks filename among
     * brother-files of provided. Returns null if nothing found.
     *
     * @return path of the first of found files or empty string or null.
     */
    
    public static String findFileInProvidedPath(String providedPath, String... fileNames) {
        if (StringUtil.isEmpty(providedPath)) {
            return "";
        }

        File providedFile = new File(providedPath);
        if (providedFile.exists()) {
            String name = providedFile.getName();
            for (String fileName : fileNames) {
                if (name.equals(fileName)) {
                    return toSystemDependentName(providedFile.getPath());
                }
            }
        }

        if (providedFile.isDirectory()) {  //user chose folder with file
            for (String fileName : fileNames) {
                File file = new File(providedFile, fileName);
                if (fileName.equals(file.getName()) && file.exists()) {
                    return toSystemDependentName(file.getPath());
                }
            }
        }

        providedFile = providedFile.getParentFile();  //users chose wrong file in same directory
        if (providedFile != null && providedFile.exists()) {
            for (String fileName : fileNames) {
                File file = new File(providedFile, fileName);
                if (fileName.equals(file.getName()) && file.exists()) {
                    return toSystemDependentName(file.getPath());
                }
            }
        }

        return null;
    }

    public static boolean isAbsolutePlatformIndependent( String path) {
        return isUnixAbsolutePath(path) || isWindowsAbsolutePath(path);
    }

    public static boolean isUnixAbsolutePath( String path) {
        return path.startsWith("/");
    }

    public static boolean isWindowsAbsolutePath( String pathString) {
        return pathString.length() >= 2 && Character.isLetter(pathString.charAt(0)) && pathString.charAt(1) == ':';
    }

    public static String getLocationRelativeToUserHome( String path) {
        return getLocationRelativeToUserHome(path, true);
    }

    public static String getLocationRelativeToUserHome( String path, boolean unixOnly) {
        if (path == null) return null;

        if (SystemInfo.isUnix || !unixOnly) {
            File projectDir = new File(path);
            File userHomeDir = new File(SystemProperties.getUserHome());
            if (isAncestor(userHomeDir, projectDir, true)) {
                return '~' + File.separator + getRelativePath(userHomeDir, projectDir);
            }
        }

        return path;
    }

    
    public static String expandUserHome( String path) {
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            path = SystemProperties.getUserHome() + path.substring(1);
        }
        return path;
    }

    
    public static File[] notNullize( File[] files) {
        return notNullize(files, ArrayUtil.EMPTY_FILE_ARRAY);
    }

    
    public static File[] notNullize( File[] files,  File[] defaultFiles) {
        return files == null ? defaultFiles : files;
    }

    public static boolean isHashBangLine(CharSequence firstCharsIfText, String marker) {
        if (firstCharsIfText == null) {
            return false;
        }
        final int lineBreak = StringUtil.indexOf(firstCharsIfText, '\n');
        if (lineBreak < 0) {
            return false;
        }
        String firstLine = firstCharsIfText.subSequence(0, lineBreak).toString();
        if (!firstLine.startsWith("#!")) {
            return false;
        }
        return firstLine.contains(marker);
    }

    
    public static File createTempDirectory(  String prefix,   String suffix) throws IOException {
        return FileUtilRt.createTempDirectory(prefix, suffix);
    }

    
    public static File createTempDirectory(  String prefix,   String suffix, boolean deleteOnExit)
            throws IOException {
        return FileUtilRt.createTempDirectory(prefix, suffix, deleteOnExit);
    }

    
    public static File createTempDirectory( File dir,   String prefix,   String suffix)
            throws IOException {
        return FileUtilRt.createTempDirectory(dir, prefix, suffix);
    }

    
    public static File createTempDirectory( File dir,
                                             String prefix,
                                             String suffix,
                                           boolean deleteOnExit) throws IOException {
        return FileUtilRt.createTempDirectory(dir, prefix, suffix, deleteOnExit);
    }

    
    public static File createTempFile(  String prefix,   String suffix) throws IOException {
        return FileUtilRt.createTempFile(prefix, suffix);
    }

    
    public static File createTempFile(  String prefix,   String suffix, boolean deleteOnExit)
            throws IOException {
        return FileUtilRt.createTempFile(prefix, suffix, deleteOnExit);
    }

    
    public static File createTempFile( File dir,   String prefix,   String suffix) throws IOException {
        return FileUtilRt.createTempFile(dir, prefix, suffix);
    }

    
    public static File createTempFile( File dir,   String prefix,   String suffix, boolean create)
            throws IOException {
        return FileUtilRt.createTempFile(dir, prefix, suffix, create);
    }

    
    public static File createTempFile( File dir,
                                        String prefix,
                                        String suffix,
                                      boolean create,
                                      boolean deleteOnExit) throws IOException {
        return FileUtilRt.createTempFile(dir, prefix, suffix, create, deleteOnExit);
    }

    
    public static String getTempDirectory() {
        return FileUtilRt.getTempDirectory();
    }

    public static void resetCanonicalTempPathCache(final String tempPath) {
        FileUtilRt.resetCanonicalTempPathCache(tempPath);
    }

    
    public static File generateRandomTemporaryPath() throws IOException {
        return FileUtilRt.generateRandomTemporaryPath();
    }

    public static void setExecutableAttribute( String path, boolean executableFlag) throws IOException {
        FileUtilRt.setExecutableAttribute(path, executableFlag);
    }

    public static void setLastModified( File file, long timeStamp) throws IOException {
        if (!file.setLastModified(timeStamp)) {
            LOG.warn(file.getPath());
        }
    }

    
    public static String loadFile( File file) throws IOException {
        return FileUtilRt.loadFile(file);
    }

    
    public static String loadFile( File file, boolean convertLineSeparators) throws IOException {
        return FileUtilRt.loadFile(file, convertLineSeparators);
    }

    
    public static String loadFile( File file,   String encoding) throws IOException {
        return FileUtilRt.loadFile(file, encoding);
    }
    
    public static String loadFile( File file,   Charset encoding) throws IOException {
        return String.valueOf(FileUtilRt.loadFileText(file, encoding));
    }

    
    public static String loadFile( File file,   String encoding, boolean convertLineSeparators) throws IOException {
        return FileUtilRt.loadFile(file, encoding, convertLineSeparators);
    }

    
    public static char[] loadFileText( File file) throws IOException {
        return FileUtilRt.loadFileText(file);
    }

    
    public static char[] loadFileText( File file,   String encoding) throws IOException {
        return FileUtilRt.loadFileText(file, encoding);
    }

    
    public static char[] loadText( Reader reader, int length) throws IOException {
        return FileUtilRt.loadText(reader, length);
    }

    
    public static List<String> loadLines( File file) throws IOException {
        return FileUtilRt.loadLines(file);
    }

    
    public static List<String> loadLines( File file,   String encoding) throws IOException {
        return FileUtilRt.loadLines(file, encoding);
    }

    
    public static List<String> loadLines( String path) throws IOException {
        return FileUtilRt.loadLines(path);
    }

    
    public static List<String> loadLines( String path,   String encoding) throws IOException {
        return FileUtilRt.loadLines(path, encoding);
    }

    
    public static List<String> loadLines( BufferedReader reader) throws IOException {
        return FileUtilRt.loadLines(reader);
    }

    /** @deprecated unclear closing policy, do not use (to remove in IDEA 14) */
    @SuppressWarnings({"UnusedDeclaration", "deprecation"})
    public static List<String> loadLines( InputStream stream) throws IOException {
        return loadLines(new InputStreamReader(stream));
    }

    /** @deprecated unclear closing policy, do not use (to remove in IDEA 14) */
    @SuppressWarnings("UnusedDeclaration")
    public static List<String> loadLines( Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            return loadLines(bufferedReader);
        }
        finally {
            bufferedReader.close();
        }
    }

    
    public static byte[] loadBytes( InputStream stream) throws IOException {
        return FileUtilRt.loadBytes(stream);
    }

    
    public static byte[] loadBytes( InputStream stream, int length) throws IOException {
        return FileUtilRt.loadBytes(stream, length);
    }

    
    public static List<String> splitPath( String path) {
        ArrayList<String> list = new ArrayList<String>();
        int index = 0;
        int nextSeparator;
        while ((nextSeparator = path.indexOf(File.separatorChar, index)) != -1) {
            list.add(path.substring(index, nextSeparator));
            index = nextSeparator + 1;
        }
        list.add(path.substring(index, path.length()));
        return list;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public static boolean isJarOrZip(File file) {
        if (file.isDirectory()) {
            return false;
        }
        final String name = file.getName();
        return StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip");
    }

    public static boolean visitFiles( File root,  Processor<File> processor) {
        if (!processor.process(root)) {
            return false;
        }

        File[] children = root.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!visitFiles(child, processor)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Like {@link Properties#load(Reader)}, but preserves the order of key/value pairs.
     */
    
    public static Map<String, String> loadProperties( Reader reader) throws IOException {
        final Map<String, String> map = ContainerUtil.newLinkedHashMap();

        new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                map.put(String.valueOf(key), String.valueOf(value));
                //noinspection UseOfPropertiesAsHashtable
                return super.put(key, value);
            }
        }.load(reader);

        return map;
    }

    public static boolean isRootPath( File file) {
        return isRootPath(file.getPath());
    }

    public static boolean isRootPath( String path) {
        return path.equals("/") || path.matches("[a-zA-Z]:[/\\\\]");
    }

    public static boolean deleteWithRenaming(File file) {
        File tempFileNameForDeletion = findSequentNonexistentFile(file.getParentFile(), file.getName(), "");
        boolean success = file.renameTo(tempFileNameForDeletion);
        return delete(success ? tempFileNameForDeletion:file);
    }

    public static boolean isFileSystemCaseSensitive( String path) throws FileNotFoundException {
        FileAttributes attributes = FileSystemUtil.getAttributes(path);
        if (attributes == null) {
            throw new FileNotFoundException(path);
        }

        FileAttributes upper = FileSystemUtil.getAttributes(path.toUpperCase(Locale.ENGLISH));
        FileAttributes lower = FileSystemUtil.getAttributes(path.toLowerCase(Locale.ENGLISH));
        return !(attributes.equals(upper) && attributes.equals(lower));
    }
}
