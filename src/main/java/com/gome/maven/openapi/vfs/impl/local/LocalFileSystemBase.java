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
package com.gome.maven.openapi.vfs.impl.local;

import com.gome.maven.ide.GeneralSettings;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileAttributes;
import com.gome.maven.openapi.util.io.FileSystemUtil;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.ex.VirtualFileManagerEx;
import com.gome.maven.openapi.vfs.newvfs.ManagingFS;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.openapi.vfs.newvfs.RefreshQueue;
import com.gome.maven.openapi.vfs.newvfs.VfsImplUtil;
import com.gome.maven.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Processor;
import com.gome.maven.util.ThrowableConsumer;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.io.SafeFileOutputStream;
import com.gome.maven.util.io.fs.IFile;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Dmitry Avdeev
 */
public abstract class LocalFileSystemBase extends LocalFileSystem {
    protected static final Logger LOG = Logger.getInstance(LocalFileSystemBase.class);

    private static final FileAttributes FAKE_ROOT_ATTRIBUTES =
            new FileAttributes(true, false, false, false, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, false);

    private final List<LocalFileOperationsHandler> myHandlers = new ArrayList<LocalFileOperationsHandler>();

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
    public VirtualFile findFileByIoFile( File file) {
        String path = file.getAbsolutePath();
        return findFileByPath(path.replace(File.separatorChar, '/'));
    }

    @Override
    
    public VirtualFile findFileByIoFile( final IFile file) {
        String path = file.getPath();
        if (path == null) return null;
        return findFileByPath(path.replace(File.separatorChar, '/'));
    }

    
    protected static File convertToIOFile( final VirtualFile file) {
        String path = file.getPath();
        if (StringUtil.endsWithChar(path, ':') && path.length() == 2 && (SystemInfo.isWindows || SystemInfo.isOS2)) {
            path += "/"; // Make 'c:' resolve to a root directory for drive c:, not the current directory on that drive
        }

        return new File(path);
    }

    
    private static File convertToIOFileAndCheck( final VirtualFile file) throws FileNotFoundException {
        final File ioFile = convertToIOFile(file);

        final FileAttributes attributes = FileSystemUtil.getAttributes(ioFile);
        if (attributes != null && !attributes.isFile()) {
            LOG.warn("not a file: " + ioFile + ", " + attributes);
            throw new FileNotFoundException("Not a file: " + ioFile);
        }

        return ioFile;
    }

    @Override
    public boolean exists( final VirtualFile file) {
        return getAttributes(file) != null;
    }

    @Override
    public long getLength( final VirtualFile file) {
        final FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.length : DEFAULT_LENGTH;
    }

    @Override
    public long getTimeStamp( final VirtualFile file) {
        final FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP;
    }

    @Override
    public boolean isDirectory( final VirtualFile file) {
        final FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isDirectory();
    }

    @Override
    public boolean isWritable( final VirtualFile file) {
        final FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isWritable();
    }

    @Override
    public boolean isSymLink( final VirtualFile file) {
        final FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isSymLink();
    }

    @Override
    public String resolveSymLink( VirtualFile file) {
        return FileSystemUtil.resolveSymLink(file.getPath());
    }

    @Override
    public boolean isSpecialFile( final VirtualFile file) {
        final FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isSpecial();
    }

    @Override
    
    public String[] list( final VirtualFile file) {
        if (file.getParent() == null) {
            final File[] roots = File.listRoots();
            if (roots.length == 1 && roots[0].getName().isEmpty()) {
                final String[] list = roots[0].list();
                if (list != null) return list;
                LOG.warn("Root '" + roots[0] + "' has no children - is it readable?");
                return ArrayUtil.EMPTY_STRING_ARRAY;
            }
            if (file.getName().isEmpty()) {
                // return drive letter names for the 'fake' root on windows
                final String[] names = new String[roots.length];
                for (int i = 0; i < names.length; i++) {
                    String name = roots[i].getPath();
                    if (name.endsWith(File.separator)) {
                        name = name.substring(0, name.length() - File.separator.length());
                    }
                    names[i] = name;
                }
                return names;
            }
        }

        final String[] names = convertToIOFile(file).list();
        return names == null ? ArrayUtil.EMPTY_STRING_ARRAY : names;
    }

    @Override
    
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    
    protected String normalize( String path) {
        if (path.isEmpty()) {
            try {
                path = new File("").getCanonicalPath();
            }
            catch (IOException e) {
                return path;
            }
        }
        else if (SystemInfo.isWindows) {
            if (path.charAt(0) == '/' && !path.startsWith("//")) {
                path = path.substring(1);  // hack over new File(path).toURI().toURL().getFile()
            }

            if (path.contains("~")) {
                try {
                    path = new File(FileUtil.toSystemDependentName(path)).getCanonicalPath();
                }
                catch (IOException e) {
                    return null;
                }
            }
        }

        File file = new File(path);
        if (!isAbsoluteFileOrDriveLetter(file)) {
            path = file.getAbsolutePath();
        }

        return FileUtil.normalize(path);
    }

    private static boolean isAbsoluteFileOrDriveLetter(File file) {
        String path = file.getPath();
        if (SystemInfo.isWindows && path.length() == 2 && path.charAt(1) == ':') {
            // just drive letter.
            // return true, despite the fact that technically it's not an absolute path
            return true;
        }
        return file.isAbsolute();
    }

    @Override
    public VirtualFile refreshAndFindFileByIoFile( File file) {
        String path = file.getAbsolutePath();
        return refreshAndFindFileByPath(path.replace(File.separatorChar, '/'));
    }

    @Override
    
    public VirtualFile refreshAndFindFileByIoFile( final IFile ioFile) {
        String path = ioFile.getPath();
        if (path == null) return null;
        return refreshAndFindFileByPath(path.replace(File.separatorChar, '/'));
    }

    @Override
    public void refreshIoFiles( Iterable<File> files) {
        refreshIoFiles(files, false, false, null);
    }

    @Override
    public void refreshIoFiles( Iterable<File> files, boolean async, boolean recursive,  Runnable onFinish) {
        final VirtualFileManagerEx manager = (VirtualFileManagerEx)VirtualFileManager.getInstance();

        Application app = ApplicationManager.getApplication();
        boolean fireCommonRefreshSession = app.isDispatchThread() || app.isWriteAccessAllowed();
        if (fireCommonRefreshSession) manager.fireBeforeRefreshStart(false);

        try {
            List<VirtualFile> filesToRefresh = new ArrayList<VirtualFile>();

            for (File file : files) {
                final VirtualFile virtualFile = refreshAndFindFileByIoFile(file);
                if (virtualFile != null) {
                    filesToRefresh.add(virtualFile);
                }
            }

            RefreshQueue.getInstance().refresh(async, recursive, onFinish, filesToRefresh);
        }
        finally {
            if (fireCommonRefreshSession) manager.fireAfterRefreshFinish(false);
        }
    }

    @Override
    public void refreshFiles( Iterable<VirtualFile> files) {
        refreshFiles(files, false, false, null);
    }

    @Override
    public void refreshFiles( Iterable<VirtualFile> files, boolean async, boolean recursive,  Runnable onFinish) {
        RefreshQueue.getInstance().refresh(async, recursive, onFinish, ContainerUtil.toCollection(files));
    }

    @Override
    public void registerAuxiliaryFileOperationsHandler( LocalFileOperationsHandler handler) {
        if (myHandlers.contains(handler)) {
            LOG.error("Handler " + handler + " already registered.");
        }
        myHandlers.add(handler);
    }

    @Override
    public void unregisterAuxiliaryFileOperationsHandler( LocalFileOperationsHandler handler) {
        if (!myHandlers.remove(handler)) {
            LOG.error("Handler" + handler + " haven't been registered or already unregistered.");
        }
    }

    @Override
    public boolean processCachedFilesInSubtree( final VirtualFile file,  Processor<VirtualFile> processor) {
        return file.getFileSystem() != this
                || processFile((NewVirtualFile)file, processor);
    }

    private static boolean processFile( NewVirtualFile file,  Processor<VirtualFile> processor) {
        if (!processor.process(file)) return false;
        if (file.isDirectory()) {
            for (final VirtualFile child : file.getCachedChildren()) {
                if (!processFile((NewVirtualFile)child, processor)) return false;
            }
        }
        return true;
    }

    private boolean auxDelete( VirtualFile file) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.delete(file)) return true;
        }

        return false;
    }

    private boolean auxMove( VirtualFile file,  VirtualFile toDir) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.move(file, toDir)) return true;
        }
        return false;
    }

    private boolean auxCopy( VirtualFile file,  VirtualFile toDir,  String copyName) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            final File copy = handler.copy(file, toDir, copyName);
            if (copy != null) return true;
        }
        return false;
    }

    private boolean auxRename( VirtualFile file,  String newName) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.rename(file, newName)) return true;
        }
        return false;
    }

    private boolean auxCreateFile( VirtualFile dir,  String name) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.createFile(dir, name)) return true;
        }
        return false;
    }

    private boolean auxCreateDirectory( VirtualFile dir,  String name) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.createDirectory(dir, name)) return true;
        }
        return false;
    }

    private void auxNotifyCompleted( ThrowableConsumer<LocalFileOperationsHandler, IOException> consumer) {
        for (LocalFileOperationsHandler handler : myHandlers) {
            handler.afterDone(consumer);
        }
    }

    @Override
    
    public VirtualFile createChildDirectory(Object requestor,  final VirtualFile parent,  final String dir) throws IOException {
        if (!VirtualFile.isValidName(dir)) {
            throw new IOException(VfsBundle.message("directory.invalid.name.error", dir));
        }

        if (!parent.exists() || !parent.isDirectory()) {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", parent.getPath()));
        }
        if (parent.findChild(dir) != null) {
            throw new IOException(VfsBundle.message("vfs.target.already.exists.error", parent.getPath() + "/" + dir));
        }

        File ioParent = convertToIOFile(parent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VfsBundle.message("target.not.directory.error", ioParent.getPath()));
        }

        if (!auxCreateDirectory(parent, dir)) {
            File ioDir = new File(ioParent, dir);
            if (!(ioDir.mkdirs() || ioDir.isDirectory())) {
                throw new IOException(VfsBundle.message("new.directory.failed.error", ioDir.getPath()));
            }
        }

        auxNotifyCompleted(new ThrowableConsumer<LocalFileOperationsHandler, IOException>() {
            @Override
            public void consume(LocalFileOperationsHandler handler) throws IOException {
                handler.createDirectory(parent, dir);
            }
        });

        return new FakeVirtualFile(parent, dir);
    }

    
    @Override
    public VirtualFile createChildFile(Object requestor,  final VirtualFile parent,  final String file) throws IOException {
        if (!VirtualFile.isValidName(file)) {
            throw new IOException(VfsBundle.message("file.invalid.name.error", file));
        }

        if (!parent.exists() || !parent.isDirectory()) {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", parent.getPath()));
        }
        if (parent.findChild(file) != null) {
            throw new IOException(VfsBundle.message("vfs.target.already.exists.error", parent.getPath() + "/" + file));
        }

        File ioParent = convertToIOFile(parent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VfsBundle.message("target.not.directory.error", ioParent.getPath()));
        }

        if (!auxCreateFile(parent, file)) {
            File ioFile = new File(ioParent, file);
            if (!FileUtil.createIfDoesntExist(ioFile)) {
                throw new IOException(VfsBundle.message("new.file.failed.error", ioFile.getPath()));
            }
        }

        auxNotifyCompleted(new ThrowableConsumer<LocalFileOperationsHandler, IOException>() {
            @Override
            public void consume(LocalFileOperationsHandler handler) throws IOException {
                handler.createFile(parent, file);
            }
        });

        return new FakeVirtualFile(parent, file);
    }

    @Override
    public void deleteFile(Object requestor,  final VirtualFile file) throws IOException {
        if (file.getParent() == null) {
            throw new IOException(VfsBundle.message("cannot.delete.root.directory", file.getPath()));
        }

        if (!auxDelete(file)) {
            File ioFile = convertToIOFile(file);
            if (!FileUtil.delete(ioFile)) {
                throw new IOException(VfsBundle.message("delete.failed.error", ioFile.getPath()));
            }
        }

        auxNotifyCompleted(new ThrowableConsumer<LocalFileOperationsHandler, IOException>() {
            @Override
            public void consume(LocalFileOperationsHandler handler) throws IOException {
                handler.delete(file);
            }
        });
    }

    @Override
    public boolean isCaseSensitive() {
        return SystemInfo.isFileSystemCaseSensitive;
    }

    @Override
    
    public InputStream getInputStream( final VirtualFile file) throws IOException {
        return new BufferedInputStream(new FileInputStream(convertToIOFileAndCheck(file)));
    }

    @Override
    
    public byte[] contentsToByteArray( final VirtualFile file) throws IOException {
        final FileInputStream stream = new FileInputStream(convertToIOFileAndCheck(file));
        try {
            long l = file.getLength();
            if (l > Integer.MAX_VALUE) throw new IOException("File is too large: " + l + ", " + file);
            final int length = (int)l;
            if (length < 0) throw new IOException("Invalid file length: " + length + ", " + file);
            // JDK's io_util.c#readBytes allocates custom native stack buffer for io operation with malloc if io request > 8K
            // so let's do buffered requests with buffer size 8192 that will use stack allocated buffer in native code
            return FileUtil.loadBytes(length <= 8192 ? stream : new BufferedInputStream(stream), length);
        }
        finally {
            stream.close();
        }
    }

    @Override
    
    public OutputStream getOutputStream( VirtualFile file, Object requestor, long modStamp, final long timeStamp) throws IOException {
        final File ioFile = convertToIOFileAndCheck(file);
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        final OutputStream stream = shallUseSafeStream(requestor, file) ?
                new SafeFileOutputStream(ioFile, SystemInfo.isUnix) : new FileOutputStream(ioFile);
        return new BufferedOutputStream(stream) {
            @Override
            public void close() throws IOException {
                super.close();
                if (timeStamp > 0 && ioFile.exists()) {
                    if (!ioFile.setLastModified(timeStamp)) {
                        LOG.warn("Failed: " + ioFile.getPath() + ", new:" + timeStamp + ", old:" + ioFile.lastModified());
                    }
                }
            }
        };
    }

    private static boolean shallUseSafeStream(final Object requestor,  VirtualFile file) {
        return requestor instanceof SafeWriteRequestor && GeneralSettings.getInstance().isUseSafeWrite() && !file.is(VFileProperty.SYMLINK);
    }

    @Override
    public void moveFile(Object requestor,  final VirtualFile file,  final VirtualFile newParent) throws IOException {
        String name = file.getName();

        if (!file.exists()) {
            throw new IOException(VfsBundle.message("vfs.file.not.exist.error", file.getPath()));
        }
        if (file.getParent() == null) {
            throw new IOException(VfsBundle.message("cannot.rename.root.directory", file.getPath()));
        }
        if (!newParent.exists() || !newParent.isDirectory()) {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", newParent.getPath()));
        }
        if (newParent.findChild(name) != null) {
            throw new IOException(VfsBundle.message("vfs.target.already.exists.error", newParent.getPath() + "/" + name));
        }

        File ioFile = convertToIOFile(file);
        if (!ioFile.exists()) {
            throw new FileNotFoundException(VfsBundle.message("file.not.exist.error", ioFile.getPath()));
        }
        File ioParent = convertToIOFile(newParent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VfsBundle.message("target.not.directory.error", ioParent.getPath()));
        }
        File ioTarget = new File(ioParent, name);
        if (ioTarget.exists()) {
            throw new IOException(VfsBundle.message("target.already.exists.error", ioTarget.getPath()));
        }

        if (!auxMove(file, newParent)) {
            if (!ioFile.renameTo(ioTarget)) {
                throw new IOException(VfsBundle.message("move.failed.error", ioFile.getPath(), ioParent.getPath()));
            }
        }

        auxNotifyCompleted(new ThrowableConsumer<LocalFileOperationsHandler, IOException>() {
            @Override
            public void consume(LocalFileOperationsHandler handler) throws IOException {
                handler.move(file, newParent);
            }
        });
    }

    @Override
    public void renameFile(Object requestor,  final VirtualFile file,  final String newName) throws IOException {
        if (!VirtualFile.isValidName(newName)) {
            throw new IOException(VfsBundle.message("file.invalid.name.error", newName));
        }

        boolean sameName = !isCaseSensitive() && newName.equalsIgnoreCase(file.getName());

        if (!file.exists()) {
            throw new IOException(VfsBundle.message("vfs.file.not.exist.error", file.getPath()));
        }
        VirtualFile parent = file.getParent();
        if (parent == null) {
            throw new IOException(VfsBundle.message("cannot.rename.root.directory", file.getPath()));
        }
        if (!sameName && parent.findChild(newName) != null) {
            throw new IOException(VfsBundle.message("vfs.target.already.exists.error", parent.getPath() + "/" + newName));
        }

        File ioFile = convertToIOFile(file);
        if (!ioFile.exists()) {
            throw new FileNotFoundException(VfsBundle.message("file.not.exist.error", ioFile.getPath()));
        }
        File ioTarget = new File(convertToIOFile(parent), newName);
        if (!sameName && ioTarget.exists()) {
            throw new IOException(VfsBundle.message("target.already.exists.error", ioTarget.getPath()));
        }

        if (!auxRename(file, newName)) {
            if (!ioFile.renameTo(ioTarget)) {
                throw new IOException(VfsBundle.message("rename.failed.error", ioFile.getPath(), newName));
            }
        }

        auxNotifyCompleted(new ThrowableConsumer<LocalFileOperationsHandler, IOException>() {
            @Override
            public void consume(LocalFileOperationsHandler handler) throws IOException {
                handler.rename(file, newName);
            }
        });
    }

    
    @Override
    public VirtualFile copyFile(Object requestor,
                                 final VirtualFile file,
                                 final VirtualFile newParent,
                                 final String copyName) throws IOException {
        if (!VirtualFile.isValidName(copyName)) {
            throw new IOException(VfsBundle.message("file.invalid.name.error", copyName));
        }

        if (!file.exists()) {
            throw new IOException(VfsBundle.message("vfs.file.not.exist.error", file.getPath()));
        }
        if (!newParent.exists() || !newParent.isDirectory()) {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", newParent.getPath()));
        }
        if (newParent.findChild(copyName) != null) {
            throw new IOException(VfsBundle.message("vfs.target.already.exists.error", newParent.getPath() + "/" + copyName));
        }

        FileAttributes attributes = getAttributes(file);
        if (attributes == null) {
            throw new FileNotFoundException(VfsBundle.message("file.not.exist.error", file.getPath()));
        }
        if (attributes.isSpecial()) {
            throw new FileNotFoundException("Not a file: " + file);
        }
        File ioParent = convertToIOFile(newParent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VfsBundle.message("target.not.directory.error", ioParent.getPath()));
        }
        File ioTarget = new File(ioParent, copyName);
        if (ioTarget.exists()) {
            throw new IOException(VfsBundle.message("target.already.exists.error", ioTarget.getPath()));
        }

        if (!auxCopy(file, newParent, copyName)) {
            try {
                File ioFile = convertToIOFile(file);
                if (attributes.isDirectory()) {
                    FileUtil.copyDir(ioFile, ioTarget);
                }
                else {
                    FileUtil.copy(ioFile, ioTarget);
                }
            }
            catch (IOException e) {
                FileUtil.delete(ioTarget);
                throw e;
            }
        }

        auxNotifyCompleted(new ThrowableConsumer<LocalFileOperationsHandler, IOException>() {
            @Override
            public void consume(LocalFileOperationsHandler handler) throws IOException {
                handler.copy(file, newParent, copyName);
            }
        });

        return new FakeVirtualFile(newParent, copyName);
    }

    @Override
    public void setTimeStamp( final VirtualFile file, final long timeStamp) {
        final File ioFile = convertToIOFile(file);
        if (ioFile.exists() && !ioFile.setLastModified(timeStamp)) {
            LOG.warn("Failed: " + file.getPath() + ", new:" + timeStamp + ", old:" + ioFile.lastModified());
        }
    }

    @Override
    public void setWritable( final VirtualFile file, final boolean writableFlag) throws IOException {
        FileUtil.setReadOnlyAttribute(file.getPath(), !writableFlag);
        final File ioFile = convertToIOFile(file);
        if (ioFile.canWrite() != writableFlag) {
            throw new IOException("Failed to change read-only flag for " + ioFile.getPath());
        }
    }

    
    @Override
    protected String extractRootPath( final String path) {
        if (path.isEmpty()) {
            try {
                return extractRootPath(new File("").getCanonicalPath());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (SystemInfo.isWindows) {
            if (path.length() >= 2 && path.charAt(1) == ':') {
                // Drive letter
                return path.substring(0, 2).toUpperCase(Locale.US);
            }

            if (path.startsWith("//") || path.startsWith("\\\\")) {
                // UNC. Must skip exactly two path elements like [\\ServerName\ShareName]\pathOnShare\file.txt
                // Root path is in square brackets here.

                int slashCount = 0;
                int idx;
                for (idx = 2; idx < path.length() && slashCount < 2; idx++) {
                    final char c = path.charAt(idx);
                    if (c == '\\' || c == '/') {
                        slashCount++;
                        idx--;
                    }
                }

                return path.substring(0, idx);
            }

            return "";
        }

        return StringUtil.startsWithChar(path, '/') ? "/" : "";
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public boolean markNewFilesAsDirty() {
        return true;
    }

    
    @Override
    public String getCanonicallyCasedName( final VirtualFile file) {
        if (isCaseSensitive()) {
            return super.getCanonicallyCasedName(file);
        }

        final String originalFileName = file.getName();
        try {
            final File ioFile = convertToIOFile(file);
            final File ioCanonicalFile = ioFile.getCanonicalFile();
            String canonicalFileName = ioCanonicalFile.getName();
            if (!SystemInfo.isUnix) {
                return canonicalFileName;
            }
            // linux & mac support symbolic links
            // unfortunately canonical file resolves sym links
            // so its name may differ from name of origin file
            //
            // Here FS is case sensitive, so let's check that original and
            // canonical file names are equal if we ignore name case
            if (canonicalFileName.compareToIgnoreCase(originalFileName) == 0) {
                // p.s. this should cover most cases related to not symbolic links
                return canonicalFileName;
            }

            // Ok, names are not equal. Let's try to find corresponding file name
            // among original file parent directory
            final File parentFile = ioFile.getParentFile();
            if (parentFile != null) {
                // I hope ls works fast on Unix
                final String[] canonicalFileNames = parentFile.list();
                if (canonicalFileNames != null) {
                    for (String name : canonicalFileNames) {
                        // if names are equals
                        if (name.compareToIgnoreCase(originalFileName) == 0) {
                            return name;
                        }
                    }
                }
            }
            // No luck. So ein mist!
            // Ok, garbage in, garbage out. We may return original or canonical name
            // no difference. Let's return canonical name just to preserve previous
            // behaviour of this code.
            return canonicalFileName;
        }
        catch (IOException e) {
            return originalFileName;
        }
    }

    @Override
    public FileAttributes getAttributes( final VirtualFile file) {
        String path = normalize(file.getPath());
        if (path == null) return null;
        if (file.getParent() == null && path.startsWith("//")) {
            return FAKE_ROOT_ATTRIBUTES;  // fake Windows roots
        }
        return FileSystemUtil.getAttributes(FileUtil.toSystemDependentName(path));
    }

    @Override
    public void refresh(final boolean asynchronous) {
        RefreshQueue.getInstance().refresh(asynchronous, true, null, ManagingFS.getInstance().getRoots(this));
    }
}
