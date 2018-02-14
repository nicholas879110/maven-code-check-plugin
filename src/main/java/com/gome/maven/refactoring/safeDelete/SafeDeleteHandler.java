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

package com.gome.maven.refactoring.safeDelete;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ScrollType;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.refactoring.RefactoringActionHandler;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.RefactoringSettings;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author dsl
 */
public class SafeDeleteHandler implements RefactoringActionHandler {
    public static final String REFACTORING_NAME = RefactoringBundle.message("safe.delete.title");

    @Override
    public void invoke( Project project, Editor editor, PsiFile file, DataContext dataContext) {
        PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        if (element == null || !SafeDeleteProcessor.validElement(element)) {
            String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("is.not.supported.in.the.current.context", REFACTORING_NAME));
            CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, "refactoring.safeDelete");
            return;
        }
        invoke(project, new PsiElement[]{element}, dataContext);
    }

    @Override
    public void invoke( final Project project,  PsiElement[] elements, DataContext dataContext) {
        invoke(project, elements, LangDataKeys.MODULE.getData(dataContext), true, null);
    }

    public static void invoke(final Project project, PsiElement[] elements, boolean checkDelegates) {
        invoke(project, elements, checkDelegates, null);
    }

    public static void invoke(final Project project, PsiElement[] elements, boolean checkDelegates,  final Runnable successRunnable) {
        invoke(project, elements, null, checkDelegates, successRunnable);
    }

    public static void invoke(final Project project, PsiElement[] elements,  Module module, boolean checkDelegates,  final Runnable successRunnable) {
        for (PsiElement element : elements) {
            if (!SafeDeleteProcessor.validElement(element)) {
                return;
            }
        }
        final PsiElement[] temptoDelete = PsiTreeUtil.filterAncestors(elements);
        Set<PsiElement> elementsSet = new HashSet<PsiElement>(Arrays.asList(temptoDelete));
        Set<PsiElement> fullElementsSet = new LinkedHashSet<PsiElement>();

        if (checkDelegates) {
            for (PsiElement element : temptoDelete) {
                boolean found = false;
                for(SafeDeleteProcessorDelegate delegate: Extensions.getExtensions(SafeDeleteProcessorDelegate.EP_NAME)) {
                    if (delegate.handlesElement(element)) {
                        found = true;
                        Collection<? extends PsiElement> addElements = delegate instanceof SafeDeleteProcessorDelegateBase
                                ? ((SafeDeleteProcessorDelegateBase)delegate).getElementsToSearch(element, module, elementsSet)
                                : delegate.getElementsToSearch(element, elementsSet);
                        if (addElements == null) return;
                        fullElementsSet.addAll(addElements);
                        break;
                    }
                }
                if (!found) {
                    fullElementsSet.add(element);
                }
            }
        } else {
            ContainerUtil.addAll(fullElementsSet, temptoDelete);
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, fullElementsSet, true)) return;

        final PsiElement[] elementsToDelete = PsiUtilCore.toPsiElementArray(fullElementsSet);

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            RefactoringSettings settings = RefactoringSettings.getInstance();
            SafeDeleteProcessor.createInstance(project, null, elementsToDelete, settings.SAFE_DELETE_SEARCH_IN_COMMENTS,
                    settings.SAFE_DELETE_SEARCH_IN_NON_JAVA, true).run();
            if (successRunnable != null) successRunnable.run();
        }
        else {
            final SafeDeleteDialog.Callback callback = new SafeDeleteDialog.Callback() {
                @Override
                public void run(final SafeDeleteDialog dialog) {
                    SafeDeleteProcessor.createInstance(project, new Runnable() {
                        @Override
                        public void run() {
                            if (successRunnable != null) {
                                successRunnable.run();
                            }
                            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
                        }
                    }, elementsToDelete, dialog.isSearchInComments(), dialog.isSearchForTextOccurences(), true).run();
                }

            };

            SafeDeleteDialog dialog = new SafeDeleteDialog(project, elementsToDelete, callback);
            dialog.show();
        }
    }
}
