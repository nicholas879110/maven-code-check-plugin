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

package com.gome.maven.codeInsight.actions;

import com.gome.maven.codeInsight.CodeInsightActionHandler;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.actionSystem.DocCommandGroupId;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilBase;

/**
 * @author Dmitry Avdeev
 */
public abstract class CodeInsightAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            Editor editor = getEditor(e.getDataContext(), project);
            actionPerformedImpl(project, editor);
        }
    }

    
    protected Editor getEditor( DataContext dataContext,  Project project) {
        return CommonDataKeys.EDITOR.getData(dataContext);
    }

    public void actionPerformedImpl( final Project project, final Editor editor) {
        if (editor == null) return;
        //final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) return;
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                final CodeInsightActionHandler handler = getHandler();
                final Runnable action = new Runnable() {
                    @Override
                    public void run() {
                        if (!ApplicationManager.getApplication().isUnitTestMode() && !editor.getContentComponent().isShowing()) return;
                        handler.invoke(project, editor, psiFile);
                    }
                };
                if (handler.startInWriteAction()) {
                    ApplicationManager.getApplication().runWriteAction(action);
                }
                else {
                    action.run();
                }
            }
        }, getCommandName(), DocCommandGroupId.noneGroupId(editor.getDocument()));
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        Project project = e.getProject();
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        Editor editor = getEditor(e.getDataContext(), project);
        if (editor == null) {
            presentation.setEnabled(false);
            return;
        }

        final PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            presentation.setEnabled(false);
            return;
        }

        update(presentation, project, editor, file);
    }

    protected void update( Presentation presentation,  Project project,  Editor editor,  PsiFile file) {
        presentation.setEnabled(isValidForFile(project, editor, file));
    }

    protected boolean isValidForFile( Project project,  Editor editor,  PsiFile file) {
        return true;
    }

    
    protected abstract CodeInsightActionHandler getHandler();

    protected String getCommandName() {
        String text = getTemplatePresentation().getText();
        return text == null ? "" : text;
    }
}
