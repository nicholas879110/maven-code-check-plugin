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

package com.gome.maven.codeInsight.actions;

import com.gome.maven.codeInsight.lookup.Lookup;
import com.gome.maven.codeInsight.lookup.LookupManager;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;

public abstract class BaseCodeInsightAction extends CodeInsightAction {
    private final boolean myLookForInjectedEditor;

    protected BaseCodeInsightAction() {
        this(true);
    }

    protected BaseCodeInsightAction(boolean lookForInjectedEditor) {
        myLookForInjectedEditor = lookForInjectedEditor;
    }

    @Override
    
    protected Editor getEditor( final DataContext dataContext,  final Project project) {
        Editor editor = getBaseEditor(dataContext, project);
        if (!myLookForInjectedEditor) return editor;
        return getInjectedEditor(project, editor);
    }

    public static Editor getInjectedEditor( Project project, final Editor editor) {
        return getInjectedEditor(project, editor, true);
    }

    public static Editor getInjectedEditor( Project project, final Editor editor, boolean commit) {
        Editor injectedEditor = editor;
        if (editor != null) {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            PsiFile psiFile = documentManager.getCachedPsiFile(editor.getDocument());
            if (psiFile != null) {
                if (commit) documentManager.commitAllDocuments();
                injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile);
            }
        }
        return injectedEditor;
    }

    
    protected Editor getBaseEditor(final DataContext dataContext, final Project project) {
        return super.getEditor(dataContext, project);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null){
            presentation.setEnabled(false);
            return;
        }

        final Lookup activeLookup = LookupManager.getInstance(project).getActiveLookup();
        if (activeLookup != null){
            presentation.setEnabled(isValidForLookup());
        }
        else {
            super.update(event);
        }
    }

    protected boolean isValidForLookup() {
        return false;
    }
}
