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

package com.gome.maven.find.actions;

import com.gome.maven.CommonBundle;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.navigation.actions.GotoDeclarationAction;
import com.gome.maven.find.FindBundle;
import com.gome.maven.find.FindManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.search.PsiElementProcessor;
import com.gome.maven.usages.PsiElementUsageTarget;
import com.gome.maven.usages.UsageTarget;
import com.gome.maven.usages.UsageView;

public class FindUsagesAction extends AnAction {
    public FindUsagesAction() {
        setInjectedContext(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        UsageTarget[] usageTargets = e.getData(UsageView.USAGE_TARGETS_KEY);
        if (usageTargets == null) {
            final Editor editor = e.getData(CommonDataKeys.EDITOR);
            chooseAmbiguousTargetAndPerform(project, editor, new PsiElementProcessor<PsiElement>() {
                @Override
                public boolean execute( final PsiElement element) {
                    startFindUsages(element);
                    return false;
                }
            });
        }
        else {
            UsageTarget target = usageTargets[0];
            if (target instanceof PsiElementUsageTarget) {
                PsiElement element = ((PsiElementUsageTarget)target).getElement();
                if (element != null) {
                    startFindUsages(element);
                }
            } else {
                target.findUsages();
            }
        }
    }

    protected void startFindUsages( PsiElement element) {
        FindManager.getInstance(element.getProject()).findUsages(element);
    }

    @Override
    public void update(AnActionEvent event){
        FindUsagesInFileAction.updateFindUsagesAction(event);
    }

    static void chooseAmbiguousTargetAndPerform( final Project project,
                                                final Editor editor,
                                                 PsiElementProcessor<PsiElement> processor) {
        if (editor == null) {
            Messages.showMessageDialog(project, FindBundle.message("find.no.usages.at.cursor.error"), CommonBundle.getErrorTitle(),
                    Messages.getErrorIcon());
        }
        else {
            int offset = editor.getCaretModel().getOffset();
            boolean chosen = GotoDeclarationAction.chooseAmbiguousTarget(editor, offset, processor, FindBundle.message("find.usages.ambiguous.title"), null);
            if (!chosen) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (editor.isDisposed() || !editor.getComponent().isShowing()) return;
                        HintManager.getInstance().showErrorHint(editor, FindBundle.message("find.no.usages.at.cursor.error"));
                    }
                }, project.getDisposed());
            }
        }
    }

    public static class ShowSettingsAndFindUsages extends FindUsagesAction {
        @Override
        protected void startFindUsages( PsiElement element) {
            FindManager.getInstance(element.getProject()).findUsages(element, true);
        }
    }
}
