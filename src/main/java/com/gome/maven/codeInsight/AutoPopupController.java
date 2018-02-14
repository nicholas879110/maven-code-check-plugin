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

package com.gome.maven.codeInsight;

import com.gome.maven.codeInsight.completion.CompletionPhase;
import com.gome.maven.codeInsight.completion.CompletionProgressIndicator;
import com.gome.maven.codeInsight.completion.CompletionType;
import com.gome.maven.codeInsight.completion.impl.CompletionServiceImpl;
import com.gome.maven.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.gome.maven.codeInsight.hint.ShowParameterInfoHandler;
import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.ide.PowerSaveMode;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.ex.ActionManagerEx;
import com.gome.maven.openapi.actionSystem.ex.AnActionListener;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.util.Alarm;

public class AutoPopupController implements Disposable {
    private final Project myProject;
    private final Alarm myAlarm = new Alarm();

    public static AutoPopupController getInstance(Project project){
        return ServiceManager.getService(project, AutoPopupController.class);
    }

    public AutoPopupController(Project project) {
        myProject = project;
        setupListeners();
    }

    private void setupListeners() {
        ActionManagerEx.getInstanceEx().addAnActionListener(new AnActionListener() {
            @Override
            public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                cancelAllRequest();
            }

            @Override
            public void beforeEditorTyping(char c, DataContext dataContext) {
                cancelAllRequest();
            }


            @Override
            public void afterActionPerformed(final AnAction action, final DataContext dataContext, AnActionEvent event) {
            }
        }, this);

        IdeEventQueue.getInstance().addActivityListener(new Runnable() {
            @Override
            public void run() {
                cancelAllRequest();
            }
        }, this);
    }

    public void autoPopupMemberLookup(final Editor editor,  final Condition<PsiFile> condition){
        scheduleAutoPopup(editor, condition);
    }

    public void scheduleAutoPopup(final Editor editor,  final Condition<PsiFile> condition) {
        if (ApplicationManager.getApplication().isUnitTestMode() && !CompletionAutoPopupHandler.ourTestingAutopopup) {
            return;
        }

        if (!CodeInsightSettings.getInstance().AUTO_POPUP_COMPLETION_LOOKUP) {
            return;
        }
        if (PowerSaveMode.isEnabled()) {
            return;
        }

        if (!CompletionServiceImpl.isPhase(CompletionPhase.CommittingDocuments.class, CompletionPhase.NoCompletion.getClass())) {
            return;
        }

        final CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCompletionService().getCurrentCompletion();
        if (currentCompletion != null) {
            currentCompletion.closeAndFinish(true);
        }

        final CompletionPhase.CommittingDocuments phase = new CompletionPhase.CommittingDocuments(null, editor);
        CompletionServiceImpl.setCompletionPhase(phase);
        phase.ignoreCurrentDocumentChange();

        CompletionAutoPopupHandler.runLaterWithCommitted(myProject, editor.getDocument(), new Runnable() {
            @Override
            public void run() {
                if (phase.checkExpired()) return;

                PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
                if (file != null && condition != null && !condition.value(file)) {
                    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
                    return;
                }

                CompletionAutoPopupHandler.invokeCompletion(CompletionType.BASIC, true, myProject, editor, 0, false);
            }
        });
    }

    public void scheduleAutoPopup(final Editor editor) {
        scheduleAutoPopup(editor, null);
    }

    private void addRequest(final Runnable request, final int delay) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                myAlarm.addRequest(request, delay);
            }
        };
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

    private void cancelAllRequest() {
        myAlarm.cancelAllRequests();
    }

    public void autoPopupParameterInfo( final Editor editor,  final PsiElement highlightedMethod){
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        if (DumbService.isDumb(myProject)) return;
        if (PowerSaveMode.isEnabled()) return;

        ApplicationManager.getApplication().assertIsDispatchThread();
        final CodeInsightSettings settings = CodeInsightSettings.getInstance();
        if (settings.AUTO_POPUP_PARAMETER_INFO) {
            final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
            PsiFile file = documentManager.getPsiFile(editor.getDocument());
            if (file == null) return;

            if (!documentManager.isUncommited(editor.getDocument())) {
                file = documentManager.getPsiFile(InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file).getDocument());
                if (file == null) return;
            }

            final PsiFile file1 = file;
            final Runnable request = new Runnable(){
                @Override
                public void run(){
                    if (myProject.isDisposed() || DumbService.isDumb(myProject)) return;
                    documentManager.commitAllDocuments();
                    if (editor.isDisposed() || !editor.getComponent().isShowing()) return;
                    int lbraceOffset = editor.getCaretModel().getOffset() - 1;
                    try {
                        ShowParameterInfoHandler.invoke(myProject, editor, file1, lbraceOffset, highlightedMethod);
                    }
                    catch (IndexNotReadyException ignored) { //anything can happen on alarm
                    }
                }
            };

            addRequest(request, settings.PARAMETER_INFO_DELAY);
        }
    }

    @Override
    public void dispose() {
    }
}
