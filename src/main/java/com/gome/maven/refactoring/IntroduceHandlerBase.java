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
package com.gome.maven.refactoring;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.refactoring.introduce.inplace.AbstractInplaceIntroducer;

/**
 * @author dsl
 */
public abstract class IntroduceHandlerBase implements RefactoringActionHandler {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.refactoring.IntroduceHandlerBase");

    public void invoke( Project project,  PsiElement[] elements, DataContext dataContext) {
        LOG.assertTrue(elements.length >= 1 && elements[0] instanceof PsiExpression, "incorrect invoke() parameters");
        final PsiElement tempExpr = elements[0];
        final Editor editor;
        if (dataContext != null) {
            final Editor editorFromDC = CommonDataKeys.EDITOR.getData(dataContext);
            final PsiFile cachedPsiFile = editorFromDC != null ? PsiDocumentManager.getInstance(project).getCachedPsiFile(editorFromDC.getDocument()) : null;
            if (cachedPsiFile != null && PsiTreeUtil.isAncestor(cachedPsiFile, tempExpr, false)) {
                editor = editorFromDC;
            }
            else {
                editor = null;
            }
        }
        else {
            editor = null;
        }
        if (tempExpr instanceof PsiExpression) {
            invokeImpl(project, (PsiExpression)tempExpr, editor);
        }
        else if(tempExpr instanceof PsiLocalVariable) {
            invokeImpl(project, (PsiLocalVariable)tempExpr, editor);
        }
        else {
            LOG.error("elements[0] should be PsiExpression or PsiLocalVariable");
        }
    }

    /**
     * @param project
     * @param tempExpr
     * @param editor editor to highlight stuff in. Should accept <code>null</code>
     * @return
     */
    protected abstract boolean invokeImpl(Project project, PsiExpression tempExpr,
                                          Editor editor);

    /**
     * @param project
     * @param localVariable
     * @param editor editor to highlight stuff in. Should accept <code>null</code>
     * @return
     */
    protected abstract boolean invokeImpl(Project project, PsiLocalVariable localVariable,
                                          Editor editor);


    public abstract AbstractInplaceIntroducer getInplaceIntroducer();
}
