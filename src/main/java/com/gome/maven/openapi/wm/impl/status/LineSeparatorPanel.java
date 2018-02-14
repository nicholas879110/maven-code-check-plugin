/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.AppTopics;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.impl.SimpleDataContext;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileDocumentManagerAdapter;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.FileEditorManagerEvent;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.CustomStatusBarWidget;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.StatusBarWidget;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.LineSeparator;
import com.gome.maven.util.messages.MessageBusConnection;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author Denis Zhdanov
 * @since 3/17/13 11:56 AM
 */
public class LineSeparatorPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {

     private final TextPanel myComponent;

    private boolean myActionEnabled;

    public LineSeparatorPanel( final Project project) {
        super(project);

        myComponent = new TextPanel() {
            @Override
            protected void paintComponent( final Graphics g) {
                super.paintComponent(g);
                if (myActionEnabled && getText() != null) {
                    final Rectangle r = getBounds();
                    final Insets insets = getInsets();
                    AllIcons.Ide.Statusbar_arrows.paintIcon(this, g, r.width - insets.right - AllIcons.Ide.Statusbar_arrows.getIconWidth() - 2,
                            r.height / 2 - AllIcons.Ide.Statusbar_arrows.getIconHeight() / 2);
                }
            }
        };

        new ClickListener() {
            @Override
            public boolean onClick( MouseEvent e, int clickCount) {
                update();
                showPopup(e);
                return true;
            }
        }.installOn(myComponent);
        myComponent.setBorder(WidgetBorder.INSTANCE);
    }

    private void update() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = getSelectedFile();
                myActionEnabled = false;
                String lineSeparator = null;
                String toolTipText = null;
                String panelText = null;

                if (file != null) {
                    myActionEnabled = file.isWritable();

                    lineSeparator =
                            LoadTextUtil.detectLineSeparator(file, true);

                    if (lineSeparator != null) {
                        toolTipText = String.format("Line separator: %s",
                                StringUtil.escapeLineBreak(lineSeparator));
                        panelText = LineSeparator.fromString(lineSeparator).toString();
                    }
                }

                if (lineSeparator == null) {
                    toolTipText = "No line separator";
                    panelText = "n/a";
                    myActionEnabled = false;
                }

                myComponent.resetColor();

                String toDoComment;

                if (myActionEnabled) {
                    toDoComment = "Click to change";
                    myComponent.setForeground(UIUtil.getActiveTextColor());
                    myComponent.setTextAlignment(Component.LEFT_ALIGNMENT);
                } else {
                    toDoComment = "";
                    myComponent.setForeground(UIUtil.getInactiveTextColor());
                    myComponent.setTextAlignment(Component.CENTER_ALIGNMENT);
                }

                myComponent.setToolTipText(String.format("%s%n%s",
                        toolTipText,
                        toDoComment));
                myComponent.setText(panelText);



                if (myStatusBar != null) {
                    myStatusBar.updateWidget(ID());
                }
            }
        });
    }

    private void showPopup(MouseEvent e) {
        if (!myActionEnabled) {
            return;
        }
        DataContext dataContext = getContext();
        AnAction group = ActionManager.getInstance().getAction("ChangeLineSeparators");
        if (!(group instanceof ActionGroup)) {
            return;
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "Line separator",
                (ActionGroup)group,
                dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
        );
        Dimension dimension = popup.getContent().getPreferredSize();
        Point at = new Point(0, -dimension.height);
        popup.show(new RelativePoint(e.getComponent(), at));
        Disposer.register(this, popup); // destroy popup on unexpected project close
    }

    @Override
    public void install( StatusBar statusBar) {
        super.install(statusBar);
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerAdapter() {
            @Override
            public void fileContentReloaded( VirtualFile file,  Document document) {
                update();
            }
        });
    }

    
    private DataContext getContext() {
        Editor editor = getEditor();
        DataContext parent = DataManager.getInstance().getDataContext((Component)myStatusBar);
        return SimpleDataContext.getSimpleContext(
                CommonDataKeys.VIRTUAL_FILE_ARRAY.getName(),
                new VirtualFile[] {getSelectedFile()},
                SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT.getName(),
                        getProject(),
                        SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT.getName(),
                                editor == null ? null : editor.getComponent(), parent)
                ));
    }

    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Override
    public StatusBarWidget copy() {
        return new LineSeparatorPanel(getProject());
    }

    
    @Override
    public String ID() {
        return "LineSeparator";
    }

    
    @Override
    public WidgetPresentation getPresentation( PlatformType type) {
        return null;
    }

    @Override
    public void selectionChanged( FileEditorManagerEvent event) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        update();
    }

    @Override
    public void fileOpened( FileEditorManager source,  VirtualFile file) {
        update();
    }
}
