/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.codeInspection;

import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

/**
 * Created by IntelliJ IDEA.
 * User: angus
 * Date: 4/20/11
 * Time: 9:27 PM
 */
public class IntentionWrapper implements LocalQuickFix, IntentionAction, ActionClassHolder {
    private final IntentionAction myAction;
    private final PsiFile myFile;

    public IntentionWrapper( IntentionAction action,  PsiFile file) {
        myAction = action;
        myFile = file;
    }

    
    @Override
    public String getName() {
        return myAction.getText();
    }

    
    @Override
    public String getText() {
        return myAction.getText();
    }

    
    @Override
    public String getFamilyName() {
        return myAction.getFamilyName();
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return myAction.isAvailable(project, editor, file);
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        myAction.invoke(project, editor, file);
    }

    @Override
    public boolean startInWriteAction() {
        return myAction.startInWriteAction();
    }

    
    public IntentionAction getAction() {
        return myAction;
    }

    @Override
    public void applyFix( Project project,  ProblemDescriptor descriptor) {
        VirtualFile virtualFile = myFile.getVirtualFile();

        if (virtualFile != null) {
            FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
            myAction.invoke(project, editor instanceof TextEditor ? ((TextEditor) editor).getEditor() : null, myFile);
        }
    }

    
    @Override
    public Class getActionClass() {
        return getAction().getClass();
    }
}

