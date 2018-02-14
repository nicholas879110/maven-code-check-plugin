/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManagerAdapter;
import com.gome.maven.openapi.fileEditor.FileEditorManagerEvent;
import com.gome.maven.openapi.fileEditor.FileEditorManagerListener;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.messages.MessageBusConnection;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("MethodMayBeStatic")
public abstract class AutoScrollFromSourceHandler implements Disposable {
    protected final Project myProject;
    protected final Alarm myAlarm;
    private JComponent myComponent;

    public AutoScrollFromSourceHandler( Project project,  JComponent view) {
        this(project, view, null);
    }

    public AutoScrollFromSourceHandler( Project project,  JComponent view,  Disposable parentDisposable) {
        myProject = project;

        if (parentDisposable != null) {
            Disposer.register(parentDisposable, this);
        }
        myComponent = view;
        myAlarm = new Alarm(this);
    }

    protected abstract boolean isAutoScrollEnabled();

    protected abstract void setAutoScrollEnabled(boolean enabled);

    protected abstract void selectElementFromEditor( FileEditor editor);

    protected ModalityState getModalityState() {
        return ModalityState.current();
    }

    protected long getAlarmDelay() {
        return 500;
    }

    public void install() {
        final MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
            @Override
            public void selectionChanged( FileEditorManagerEvent event) {
                final FileEditor editor = event.getNewEditor();
                if (editor != null && myComponent.isShowing() && isAutoScrollEnabled()) {
                    myAlarm.cancelAllRequests();
                    myAlarm.addRequest(new Runnable() {
                        @Override
                        public void run() {
                            selectElementFromEditor(editor);
                        }
                    }, getAlarmDelay(), getModalityState());
                }
            }
        });
    }

    @Override
    public void dispose() {
        if (!myAlarm.isDisposed()) {
            myAlarm.cancelAllRequests();
        }
    }

    public ToggleAction createToggleAction() {
        return new AutoScrollFromSourceAction();
    }

    private class AutoScrollFromSourceAction extends ToggleAction implements DumbAware {
        public AutoScrollFromSourceAction() {
            super(UIBundle.message("autoscroll.from.source.action.name"),
                    UIBundle.message("autoscroll.from.source.action.description"),
                    AllIcons.General.AutoscrollFromSource);
        }

        public boolean isSelected(final AnActionEvent event) {
            return isAutoScrollEnabled();
        }

        public void setSelected(final AnActionEvent event, final boolean flag) {
            setAutoScrollEnabled(flag);
        }
    }
}

