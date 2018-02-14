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
package com.gome.maven.openapi.wm.impl.status;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.fileEditor.*;
import com.gome.maven.openapi.fileEditor.ex.FileEditorManagerEx;
import com.gome.maven.openapi.fileEditor.impl.EditorsSplitters;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.StatusBarWidget;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.io.ReadOnlyAttributeUtil;
import com.gome.maven.util.messages.MessageBusConnection;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class ToggleReadOnlyAttributePanel extends FileEditorManagerAdapter implements StatusBarWidget.Multiframe,
        StatusBarWidget.IconPresentation{
    private Project myProject;
    private StatusBar myStatusBar;

    public ToggleReadOnlyAttributePanel( Project project) {
        myProject = project;
        MessageBusConnection connection = project.getMessageBus().connect(this);
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
    }

    
    public Icon getIcon() {
        VirtualFile virtualFile = getCurrentFile();
        return virtualFile == null || virtualFile.isWritable() ? AllIcons.Ide.Readwrite : AllIcons.Ide.Readonly;
    }

    
    public String ID() {
        return "ReadOnlyAttribute";
    }


    @Override
    public StatusBarWidget copy() {
        return new ToggleReadOnlyAttributePanel(myProject);
    }

    public WidgetPresentation getPresentation( PlatformType type) {
        return this;
    }

    public void dispose() {
        myStatusBar = null;
        myProject = null;
    }

    public void install( StatusBar statusBar) {
        myStatusBar = statusBar;
    }

    public String getTooltipText() {
        return isReadonlyApplicable() ? UIBundle.message("read.only.attr.panel.double.click.to.toggle.attr.tooltip.text") : null;
    }

    public Consumer<MouseEvent> getClickConsumer() {
        return new Consumer<MouseEvent>() {
            public void consume(MouseEvent mouseEvent) {
                final VirtualFile file = getCurrentFile();
                if (!isReadOnlyApplicableForFile(file)) {
                    return;
                }
                FileDocumentManager.getInstance().saveAllDocuments();

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        try {
                            ReadOnlyAttributeUtil.setReadOnlyAttribute(file, file.isWritable());
                            myStatusBar.updateWidget(ID());
                        }
                        catch (IOException e) {
                            Messages.showMessageDialog(getProject(), e.getMessage(), UIBundle.message("error.dialog.title"), Messages.getErrorIcon());
                        }
                    }
                });
            }
        };
    }

    private boolean isReadonlyApplicable() {
        VirtualFile file = getCurrentFile();
        return isReadOnlyApplicableForFile(file);
    }

    private static boolean isReadOnlyApplicableForFile( VirtualFile file) {
        return file != null && !file.getFileSystem().isReadOnly();
    }

    
    private Project getProject() {
        return CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext((JComponent) myStatusBar));
    }

    
    private VirtualFile getCurrentFile() {
        final Project project = getProject();
        if (project == null) return null;
        EditorsSplitters splitters = FileEditorManagerEx.getInstanceEx(project).getSplittersFor(myStatusBar.getComponent());
        return splitters.getCurrentFile();
    }

    @Override
    public void selectionChanged( FileEditorManagerEvent event) {
        myStatusBar.updateWidget(ID());
    }
}
