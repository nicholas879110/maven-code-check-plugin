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
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.UndoConfirmationPolicy;
import com.gome.maven.openapi.command.undo.GlobalUndoableAction;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.command.undo.UndoableAction;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectLocator;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Function;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author cdr
 */
public class ChangeFileEncodingAction extends AnAction implements DumbAware {
    private final boolean allowDirectories;

    public ChangeFileEncodingAction() {
        this(false);
    }

    public ChangeFileEncodingAction(boolean allowDirectories) {
        this.allowDirectories = allowDirectories;
    }

    private boolean checkEnabled( VirtualFile virtualFile) {
        if (allowDirectories && virtualFile.isDirectory()) return true;
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        if (document == null) return false;

        return EncodingUtil.checkCanConvert(virtualFile) == null || EncodingUtil.checkCanReload(virtualFile).second == null;
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile myFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = myFile != null && checkEnabled(myFile);
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(myFile != null);
    }

    @Override
    public final void actionPerformed(final AnActionEvent e) {
        DataContext dataContext = e.getDataContext();

        ListPopup popup = createPopup(dataContext);
        if (popup != null) {
            popup.showInBestPositionFor(dataContext);
        }
    }

    
    public ListPopup createPopup( DataContext dataContext) {
        final VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        if (virtualFile == null) return null;
        boolean enabled = checkEnabled(virtualFile);
        if (!enabled) return null;
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        final Document document = documentManager.getDocument(virtualFile);
        if (!allowDirectories && virtualFile.isDirectory() || document == null && !virtualFile.isDirectory()) return null;

        final byte[] bytes;
        try {
            bytes = virtualFile.isDirectory() ? null : virtualFile.contentsToByteArray();
        }
        catch (IOException e) {
            return null;
        }
        DefaultActionGroup group = createActionGroup(virtualFile, editor, document, bytes, null);

        return JBPopupFactory.getInstance().createActionGroupPopup(getTemplatePresentation().getText(),
                group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    public DefaultActionGroup createActionGroup( final VirtualFile myFile,
                                                final Editor editor,
                                                final Document document,
                                                final byte[] bytes,
                                                 final String clearItemText) {
        final String text = document == null ? null : document.getText();

        return new ChooseFileEncodingAction(myFile) {
            @Override
            public void update(final AnActionEvent e) {
            }

            
            @Override
            protected DefaultActionGroup createPopupActionGroup(JComponent button) {
                return createCharsetsActionGroup(clearItemText, null, new Function<Charset, String>() {
                    @Override
                    public String fun(Charset charset) {
                        assert myFile.isDirectory() || text != null : charset;
                        EncodingUtil.Magic8 safeToReload = myFile.isDirectory() ? EncodingUtil.Magic8.ABSOLUTELY : EncodingUtil.isSafeToReloadIn(myFile, text, bytes, charset);
                        boolean enabled = safeToReload != EncodingUtil.Magic8.NO_WAY;
                        if (!enabled) {
                            EncodingUtil.Magic8 safeToConvert = myFile.isDirectory() ? EncodingUtil.Magic8.ABSOLUTELY : EncodingUtil.isSafeToConvertTo(myFile, text, bytes, charset);
                            enabled = safeToConvert != EncodingUtil.Magic8.NO_WAY;
                        }
                        return enabled ? "Change encoding to '"+charset.displayName()+"'" : null;
                    }
                }); // no 'clear'
            }

            @Override
            protected void chosen( VirtualFile virtualFile,  Charset charset) {
                if (virtualFile != null) {
                    ChangeFileEncodingAction.this.chosen(document, editor, virtualFile, bytes, charset);
                }
            }
        }
                .createPopupActionGroup(null);
    }

    // returns true if charset was changed, false if failed
    protected boolean chosen(final Document document,
                             final Editor editor,
                              final VirtualFile virtualFile,
                             byte[] bytes,
                              final Charset charset) {

        String text = document.getText();
        EncodingUtil.Magic8 isSafeToConvert = EncodingUtil.isSafeToConvertTo(virtualFile, text, bytes, charset);
        EncodingUtil.Magic8 isSafeToReload = EncodingUtil.isSafeToReloadIn(virtualFile, text, bytes, charset);

        final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        final Charset oldCharset = virtualFile.getCharset();
        final Runnable undo;
        final Runnable redo;

        if (isSafeToConvert == EncodingUtil.Magic8.ABSOLUTELY && isSafeToReload == EncodingUtil.Magic8.ABSOLUTELY) {
            //change and forget
            undo = new Runnable() {
                @Override
                public void run() {
                    EncodingManager.getInstance().setEncoding(virtualFile, oldCharset);
                }
            };
            redo = new Runnable() {
                @Override
                public void run() {
                    EncodingManager.getInstance().setEncoding(virtualFile, charset);
                }
            };
        }
        else {
            IncompatibleEncodingDialog dialog = new IncompatibleEncodingDialog(virtualFile, charset, isSafeToReload, isSafeToConvert);
            dialog.show();
            if (dialog.getExitCode() == IncompatibleEncodingDialog.RELOAD_EXIT_CODE) {
                undo = new Runnable() {
                    @Override
                    public void run() {
                        EncodingUtil.reloadIn(virtualFile, oldCharset);
                    }
                };
                redo = new Runnable() {
                    @Override
                    public void run() {
                        EncodingUtil.reloadIn(virtualFile, charset);
                    }
                };
            }
            else if (dialog.getExitCode() == IncompatibleEncodingDialog.CONVERT_EXIT_CODE) {
                undo = new Runnable() {
                    @Override
                    public void run() {
                        EncodingUtil.saveIn(document, editor, virtualFile, oldCharset);
                    }
                };
                redo = new Runnable() {
                    @Override
                    public void run() {
                        EncodingUtil.saveIn(document, editor, virtualFile, charset);
                    }
                };
            }
            else {
                return false;
            }
        }

        final UndoableAction action = new GlobalUndoableAction(virtualFile) {
            @Override
            public void undo() {
                // invoke later because changing document inside undo/redo is not allowed
                Application application = ApplicationManager.getApplication();
                application.invokeLater(undo, ModalityState.NON_MODAL, (project == null ? application : project).getDisposed());
            }

            @Override
            public void redo() {
                // invoke later because changing document inside undo/redo is not allowed
                Application application = ApplicationManager.getApplication();
                application.invokeLater(redo, ModalityState.NON_MODAL, (project == null ? application : project).getDisposed());
            }
        };

        redo.run();
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                UndoManager undoManager = project == null ? UndoManager.getGlobalInstance() : UndoManager.getInstance(project);
                undoManager.undoableActionPerformed(action);
            }
        }, "Change encoding for '" + virtualFile.getName() + "'", null, UndoConfirmationPolicy.REQUEST_CONFIRMATION);

        return true;
    }
}
