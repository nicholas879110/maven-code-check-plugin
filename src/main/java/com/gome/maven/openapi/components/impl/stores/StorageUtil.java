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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.NotificationsManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.WriteAction;
import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.StateStorage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.components.store.ReadOnlyModificationException;
import com.gome.maven.openapi.editor.DocumentRunnable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.util.*;
//import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;
import org.jdom.Element;
import org.jdom.Parent;


import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.List;

public class StorageUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.components.impl.stores.StorageUtil");

    private static final byte[] XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(CharsetToolkit.UTF8_CHARSET);

    private static final Pair<byte[], String> NON_EXISTENT_FILE_DATA = Pair.create(null, SystemProperties.getLineSeparator());

    private StorageUtil() { }

    public static boolean isChangedByStorageOrSaveSession( VirtualFileEvent event) {
        return event.getRequestor() instanceof StateStorage.SaveSession || event.getRequestor() instanceof StateStorage;
    }

    public static void notifyUnknownMacros( TrackingPathMacroSubstitutor substitutor,
                                            final Project project,
                                            final String componentName) {
        final LinkedHashSet<String> macros = new LinkedHashSet<String>(substitutor.getUnknownMacros(componentName));
        if (macros.isEmpty()) {
            return;
        }
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                List<String> notified = null;
                NotificationsManager manager = NotificationsManager.getNotificationsManager();
                for (UnknownMacroNotification notification : manager.getNotificationsOfType(UnknownMacroNotification.class, project)) {
                    if (notified == null) {
                        notified = new SmartList<String>();
                    }
                    notified.addAll(notification.getMacros());
                }
                if (!ContainerUtil.isEmpty(notified)) {
                    macros.removeAll(notified);
                }

                if (!macros.isEmpty()) {
                    LOG.debug("Reporting unknown path macros " + macros + " in component " + componentName);
                    String format = "<p><i>%s</i> %s undefined. <a href=\"define\">Fix it</a></p>";
                    String productName = ApplicationNamesInfo.getInstance().getProductName();
                    String content = String.format(format, StringUtil.join(macros, ", "), macros.size() == 1 ? "is" : "are") +
                            "<br>Path variables are used to substitute absolute paths " +
                            "in " + productName + " project files " +
                            "and allow project file sharing in version control systems.<br>" +
                            "Some of the files describing the current project settings contain unknown path variables " +
                            "and " + productName + " cannot restore those paths.";
                    new UnknownMacroNotification("Load Error", "Load error: undefined path variables", content, NotificationType.ERROR,
                            new NotificationListener() {
                                @Override
                                public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                                    ((ProjectEx)project).checkUnknownMacros(true);
                                }
                            }, macros).notify(project);
                }
            }
        });
    }

    
    public static VirtualFile writeFile( File file,
                                         Object requestor,
                                         VirtualFile virtualFile,
                                         BufferExposingByteArrayOutputStream content,
                                         LineSeparator lineSeparatorIfPrependXmlProlog) throws IOException {
        final VirtualFile result;
        if (file != null && (virtualFile == null || !virtualFile.isValid())) {
            result = getOrCreateVirtualFile(requestor, file.getAbsolutePath());
        }
        else {
            result = virtualFile;
            assert result != null;
        }

        boolean equals = isEqualContent(result, lineSeparatorIfPrependXmlProlog, content);
        if (equals) {
            // commented — the upcoming fix of such warnings will not be cherry-picked to release (is not safe)
            //LOG.warn("Content equals, but it must be handled not on this level — " + result.getName());
            return result;
        }
        else {
            doWrite(requestor, result, virtualFile, content, lineSeparatorIfPrependXmlProlog);
            return result;
        }
    }

    private static void doWrite( final Object requestor,
                                 final VirtualFile file,
                                 final VirtualFile proposedFile,
                                 final BufferExposingByteArrayOutputStream content,
                                 final LineSeparator lineSeparatorIfPrependXmlProlog) throws IOException {
        AccessToken token = WriteAction.start();
        try {
            OutputStream out = file.getOutputStream(requestor);
            try {
                if (lineSeparatorIfPrependXmlProlog != null) {
                    out.write(XML_PROLOG);
                    out.write(lineSeparatorIfPrependXmlProlog.getSeparatorBytes());
                }
                content.writeTo(out);
            }
            finally {
                out.close();
            }
        }
        catch (FileNotFoundException e) {
            if (proposedFile == null) {
                throw e;
            }
            else {
                throw new ReadOnlyModificationException(proposedFile, e, new StateStorage.SaveSession() {
                    @Override
                    public void save() throws IOException {
                        doWrite(requestor, file, proposedFile, content, lineSeparatorIfPrependXmlProlog);
                    }
                });
            }
        }
        finally {
            token.finish();
        }
    }

    private static boolean isEqualContent(VirtualFile result,
                                           LineSeparator lineSeparatorIfPrependXmlProlog,
                                           BufferExposingByteArrayOutputStream content) throws IOException {
        boolean equals = true;
        int headerLength = lineSeparatorIfPrependXmlProlog == null ? 0 : XML_PROLOG.length + lineSeparatorIfPrependXmlProlog.getSeparatorBytes().length;
        int toWriteLength = headerLength + content.size();

        if (result.getLength() != toWriteLength) {
            equals = false;
        }
        else {
            byte[] bytes = result.contentsToByteArray();
            if (lineSeparatorIfPrependXmlProlog != null) {
                if (!ArrayUtil.startsWith(bytes, XML_PROLOG) || !ArrayUtil.startsWith(bytes, XML_PROLOG.length, lineSeparatorIfPrependXmlProlog.getSeparatorBytes())) {
                    equals = false;
                }
            }
            if (!ArrayUtil.startsWith(bytes, headerLength, content.toByteArray())) {
                equals = false;
            }
        }
        return equals;
    }

    public static void deleteFile( File file,  final Object requestor,  final VirtualFile virtualFile) throws IOException {
        if (virtualFile == null) {
            LOG.warn("Cannot find virtual file " + file.getAbsolutePath());
        }

        if (virtualFile == null) {
            if (file.exists()) {
                FileUtil.delete(file);
            }
        }
        else if (virtualFile.exists()) {
            try {
                deleteFile(requestor, virtualFile);
            }
            catch (FileNotFoundException e) {
                throw new ReadOnlyModificationException(virtualFile, e, new StateStorage.SaveSession() {
                    @Override
                    public void save() throws IOException {
                        deleteFile(requestor, virtualFile);
                    }
                });
            }
        }
    }

    public static void deleteFile( Object requestor,  VirtualFile virtualFile) throws IOException {
        AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
        try {
            virtualFile.delete(requestor);
        }
        finally {
            token.finish();
        }
    }

    
    public static BufferExposingByteArrayOutputStream writeToBytes( Parent element,  String lineSeparator) throws IOException {
        BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream(512);
        JDOMUtil.writeParent(element, out, lineSeparator);
        return out;
    }

    
    public static VirtualFile getOrCreateVirtualFile( Object requestor,  String path) throws IOException {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (virtualFile != null) {
            return virtualFile;
        }

        String parentFile = PathUtilRt.getParentPath(path);
        // need refresh if the directory has just been created
        VirtualFile parentVirtualFile = StringUtil.isEmpty(parentFile) ? null : LocalFileSystem.getInstance().refreshAndFindFileByPath(parentFile);
        if (parentVirtualFile == null) {
            throw new IOException(ProjectBundle.message("project.configuration.save.file.not.found", parentFile));
        }

        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            return parentVirtualFile.createChildData(requestor, PathUtilRt.getFileName(path));
        }
        else {
            AccessToken token = WriteAction.start();
            try {
                return parentVirtualFile.createChildData(requestor, PathUtilRt.getFileName(path));
            }
            finally {
                token.finish();
            }
        }
    }

    /**
     * @return pair.first - file contents (null if file does not exist), pair.second - file line separators
     */
    
    public static Pair<byte[], String> loadFile( final VirtualFile file) throws IOException {
        if (file == null || !file.exists()) {
            return NON_EXISTENT_FILE_DATA;
        }

        byte[] bytes = file.contentsToByteArray();
        String lineSeparator = file.getDetectedLineSeparator();
        if (lineSeparator == null) {
            lineSeparator = detectLineSeparators(CharsetToolkit.UTF8_CHARSET.decode(ByteBuffer.wrap(bytes)), null).getSeparatorString();
        }
        return Pair.create(bytes, lineSeparator);
    }

    
    public static LineSeparator detectLineSeparators( CharSequence chars,  LineSeparator defaultSeparator) {
        for (int i = 0, n = chars.length(); i < n; i++) {
            char c = chars.charAt(i);
            if (c == '\r') {
                return LineSeparator.CRLF;
            }
            else if (c == '\n') {
                // if we are here, there was no \r before
                return LineSeparator.LF;
            }
        }
        return defaultSeparator == null ? LineSeparator.getSystemLineSeparator() : defaultSeparator;
    }

    public static void delete( StreamProvider provider,  String fileSpec,  RoamingType type) {
        if (provider.isApplicable(fileSpec, type)) {
            provider.delete(fileSpec, type);
        }
    }

    /**
     * You must call {@link StreamProvider#isApplicable(String, com.gome.maven.openapi.components.RoamingType)} before
     */
    public static void sendContent( StreamProvider provider,  String fileSpec,  Element element,  RoamingType type, boolean async) throws IOException {
        // we should use standard line-separator (\n) - stream provider can share file content on any OS
        BufferExposingByteArrayOutputStream content = writeToBytes(element, "\n");
        provider.saveContent(fileSpec, content.getInternalBuffer(), content.size(), type, async);
    }

    public static boolean isProjectOrModuleFile( String fileSpec) {
        return StoragePathMacros.PROJECT_FILE.equals(fileSpec) || fileSpec.startsWith(StoragePathMacros.PROJECT_CONFIG_DIR) || fileSpec.equals(StoragePathMacros.MODULE_FILE);
    }
}
