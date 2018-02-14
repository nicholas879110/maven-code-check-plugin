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
package com.gome.maven.openapi.wm.impl.status;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.actionSystem.impl.SimpleDataContext;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.fileEditor.*;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileAdapter;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.VirtualFilePropertyEvent;
import com.gome.maven.openapi.vfs.encoding.ChangeFileEncodingAction;
import com.gome.maven.openapi.vfs.encoding.EncodingManager;
import com.gome.maven.openapi.vfs.encoding.EncodingManagerImpl;
import com.gome.maven.openapi.vfs.encoding.EncodingUtil;
import com.gome.maven.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.gome.maven.openapi.wm.CustomStatusBarWidget;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.StatusBarWidget;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

/**
 * @author cdr
 */
public class EncodingPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {
    private final TextPanel myComponent;
    private boolean actionEnabled;
    private final Alarm update;
    private volatile Reference<Editor> myEditor = new WeakReference<Editor>(null); // store editor here to avoid expensive and EDT-only getSelectedEditor() retrievals

    public EncodingPanel( final Project project) {
        super(project);
        update = new Alarm(this);
        myComponent = new TextPanel() {
            @Override
            protected void paintComponent( final Graphics g) {
                super.paintComponent(g);
                if (actionEnabled && getText() != null) {
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

    private static Charset cachedCharsetFromContent(final VirtualFile virtualFile) {
        if (virtualFile == null) return null;
        final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) return null;

        return EncodingManager.getInstance().getCachedCharsetFromContent(document);
    }

    @Override
    public void selectionChanged( FileEditorManagerEvent event) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        VirtualFile newFile = event.getNewFile();
        fileChanged(newFile);
    }

    private void fileChanged(VirtualFile newFile) {
        FileEditor fileEditor = newFile == null ? null : FileEditorManager.getInstance(getProject()).getSelectedEditor(newFile);
        Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : null;
        myEditor = new WeakReference<Editor>(editor);
        update();
    }

    @Override
    public void fileOpened( FileEditorManager source,  VirtualFile file) {
        fileChanged(file);
    }

    @Override
    public StatusBarWidget copy() {
        return new EncodingPanel(getProject());
    }

    @Override
    
    public String ID() {
        return "Encoding";
    }

    @Override
    public WidgetPresentation getPresentation( PlatformType type) {
        return null;
    }

    @Override
    public void install( StatusBar statusBar) {
        super.install(statusBar);
        // should update to reflect encoding-from-content
        EncodingManager.getInstance().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(EncodingManagerImpl.PROP_CACHED_ENCODING_CHANGED)) {
                    Document document = evt.getSource() instanceof Document ? (Document)evt.getSource() : null;
                    updateForDocument(document);
                }
            }
        }, this);
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new VirtualFileAdapter() {
            @Override
            public void propertyChanged( VirtualFilePropertyEvent event) {
                if (VirtualFile.PROP_ENCODING.equals(event.getPropertyName())) {
                    updateForFile(event.getFile());
                }
            }
        }));

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                Document document = e.getDocument();
                updateForDocument(document);
            }
        }, this);
    }

    private void updateForDocument( Document document) {
        Editor selectedEditor = myEditor.get();
        if (document != null && (selectedEditor == null || selectedEditor.getDocument() != document)) return;
        update();
    }

    private void updateForFile( VirtualFile file) {
        if (file == null) {
            update();
        }
        else {
            updateForDocument(FileDocumentManager.getInstance().getCachedDocument(file));
        }
    }

    private void showPopup( MouseEvent e) {
        if (!actionEnabled) {
            return;
        }
        DataContext dataContext = getContext();
        ListPopup popup = new ChangeFileEncodingAction().createPopup(dataContext);

        if (popup != null) {
            Dimension dimension = popup.getContent().getPreferredSize();
            Point at = new Point(0, -dimension.height);
            popup.show(new RelativePoint(e.getComponent(), at));
            Disposer.register(this, popup); // destroy popup on unexpected project close
        }
    }

    
    private DataContext getContext() {
        Editor editor = getEditor();
        DataContext parent = DataManager.getInstance().getDataContext((Component)myStatusBar);
        return SimpleDataContext.getSimpleContext(CommonDataKeys.VIRTUAL_FILE.getName(), getSelectedFile(),
                SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT.getName(), getProject(),
                        SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT.getName(), editor == null ? null : editor.getComponent(), parent)
                ));
    }

    private void update() {
        if (update.isDisposed()) return;

        update.cancelAllRequests();
        update.addRequest(new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;

                VirtualFile file = getSelectedFile();
                actionEnabled = false;
                String charsetName = null;
                Pair<Charset, String> check = null;

                if (file != null) {
                    check = EncodingUtil.checkSomeActionEnabled(file);
                    Charset charset = null;

                    if (LoadTextUtil.wasCharsetDetectedFromBytes(file) != null) {
                        charset = cachedCharsetFromContent(file);
                    }

                    if (charset == null) {
                        charset = file.getCharset();
                    }

                    actionEnabled = check == null || check.second == null;

                    if (!actionEnabled) {
                        charset = check.first;
                    }

                    if (charset != null) {
                        charsetName = charset.displayName();
                    }
                }

                if (charsetName == null) {
                    charsetName = "n/a";
                }

                String toolTipText;

                if (actionEnabled) {
                    toolTipText = String.format(
                            "File Encoding%n%s", charsetName);

                    myComponent.setForeground(UIUtil.getActiveTextColor());
                    myComponent.setTextAlignment(Component.LEFT_ALIGNMENT);
                }
                else {
                    String failReason = check == null ? "" : check.second;
                    toolTipText = String.format("File encoding is disabled%n%s",
                            failReason);

                    myComponent.setForeground(UIUtil.getInactiveTextColor());
                    myComponent.setTextAlignment(Component.CENTER_ALIGNMENT);
                }

                myComponent.setToolTipText(toolTipText);
                myComponent.setText(charsetName);

                if (myStatusBar != null) {
                    myStatusBar.updateWidget(ID());
                }
            }
        }, 200, ModalityState.any());
    }

    @Override
    public JComponent getComponent() {
        return myComponent;
    }
}
