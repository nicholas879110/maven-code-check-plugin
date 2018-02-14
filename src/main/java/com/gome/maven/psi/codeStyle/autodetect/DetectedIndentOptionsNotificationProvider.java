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
package com.gome.maven.psi.codeStyle.autodetect;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.*;
import com.gome.maven.ui.EditorNotificationPanel;
import com.gome.maven.ui.EditorNotifications;
import com.gome.maven.util.Processor;

import static com.gome.maven.psi.codeStyle.EditorNotificationInfo.ActionLabelData;

/**
 * @author Rustam Vishnyakov
 */
public class DetectedIndentOptionsNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("indent.options.notification.provider");
    private static final Key<Boolean> NOTIFIED_FLAG = Key.create("indent.options.notification.provider.status");
    protected static final Key<Boolean> DETECT_INDENT_NOTIFICATION_SHOWN_KEY = Key.create("indent.options.notification.provider.status.test.notification.shown");

    private static boolean myShowNotificationInTest = false;

    
    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    
    @Override
    public EditorNotificationPanel createNotificationPanel( final VirtualFile file,  FileEditor fileEditor) {
        Boolean notifiedFlag = fileEditor.getUserData(NOTIFIED_FLAG);
        if (fileEditor instanceof TextEditor && notifiedFlag != null) {
            final Editor editor = ((TextEditor)fileEditor).getEditor();
            final Project project = editor.getProject();
            if (project != null) {
                Document document = editor.getDocument();
                PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
                PsiFile psiFile = documentManager.getPsiFile(document);
                final Ref<FileIndentOptionsProvider> indentOptionsProviderRef = new Ref<FileIndentOptionsProvider>();
                if (psiFile != null) {
                    CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(project);
                    CommonCodeStyleSettings.IndentOptions userOptions = settings.getIndentOptions(psiFile.getFileType());
                    CommonCodeStyleSettings.IndentOptions detectedOptions = CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(
                            psiFile, null, false,
                            new Processor<FileIndentOptionsProvider>() {
                                @Override
                                public boolean process(FileIndentOptionsProvider provider) {
                                    indentOptionsProviderRef.set(provider);
                                    return false;
                                }
                            });
                    final FileIndentOptionsProvider provider = indentOptionsProviderRef.get();
                    EditorNotificationInfo info = provider != null && !provider.isAcceptedWithoutWarning(file) && !userOptions.equals(detectedOptions)
                            ? provider.getNotificationInfo(project, file, fileEditor, userOptions, detectedOptions)
                            : null;

                    if (info != null) {
                        EditorNotificationPanel panel = new EditorNotificationPanel().text(info.getTitle());
                        if (info.getIcon() != null) {
                            panel.icon(info.getIcon());
                        }
                        for (final ActionLabelData actionLabelData : info.getLabelAndActions()) {
                            Runnable onClickAction = new Runnable() {
                                @Override
                                public void run() {
                                    actionLabelData.action.run();
                                    EditorNotifications.getInstance(project).updateAllNotifications();
                                }
                            };
                            panel.createActionLabel(actionLabelData.label, onClickAction);
                        }
                        if (ApplicationManager.getApplication().isUnitTestMode()) {
                            file.putUserData(DETECT_INDENT_NOTIFICATION_SHOWN_KEY, Boolean.TRUE);
                        }
                        return panel;
                    }
                }
            }
        }
        return null;
    }

    public static void updateIndentNotification( PsiFile file, boolean enforce) {
        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) return;

        if (!ApplicationManager.getApplication().isHeadlessEnvironment()
                || ApplicationManager.getApplication().isUnitTestMode() && myShowNotificationInTest)
        {
            FileEditor fileEditor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(vFile);
            if (fileEditor != null) {
                Boolean notifiedFlag = fileEditor.getUserData(NOTIFIED_FLAG);
                if (notifiedFlag == null || enforce) {
                    fileEditor.putUserData(NOTIFIED_FLAG, Boolean.TRUE);
                    EditorNotifications.getInstance(file.getProject()).updateNotifications(vFile);
                }
            }
        }
    }

    
    static void setShowNotificationInTest(boolean show) {
        myShowNotificationInTest = show;
    }
}