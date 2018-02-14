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
package com.gome.maven.openapi.vfs.impl.jrt;

import com.gome.maven.ide.AppLifecycleListener;
import com.gome.maven.lang.LangBundle;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.JavaSdk;
import com.gome.maven.openapi.projectRoots.ProjectJdkTable;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.impl.ArchiveHandler;
import com.gome.maven.openapi.vfs.newvfs.*;
import com.gome.maven.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBusConnection;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gome.maven.util.containers.ContainerUtil.newTroveMap;

public class JrtFileSystem extends ArchiveFileSystem {
    public static final String PROTOCOL = StandardFileSystems.JRT_PROTOCOL;
    public static final String SEPARATOR = JarFileSystem.JAR_SEPARATOR;

    private final Map<String, ArchiveHandler> myHandlers = newTroveMap(FileUtil.PATH_HASHING_STRATEGY);
    private final AtomicBoolean mySubscribed = new AtomicBoolean(false);

    public JrtFileSystem() {
        scheduleConfiguredSdkCheck();
    }

    private static void scheduleConfiguredSdkCheck() {
        if (isSupported()) return;
        final MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener.Adapter() {
            @Override
            public void appStarting(Project project) {
                for (Sdk sdk : ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance())) {
                    String homePath = sdk.getHomePath();
                    if (homePath != null && isModularJdk(homePath)) {
                        String title = LangBundle.message("jrt.not.available.title", sdk.getName());
                        String message = LangBundle.message("jrt.not.available.message");
                        Notifications.Bus.notify(new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, title, message, NotificationType.WARNING));
                    }
                }
                connection.disconnect();
            }
        });
    }

    
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    
    @Override
    protected String extractLocalPath( String rootPath) {
        return StringUtil.trimEnd(rootPath, SEPARATOR);
    }

    
    @Override
    protected String composeRootPath( String localPath) {
        return localPath + SEPARATOR;
    }

    
    @Override
    protected String extractRootPath( String entryPath) {
        int separatorIndex = entryPath.indexOf(SEPARATOR);
        assert separatorIndex >= 0 : "Path passed to JrtFileSystem must have a separator '!/': " + entryPath;
        return entryPath.substring(0, separatorIndex + SEPARATOR.length());
    }

    
    @Override
    protected ArchiveHandler getHandler( VirtualFile entryFile) {
        checkSubscription();

        final String homePath = extractLocalPath(extractRootPath(entryFile.getPath()));
        ArchiveHandler handler = myHandlers.get(homePath);
        if (handler == null) {
            handler = isSupported() ? new JrtHandler(homePath) : new JrtHandlerStub(homePath);
            myHandlers.put(homePath, handler);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByPath(homePath + "/lib/modules");
                    if (dir != null) dir.getChildren();
                }
            }, ModalityState.defaultModalityState());
        }
        return handler;
    }

    private void checkSubscription() {
        if (mySubscribed.getAndSet(true)) return;

        Application app = ApplicationManager.getApplication();
        app.getMessageBus().connect(app).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after( List<? extends VFileEvent> events) {
                Set<VirtualFile> toRefresh = null;

                for (VFileEvent event : events) {
                    if (event.getFileSystem() instanceof LocalFileSystem && event instanceof VFileContentChangeEvent) {
                        VirtualFile file = event.getFile();
                        if (file != null && "jimage".equals(file.getExtension())) {
                            String homePath = file.getParent().getParent().getParent().getPath();
                            if (myHandlers.remove(homePath) != null) {
                                VirtualFile root = findFileByPath(composeRootPath(homePath));
                                if (root != null) {
                                    ((NewVirtualFile)root).markDirtyRecursively();
                                    if (toRefresh == null) toRefresh = ContainerUtil.newHashSet();
                                    toRefresh.add(root);
                                }
                            }
                        }
                    }
                }

                if (toRefresh != null) {
                    boolean async = !ApplicationManager.getApplication().isUnitTestMode();
                    RefreshQueue.getInstance().refresh(async, true, null, toRefresh);
                }
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

    public static boolean isSupported() {
        return SystemInfo.isJavaVersionAtLeast("1.8") && !SystemInfo.isJavaVersionAtLeast("1.9");
    }

    public static boolean isModularJdk( String homePath) {
        return new File(homePath, "lib/modules").isDirectory();
    }

    public static boolean isRoot( VirtualFile file) {
        return file.getParent() == null && file.getFileSystem() instanceof JrtFileSystem;
    }
}
