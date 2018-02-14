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

import com.gome.maven.diagnostic.LoggerRt;
import com.gome.maven.openapi.util.SystemInfoRt;
import com.gome.maven.openapi.util.text.StringUtilRt;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stripped-down version of {@code com.gome.maven.openapi.util.io.FileUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
public class FileUtilRt {
    private static final int KILOBYTE = 1024;
    public static final int MEGABYTE = KILOBYTE * KILOBYTE;
    public static final int LARGE_FOR_CONTENT_LOADING = Math.max(20 * MEGABYTE, getUserFileSizeLimit());

    private static final int MAX_FILE_IO_ATTEMPTS = 10;
    private static final boolean USE_FILE_CHANNELS = "true".equalsIgnoreCase(System.getProperty("idea.fs.useChannels"));

    public static final FileFilter ALL_FILES = new FileFilter() {
        public boolean accept(File file) {
            return true;
        }
    };
    public static final FileFilter ALL_DIRECTORIES = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    protected static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[1024 * 20];
        }
    };

    private static String ourCanonicalTempPathCache = null;

    protected static final class NIOReflect {
        // NIO-reflection initialization placed in a separate class for lazy loading
        static final boolean IS_AVAILABLE;

        // todo: replace reflection with normal code after migration to JDK 1.8
        private static Method ourFilesDeleteIfExistsMethod;
        private static Method ourFilesWalkMethod;
        private static Method ourFileToPathMethod;
        private static Method ourPathToFileMethod;
        private static Object ourDeletionVisitor;
        private static Class ourNoSuchFileExceptionClass;
        private static Class ourAccessDeniedExceptionClass;

        static {
            boolean initSuccess = false;
            try {
                final Class<?> pathClass = Class.forName("java.nio.file.Path");
                final Class<?> visitorClass = Class.forName("java.nio.file.FileVisitor");
                final Class<?> filesClass = Class.forName("java.nio.file.Files");
                ourNoSuchFileExceptionClass = Class.forName("java.nio.file.NoSuchFileException");
                ourAccessDeniedExceptionClass = Class.forName("java.nio.file.AccessDeniedException");

                ourFileToPathMethod = Class.forName("java.io.File").getMethod("toPath");
                ourPathToFileMethod = pathClass.getMethod("toFile");
                ourFilesWalkMethod = filesClass.getMethod("walkFileTree", pathClass, visitorClass);
                ourFilesDeleteIfExistsMethod = filesClass.getMethod("deleteIfExists", pathClass);
                final Class<?> fileVisitResultClass = Class.forName("java.nio.file.FileVisitResult");
                final Object Result_Continue = fileVisitResultClass.getDeclaredField("CONTINUE").get(null);
                final Object Result_Terminate = fileVisitResultClass.getDeclaredField("TERMINATE").get(null);
                ourDeletionVisitor = Proxy.newProxyInstance(FileUtilRt.class.getClassLoader(), new Class[]{visitorClass}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (args.length == 2) {
                            final Object second = args[1];
                            if (second instanceof Throwable) {
                                throw (Throwable)second;
                            }
                            final String methodName = method.getName();
                            if ("visitFile".equals(methodName) || "postVisitDirectory".equals(methodName)) {
                                if (!performDelete(args[0])) {
                                    return Result_Terminate;
                                }
                            }
                        }
                        return Result_Continue;
                    }

                    private boolean performDelete( final Object fileObject) {
                        Boolean result = doIOOperation(new RepeatableIOOperation<Boolean, RuntimeException>() {
                            public Boolean execute(boolean lastAttempt) {
                                try {
                                    //Files.deleteIfExists(file);
                                    ourFilesDeleteIfExistsMethod.invoke(null, fileObject);
                                    return Boolean.TRUE;
                                }
                                catch (InvocationTargetException e) {
                                    final Throwable cause = e.getCause();
                                    if (!(cause instanceof IOException)) {
                                        return Boolean.FALSE;
                                    }
                                    if (ourAccessDeniedExceptionClass.isInstance(cause)) {
                                        // file is read-only: fallback to standard java.io API
                                        try {
                                            final File file = (File)ourPathToFileMethod.invoke(fileObject);
                                            if (file == null) {
                                                return Boolean.FALSE;
                                            }
                                            if (file.delete() || !file.exists()) {
                                                return Boolean.TRUE;
                                            }
                                        }
                                        catch (Throwable ignored) {
                                            return Boolean.FALSE;
                                        }
                                    }
                                }
                                catch (IllegalAccessException e) {
                                    return Boolean.FALSE;
                                }
                                return lastAttempt ? Boolean.FALSE : null;
                            }
                        });
                        return Boolean.TRUE.equals(result);
                    }
                });
                initSuccess = true;
            }
            catch (Throwable ignored) {
                logger().info("Was not able to detect NIO API");
                ourFileToPathMethod = null;
                ourFilesWalkMethod = null;
                ourFilesDeleteIfExistsMethod = null;
                ourDeletionVisitor = null;
                ourNoSuchFileExceptionClass = null;
            }
            IS_AVAILABLE = initSuccess;
        }
    }

    
    public static String getExtension( String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) return "";
        return fileName.substring(index + 1);
    }

    
    public static CharSequence getExtension( CharSequence fileName) {
        int index = StringUtilRt.lastIndexOf(fileName, '.', 0, fileName.length());
        if (index < 0) return "";
        return fileName.subSequence(index + 1, fileName.length());
    }

    public static boolean extensionEquals( String fileName,  String extension) {
        int extLen = extension.length();
        if (extLen == 0) {
            return fileName.indexOf('.') == -1;
        }
        int extStart = fileName.length() - extLen;
        return extStart >= 1 && fileName.charAt(extStart-1) == '.'
                && fileName.regionMatches(!SystemInfoRt.isFileSystemCaseSensitive, extStart, extension, 0, extLen);
    }

    
    public static String toSystemDependentName(  String fileName) {
        return toSystemDependentName(fileName, File.separatorChar);
    }

    
    public static String toSystemDependentName(  String fileName, final char separatorChar) {
        return fileName.replace('/', separatorChar).replace('\\', separatorChar);
    }

    
    public static String toSystemIndependentName(  String fileName) {
        return fileName.replace('\\', '/');
    }

   
    public static String getRelativePath(File base, File file) {
        if (base == null || file == null) return null;

        if (!base.isDirectory()) {
            base = base.getParentFile();
            if (base == null) return null;
        }

        //noinspection FileEqualsUsage
        if (base.equals(file)) return ".";

        final String filePath = file.getAbsolutePath();
        String basePath = base.getAbsolutePath();
        return getRelativePath(basePath, filePath, File.separatorChar);
    }

   
    public static String getRelativePath( String basePath,  String filePath, char separator) {
        return getRelativePath(basePath, filePath, separator, SystemInfoRt.isFileSystemCaseSensitive);
    }

   
    public static String getRelativePath( String basePath,  String filePath, char separator, boolean caseSensitive) {
        basePath = ensureEnds(basePath, separator);

        if (caseSensitive ? basePath.equals(ensureEnds(filePath, separator)) : basePath.equalsIgnoreCase(ensureEnds(filePath, separator))) {
            return ".";
        }

        int len = 0;
        int lastSeparatorIndex = 0; // need this for cases like this: base="/temp/abc/base" and file="/temp/ab"
        CharComparingStrategy strategy = caseSensitive ? CharComparingStrategy.IDENTITY : CharComparingStrategy.CASE_INSENSITIVE;
        while (len < filePath.length() && len < basePath.length() && strategy.charsEqual(filePath.charAt(len), basePath.charAt(len))) {
            if (basePath.charAt(len) == separator) {
                lastSeparatorIndex = len;
            }
            len++;
        }

        if (len == 0) return null;

        StringBuilder relativePath = new StringBuilder();
        for (int i = len; i < basePath.length(); i++) {
            if (basePath.charAt(i) == separator) {
                relativePath.append("..");
                relativePath.append(separator);
            }
        }
        relativePath.append(filePath.substring(lastSeparatorIndex + 1));

        return relativePath.toString();
    }

    private static String ensureEnds( String s, final char endsWith) {
        return StringUtilRt.endsWithChar(s, endsWith) ? s : s + endsWith;
    }

    
    public static String getNameWithoutExtension( String name) {
        int i = name.lastIndexOf('.');
        if (i != -1) {
            name = name.substring(0, i);
        }
        return name;
    }

    
    public static File createTempDirectory(  String prefix,  String suffix) throws IOException {
        return createTempDirectory(prefix, suffix, true);
    }

    
    public static File createTempDirectory(  String prefix,  String suffix, boolean deleteOnExit) throws IOException {
        final File dir = new File(getTempDirectory());
        return createTempDirectory(dir, prefix, suffix, deleteOnExit);
    }

    
    public static File createTempDirectory( File dir,
                                             String prefix,  String suffix) throws IOException {
        return createTempDirectory(dir, prefix, suffix, true);
    }

    
    public static File createTempDirectory( File dir,
                                             String prefix,  String suffix,
                                           boolean deleteOnExit) throws IOException {
        File file = doCreateTempFile(dir, prefix, suffix, true);
        if (deleteOnExit) {
            file.deleteOnExit();
        }
        if (!file.isDirectory()) {
            throw new IOException("Cannot create directory: " + file);
        }
        return file;
    }

    
    public static File createTempFile(  String prefix,  String suffix) throws IOException {
        return createTempFile(prefix, suffix, false); //false until TeamCity fixes its plugin
    }

    
    public static File createTempFile(  String prefix,  String suffix,
                                      boolean deleteOnExit) throws IOException {
        final File dir = new File(getTempDirectory());
        return createTempFile(dir, prefix, suffix, true, deleteOnExit);
    }

    
    public static File createTempFile( File dir,
                                        String prefix,  String suffix) throws IOException {
        return createTempFile(dir, prefix, suffix, true, true);
    }

    
    public static File createTempFile( File dir,
                                        String prefix,  String suffix,
                                      boolean create) throws IOException {
        return createTempFile(dir, prefix, suffix, create, true);
    }

    
    public static File createTempFile( File dir,
                                        String prefix,  String suffix,
                                      boolean create, boolean deleteOnExit) throws IOException {
        File file = doCreateTempFile(dir, prefix, suffix, false);
        if (deleteOnExit) {
            file.deleteOnExit();
        }
        if (!create) {
            if (!file.delete() && file.exists()) {
                throw new IOException("Cannot delete file: " + file);
            }
        }
        return file;
    }

    
    private static File doCreateTempFile( File dir,
                                           String prefix,
                                          String suffix,
                                         boolean isDirectory) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        if (prefix.length() < 3) {
            prefix = (prefix + "___").substring(0, 3);
        }
        if (suffix == null) {
            suffix = ".tmp";
        }

        int exceptionsCount = 0;
        while (true) {
            try {
                final File temp = createTemp(prefix, suffix, dir, isDirectory);
                return normalizeFile(temp);
            }
            catch (IOException e) { // Win32 createFileExclusively access denied
                if (++exceptionsCount >= 100) {
                    throw e;
                }
            }
        }
    }

    
    private static File createTemp( String prefix,  String suffix,  File directory, boolean isDirectory) throws IOException {
        // normalize and use only the file name from the prefix
        prefix = new File(prefix).getName();

        File f;
        int i = 0;
        do {
            String name = prefix + i + suffix;
            f = new File(directory, name);
            if (!name.equals(f.getName())) {
                throw new IOException("Unable to create temporary file " + f + " for name " + name);
            }
            i++;
        }
        while (f.exists());

        boolean success = isDirectory ? f.mkdir() : f.createNewFile();
        if (!success) {
            throw new IOException("Unable to create temporary file " + f);
        }

        return f;
    }

    
    private static File normalizeFile( File temp) throws IOException {
        final File canonical = temp.getCanonicalFile();
        return SystemInfoRt.isWindows && canonical.getAbsolutePath().contains(" ") ? temp.getAbsoluteFile() : canonical;
    }

    
    public static String getTempDirectory() {
        if (ourCanonicalTempPathCache == null) {
            ourCanonicalTempPathCache = calcCanonicalTempPath();
        }
        return ourCanonicalTempPathCache;
    }

    
    private static String calcCanonicalTempPath() {
        final File file = new File(System.getProperty("java.io.tmpdir"));
        try {
            final String canonical = file.getCanonicalPath();
            if (!SystemInfoRt.isWindows || !canonical.contains(" ")) {
                return canonical;
            }
        }
        catch (IOException ignore) { }
        return file.getAbsolutePath();
    }

   
    public static void resetCanonicalTempPathCache(final String tempPath) {
        ourCanonicalTempPathCache = tempPath;
    }

    
    public static File generateRandomTemporaryPath() throws IOException {
        File file = new File(getTempDirectory(), UUID.randomUUID().toString());
        int i = 0;
        while (file.exists() && i < 5) {
            file = new File(getTempDirectory(), UUID.randomUUID().toString());
            ++i;
        }
        if (file.exists()) {
            throw new IOException("Couldn't generate unique random path.");
        }
        return normalizeFile(file);
    }

    /**
     * Set executable attribute, it makes sense only on non-windows platforms.
     *
     * @param path           the path to use
     * @param executableFlag new value of executable attribute
     * @throws IOException if there is a problem with setting the flag
     */
    public static void setExecutableAttribute( String path, boolean executableFlag) throws IOException {
        final File file = new File(path);
        if (!file.setExecutable(executableFlag) && file.canExecute() != executableFlag) {
            logger().warn("Can't set executable attribute of '" + path + "' to " + executableFlag);
        }
    }

    
    public static String loadFile( File file) throws IOException {
        return loadFile(file, null, false);
    }

    
    public static String loadFile( File file, boolean convertLineSeparators) throws IOException {
        return loadFile(file, null, convertLineSeparators);
    }

    
    public static String loadFile( File file,  String encoding) throws IOException {
        return loadFile(file, encoding, false);
    }

    
    public static String loadFile( File file,  String encoding, boolean convertLineSeparators) throws IOException {
        final String s = new String(loadFileText(file, encoding));
        return convertLineSeparators ? StringUtilRt.convertLineSeparators(s) : s;
    }

    
    public static char[] loadFileText( File file) throws IOException {
        return loadFileText(file, (String)null);
    }

    
    public static char[] loadFileText( File file,  String encoding) throws IOException {
        InputStream stream = new FileInputStream(file);
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        Reader reader = encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding);
        try {
            return loadText(reader, (int)file.length());
        }
        finally {
            reader.close();
        }
    }
    
    public static char[] loadFileText( File file,   Charset encoding) throws IOException {
        Reader reader = new InputStreamReader(new FileInputStream(file), encoding);
        try {
            return loadText(reader, (int)file.length());
        }
        finally {
            reader.close();
        }
    }

    
    public static char[] loadText( Reader reader, int length) throws IOException {
        char[] chars = new char[length];
        int count = 0;
        while (count < chars.length) {
            int n = reader.read(chars, count, chars.length - count);
            if (n <= 0) break;
            count += n;
        }
        if (count == chars.length) {
            return chars;
        }
        else {
            char[] newChars = new char[count];
            System.arraycopy(chars, 0, newChars, 0, count);
            return newChars;
        }
    }

    
    public static List<String> loadLines( File file) throws IOException {
        return loadLines(file.getPath());
    }

    
    public static List<String> loadLines( File file,  String encoding) throws IOException {
        return loadLines(file.getPath(), encoding);
    }

    
    public static List<String> loadLines( String path) throws IOException {
        return loadLines(path, null);
    }

    
    public static List<String> loadLines( String path,  String encoding) throws IOException {
        InputStream stream = new FileInputStream(path);
        try {
            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
            InputStreamReader in = encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding);
            BufferedReader reader = new BufferedReader(in);
            try {
                return loadLines(reader);
            }
            finally {
                reader.close();
            }
        }
        finally {
            stream.close();
        }
    }

    
    public static List<String> loadLines( BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    
    public static byte[] loadBytes( InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] bytes = BUFFER.get();
        while (true) {
            int n = stream.read(bytes, 0, bytes.length);
            if (n <= 0) break;
            buffer.write(bytes, 0, n);
        }
        buffer.close();
        return buffer.toByteArray();
    }

    public static boolean isTooLarge(long len) {
        return len > LARGE_FOR_CONTENT_LOADING;
    }

    
    public static byte[] loadBytes( InputStream stream, int length) throws IOException {
        byte[] bytes = new byte[length];
        int count = 0;
        while (count < length) {
            int n = stream.read(bytes, count, length - count);
            if (n <= 0) break;
            count += n;
        }
        return bytes;
    }

    /**
     * Get parent for the file. The method correctly
     * processes "." and ".." in file names. The name
     * remains relative if was relative before.
     *
     * @param file a file to analyze
     * @return a parent or the null if the file has no parent.
     */
   
    public static File getParentFile( File file) {
        int skipCount = 0;
        File parentFile = file;
        while (true) {
            parentFile = parentFile.getParentFile();
            if (parentFile == null) {
                return null;
            }
            if (".".equals(parentFile.getName())) {
                continue;
            }
            if ("..".equals(parentFile.getName())) {
                skipCount++;
                continue;
            }
            if (skipCount > 0) {
                skipCount--;
                continue;
            }
            return parentFile;
        }
    }

    /**
     * Warning! this method is _not_ symlinks-aware. Consider using com.gome.maven.openapi.util.io.FileUtil.delete()
     * @param file file or directory to delete
     * @return true if the file did not exist or was successfully deleted
     */
    public static boolean delete( File file) {
        if (NIOReflect.IS_AVAILABLE) {
            return deleteRecursivelyNIO(file);
        }
        return deleteRecursively(file);
    }

    protected static boolean deleteRecursivelyNIO(File file) {
        try {
      /*
      Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.deleteIfExists(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.deleteIfExists(dir);
          return FileVisitResult.CONTINUE;
        }
      });
      */
            final Object pathObject = NIOReflect.ourFileToPathMethod.invoke(file);
            NIOReflect.ourFilesWalkMethod.invoke(null, pathObject, NIOReflect.ourDeletionVisitor);
        }
        catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause == null || !NIOReflect.ourNoSuchFileExceptionClass.isInstance(cause)) {
                logger().info(e);
                return false;
            }
        }
        catch (Exception e) {
            logger().info(e);
            return false;
        }
        return true;
    }

    private static boolean deleteRecursively( File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                if (!deleteRecursively(child)) return false;
            }
        }

        return deleteFile(file);
    }

    public interface RepeatableIOOperation<T, E extends Throwable> {
        T execute(boolean lastAttempt) throws E;
    }

   
    public static <T, E extends Throwable> T doIOOperation( RepeatableIOOperation<T, E> ioTask) throws E {
        for (int i = MAX_FILE_IO_ATTEMPTS; i > 0; i--) {
            T result = ioTask.execute(i == 1);
            if (result != null) return result;

            try {
                //noinspection BusyWait
                Thread.sleep(10);
            }
            catch (InterruptedException ignored) { }
        }
        return null;
    }

    protected static boolean deleteFile( final File file) {
        Boolean result = doIOOperation(new RepeatableIOOperation<Boolean, RuntimeException>() {
            public Boolean execute(boolean lastAttempt) {
                if (file.delete() || !file.exists()) return Boolean.TRUE;
                else if (lastAttempt) return Boolean.FALSE;
                else return null;
            }
        });
        return Boolean.TRUE.equals(result);
    }

    public static boolean ensureCanCreateFile( File file) {
        if (file.exists()) return file.canWrite();
        if (!createIfNotExists(file)) return false;
        return delete(file);
    }

    public static boolean createIfNotExists( File file) {
        if (file.exists()) return true;
        try {
            if (!createParentDirs(file)) return false;

            OutputStream s = new FileOutputStream(file);
            s.close();
            return true;
        }
        catch (IOException e) {
            logger().info(e);
            return false;
        }
    }

    public static boolean createParentDirs( File file) {
        if (!file.exists()) {
            final File parentFile = file.getParentFile();
            if (parentFile != null) {
                return createDirectory(parentFile);
            }
        }
        return true;
    }

    public static boolean createDirectory( File path) {
        return path.isDirectory() || path.mkdirs();
    }

    public static void copy( File fromFile,  File toFile) throws IOException {
        if (!ensureCanCreateFile(toFile)) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(toFile);
        try {
            FileInputStream fis = new FileInputStream(fromFile);
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

        long timeStamp = fromFile.lastModified();
        if (timeStamp < 0) {
            logger().warn("Invalid timestamp " + timeStamp + " of '" + fromFile + "'");
        }
        else if (!toFile.setLastModified(timeStamp)) {
            logger().warn("Unable to set timestamp " + timeStamp + " to '" + toFile + "'");
        }
    }

    public static void copy( InputStream inputStream,  OutputStream outputStream) throws IOException {
        if (USE_FILE_CHANNELS && inputStream instanceof FileInputStream && outputStream instanceof FileOutputStream) {
            final FileChannel fromChannel = ((FileInputStream)inputStream).getChannel();
            try {
                final FileChannel toChannel = ((FileOutputStream)outputStream).getChannel();
                try {
                    fromChannel.transferTo(0, Long.MAX_VALUE, toChannel);
                }
                finally {
                    toChannel.close();
                }
            }
            finally {
                fromChannel.close();
            }
        }
        else {
            final byte[] buffer = BUFFER.get();
            while (true) {
                int read = inputStream.read(buffer);
                if (read < 0) break;
                outputStream.write(buffer, 0, read);
            }
        }
    }

    public static int getUserFileSizeLimit() {
        try {
            return Integer.parseInt(System.getProperty("idea.max.intellisense.filesize")) * KILOBYTE;
        }
        catch (NumberFormatException e) {
            return 2500 * KILOBYTE;
        }
    }

    private interface CharComparingStrategy {
        CharComparingStrategy IDENTITY = new CharComparingStrategy() {
            @Override
            public boolean charsEqual(char ch1, char ch2) {
                return ch1 == ch2;
            }
        };
        CharComparingStrategy CASE_INSENSITIVE = new CharComparingStrategy() {
            @Override
            public boolean charsEqual(char ch1, char ch2) {
                return StringUtilRt.charsEqualIgnoreCase(ch1, ch2);
            }
        };

        boolean charsEqual(char ch1, char ch2);
    }

    private static LoggerRt logger() {
        return LoggerRt.getInstance("#com.gome.maven.openapi.util.io.FileUtilRt");
    }
}
