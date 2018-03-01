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

package com.gome.maven.ide.actions;

import com.gome.maven.CommonBundle;
import com.gome.maven.history.LocalHistory;
import com.gome.maven.history.LocalHistoryAction;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.command.UndoConfirmationPolicy;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.SmartPsiElementPointer;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;

/**
 * @author peter
 */
public abstract class ElementCreator {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.actions.ElementCreator");
    private final Project myProject;
    private final String myErrorTitle;

    protected ElementCreator(Project project, String errorTitle) {
        myProject = project;
        myErrorTitle = errorTitle;
    }

    protected abstract PsiElement[] create(String newName) throws Exception;
    protected abstract String getActionName(String newName);

    public PsiElement[] tryCreate( final String inputString) {
        if (inputString.length() == 0) {
            Messages.showMessageDialog(myProject, IdeBundle.message("error.name.should.be.specified"), CommonBundle.getErrorTitle(),
                    Messages.getErrorIcon());
            return PsiElement.EMPTY_ARRAY;
        }

        final Exception[] exception = new Exception[1];
        final SmartPsiElementPointer[][] myCreatedElements = {null};

        final String commandName = getActionName(inputString);
        new WriteCommandAction(myProject, commandName) {
            @Override
            protected void run(Result result) throws Throwable {
                LocalHistoryAction action = LocalHistoryAction.NULL;
                try {
                    action = LocalHistory.getInstance().startAction(commandName);

                    PsiElement[] psiElements = create(inputString);
                    myCreatedElements[0] = new SmartPsiElementPointer[psiElements.length];
                    SmartPointerManager manager = SmartPointerManager.getInstance(myProject);
                    for (int i = 0; i < myCreatedElements[0].length; i++) {
                        myCreatedElements[0][i] = manager.createSmartPsiElementPointer(psiElements[i]);
                    }
                }
                catch (Exception ex) {
                    exception[0] = ex;
                }
                finally {
                    action.finish();
                }
            }

            @Override
            protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
                return UndoConfirmationPolicy.REQUEST_CONFIRMATION;
            }
        }.execute();

        if (exception[0] != null) {
            LOG.info(exception[0]);
            String errorMessage = CreateElementActionBase.filterMessage(exception[0].getMessage());
            if (errorMessage == null || errorMessage.length() == 0) {
                errorMessage = exception[0].toString();
            }
            Messages.showMessageDialog(myProject, errorMessage, myErrorTitle, Messages.getErrorIcon());
            return PsiElement.EMPTY_ARRAY;
        }

        List<PsiElement> result = new SmartList<PsiElement>();
        for (final SmartPsiElementPointer pointer : myCreatedElements[0]) {
            ContainerUtil.addIfNotNull(pointer.getElement(), result);
        }
        return PsiUtilCore.toPsiElementArray(result);
    }
}
