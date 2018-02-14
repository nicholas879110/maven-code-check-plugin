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
package com.gome.maven.ui;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.FileEditorManagerAdapter;
import com.gome.maven.openapi.fileEditor.FileEditorManagerListener;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.util.ProgressIndicatorBase;
import com.gome.maven.openapi.progress.util.ProgressIndicatorUtils;
import com.gome.maven.openapi.progress.util.ReadTask;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBusConnection;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.update.MergingUpdateQueue;
import com.gome.maven.util.ui.update.Update;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author peter
 */
public class EditorNotificationsImpl extends EditorNotifications {
    private static final ExtensionPointName<Provider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.intellij.editorNotificationProvider");
    private static final Key<WeakReference<ProgressIndicator>> CURRENT_UPDATES = Key.create("CURRENT_UPDATES");
    private final ThreadPoolExecutor myExecutor = ConcurrencyUtil.newSingleThreadExecutor("EditorNotifications executor");
    private final MergingUpdateQueue myUpdateMerger;

    public EditorNotificationsImpl(Project project) {
        super(project);
        myUpdateMerger = new MergingUpdateQueue("EditorNotifications update merger", 100, true, null, project);
        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
            @Override
            public void fileOpened( FileEditorManager source,  VirtualFile file) {
                updateNotifications(file);
            }
        });
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                updateAllNotifications();
            }

            @Override
            public void exitDumbMode() {
                updateAllNotifications();
            }
        });

    }

    @Override
    public void updateNotifications( final VirtualFile file) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                ProgressIndicator indicator = getCurrentProgress(file);
                if (indicator != null) {
                    indicator.cancel();
                }
                file.putUserData(CURRENT_UPDATES, null);

                if (myProject.isDisposed() || !file.isValid()) {
                    return;
                }

                indicator = new ProgressIndicatorBase();
                final ReadTask task = createTask(indicator, file);
                if (task == null) return;

                file.putUserData(CURRENT_UPDATES, new WeakReference<ProgressIndicator>(indicator));
                if (ApplicationManager.getApplication().isUnitTestMode()) {
                    task.computeInReadAction(indicator);
                }
                else {
                    ProgressIndicatorUtils.scheduleWithWriteActionPriority(indicator, myExecutor, task);
                }
            }
        });
    }

    
    private ReadTask createTask(final ProgressIndicator indicator,  final VirtualFile file) {
        final FileEditor[] editors = FileEditorManager.getInstance(myProject).getAllEditors(file);
        if (editors.length == 0) return null;

        return new ReadTask() {
            private boolean isOutdated() {
                if (myProject.isDisposed() || !file.isValid() || indicator != getCurrentProgress(file)) {
                    return true;
                }

                for (FileEditor editor : editors) {
                    if (!editor.isValid()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public void computeInReadAction( final ProgressIndicator indicator) {
                if (isOutdated()) return;

                final List<Runnable> updates = ContainerUtil.newArrayList();
                for (final FileEditor editor : editors) {
                    for (final Provider<?> provider : Extensions.getExtensions(EXTENSION_POINT_NAME, myProject)) {
                        final JComponent component = provider.createNotificationPanel(file, editor);
                        updates.add(new Runnable() {
                            @Override
                            public void run() {
                                updateNotification(editor, provider.getKey(), component);
                            }
                        });
                    }
                }

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        if (!isOutdated()) {
                            file.putUserData(CURRENT_UPDATES, null);
                            for (Runnable update : updates) {
                                update.run();
                            }
                        }
                    }
                });
            }

            @Override
            public void onCanceled( ProgressIndicator ignored) {
                if (getCurrentProgress(file) == indicator) {
                    updateNotifications(file);
                }
            }
        };
    }

    private static ProgressIndicator getCurrentProgress(VirtualFile file) {
        return SoftReference.dereference(file.getUserData(CURRENT_UPDATES));
    }


    private void updateNotification( FileEditor editor,  Key<? extends JComponent> key,  JComponent component) {
        JComponent old = editor.getUserData(key);
        if (old != null) {
            FileEditorManager.getInstance(myProject).removeTopComponent(editor, old);
        }
        if (component != null) {
            FileEditorManager.getInstance(myProject).addTopComponent(editor, component);
            @SuppressWarnings("unchecked") Key<JComponent> _key = (Key<JComponent>)key;
            editor.putUserData(_key, component);
        }
        else {
            editor.putUserData(key, null);
        }
    }

    @Override
    public void updateAllNotifications() {
        myUpdateMerger.queue(new Update("update") {
            @Override
            public void run() {
                for (VirtualFile file : FileEditorManager.getInstance(myProject).getOpenFiles()) {
                    updateNotifications(file);
                }
            }
        });
    }
}
