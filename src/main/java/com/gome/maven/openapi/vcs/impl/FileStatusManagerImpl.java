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
package com.gome.maven.openapi.vcs.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.ProjectComponent;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.colors.ColorKey;
import com.gome.maven.openapi.editor.colors.EditorColorsListener;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.impl.DirectoryIndex;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusListener;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashMap;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
public class FileStatusManagerImpl extends FileStatusManager implements ProjectComponent {
    private final Map<VirtualFile, FileStatus> myCachedStatuses = Collections.synchronizedMap(new HashMap<VirtualFile, FileStatus>());
    private final Map<VirtualFile, Boolean> myWhetherExactlyParentToChanged =
            Collections.synchronizedMap(new HashMap<VirtualFile, Boolean>());
    private final Project myProject;
    private final List<FileStatusListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private FileStatusProvider myFileStatusProvider;
    private final NotNullLazyValue<FileStatusProvider[]> myExtensions = new NotNullLazyValue<FileStatusProvider[]>() {
        
        @Override
        protected FileStatusProvider[] compute() {
            return Extensions.getExtensions(FileStatusProvider.EP_NAME, myProject);
        }
    };

    private static class FileStatusNull implements FileStatus {
        private static final FileStatus INSTANCE = new FileStatusNull();

        @Override
        public String getText() {
            throw new AssertionError("Should not be called");
        }

        @Override
        public Color getColor() {
            throw new AssertionError("Should not be called");
        }

        
        @Override
        public ColorKey getColorKey() {
            throw new AssertionError("Should not be called");
        }

        
        @Override
        public String getId() {
            throw new AssertionError("Should not be called");
        }
    }

    public FileStatusManagerImpl(Project project, StartupManager startupManager, EditorColorsManager colorsManager,
                                 @SuppressWarnings("UnusedParameters") DirectoryIndex makeSureIndexIsInitializedFirst) {
        myProject = project;

        colorsManager.addEditorColorsListener(new EditorColorsListener() {
            @Override
            public void globalSchemeChange(EditorColorsScheme scheme) {
                fileStatusesChanged();
            }
        }, myProject);

        startupManager.registerPreStartupActivity(new Runnable() {
            @Override
            public void run() {
                DocumentAdapter documentListener = new DocumentAdapter() {
                    @Override
                    public void documentChanged(DocumentEvent event) {
                        VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
                        if (file != null) {
                            refreshFileStatusFromDocument(file, event.getDocument());
                        }
                    }
                };

                final EditorFactory factory = EditorFactory.getInstance();
                if (factory != null) {
                    factory.getEventMulticaster().addDocumentListener(documentListener, myProject);
                }
            }
        });
        startupManager.registerPostStartupActivity(new DumbAwareRunnable() {
            @Override
            public void run() {
                fileStatusesChanged();
            }
        });
    }

    public void setFileStatusProvider(final FileStatusProvider fileStatusProvider) {
        myFileStatusProvider = fileStatusProvider;
    }

    public FileStatus calcStatus( final VirtualFile virtualFile) {
        for (FileStatusProvider extension : myExtensions.getValue()) {
            final FileStatus status = extension.getFileStatus(virtualFile);
            if (status != null) {
                return status;
            }
        }

        if (virtualFile.isInLocalFileSystem() && myFileStatusProvider != null) {
            return myFileStatusProvider.getFileStatus(virtualFile);
        }

        return getDefaultStatus(virtualFile);
    }

    
    public static FileStatus getDefaultStatus( final VirtualFile file) {
        return file.isValid() && file.is(VFileProperty.SPECIAL) ? FileStatus.IGNORED : FileStatus.NOT_CHANGED;
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void disposeComponent() {
        myCachedStatuses.clear();
    }

    @Override
    
    public String getComponentName() {
        return "FileStatusManager";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void addFileStatusListener( FileStatusListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void addFileStatusListener( final FileStatusListener listener,  Disposable parentDisposable) {
        addFileStatusListener(listener);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeFileStatusListener(listener);
            }
        });
    }

    @Override
    public void fileStatusesChanged() {
        if (myProject.isDisposed()) {
            return;
        }
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
                @Override
                public void run() {
                    fileStatusesChanged();
                }
            }, ModalityState.NON_MODAL);
            return;
        }

        myCachedStatuses.clear();
        myWhetherExactlyParentToChanged.clear();

        for (FileStatusListener listener : myListeners) {
            listener.fileStatusesChanged();
        }
    }

    private void cacheChangedFileStatus(final VirtualFile virtualFile, final FileStatus fs) {
        myCachedStatuses.put(virtualFile, fs);
        if (FileStatus.NOT_CHANGED.equals(fs)) {
            final ThreeState parentingStatus = myFileStatusProvider.getNotChangedDirectoryParentingStatus(virtualFile);
            if (ThreeState.YES.equals(parentingStatus)) {
                myWhetherExactlyParentToChanged.put(virtualFile, true);
            }
            else if (ThreeState.UNSURE.equals(parentingStatus)) {
                myWhetherExactlyParentToChanged.put(virtualFile, false);
            }
        }
        else {
            myWhetherExactlyParentToChanged.remove(virtualFile);
        }
    }

    @Override
    public void fileStatusChanged(final VirtualFile file) {
        final Application application = ApplicationManager.getApplication();
        if (!application.isDispatchThread() && !application.isUnitTestMode()) {
            ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
                @Override
                public void run() {
                    fileStatusChanged(file);
                }
            });
            return;
        }

        if (file == null || !file.isValid()) return;
        FileStatus cachedStatus = getCachedStatus(file);
        if (cachedStatus == FileStatusNull.INSTANCE) {
            return;
        }
        if (cachedStatus == null) {
            cacheChangedFileStatus(file, FileStatusNull.INSTANCE);
            return;
        }
        FileStatus newStatus = calcStatus(file);
        if (cachedStatus == newStatus) return;
        cacheChangedFileStatus(file, newStatus);

        for (FileStatusListener listener : myListeners) {
            listener.fileStatusChanged(file);
        }
    }

    @Override
    public FileStatus getStatus( final VirtualFile file) {
        if (file instanceof LightVirtualFile) {
            return FileStatus.NOT_CHANGED;  // do not leak light files via cache
        }

        FileStatus status = getCachedStatus(file);
        if (status == null || status == FileStatusNull.INSTANCE) {
            status = calcStatus(file);
            cacheChangedFileStatus(file, status);
        }

        return status;
    }

    public FileStatus getCachedStatus(final VirtualFile file) {
        return myCachedStatuses.get(file);
    }

    @Override
    public void removeFileStatusListener( FileStatusListener listener) {
        myListeners.remove(listener);
    }

    @Override
    public Color getNotChangedDirectoryColor( VirtualFile vf) {
        final Color notChangedColor = FileStatus.NOT_CHANGED.getColor();
        if (!vf.isDirectory()) {
            return notChangedColor;
        }
        final Boolean exactMatch = myWhetherExactlyParentToChanged.get(vf);
        return exactMatch == null
                ? notChangedColor
                : exactMatch ? FileStatus.NOT_CHANGED_IMMEDIATE.getColor() : FileStatus.NOT_CHANGED_RECURSIVE.getColor();
    }

    public void refreshFileStatusFromDocument(final VirtualFile file, final Document doc) {
        if (myFileStatusProvider != null) {
            myFileStatusProvider.refreshFileStatusFromDocument(file, doc);
        }
    }
}
