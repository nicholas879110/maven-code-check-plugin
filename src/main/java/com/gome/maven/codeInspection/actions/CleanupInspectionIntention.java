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

package com.gome.maven.codeInspection.actions;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.intention.EmptyIntentionAction;
import com.gome.maven.codeInsight.intention.HighPriorityAction;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.ex.LocalInspectionToolWrapper;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: anna
 * Date: 21-Feb-2006
 */
public class CleanupInspectionIntention implements IntentionAction, HighPriorityAction {
    private final InspectionToolWrapper myToolWrapper;
    private final Class myQuickfixClass;
    private final String myText;

    public CleanupInspectionIntention( InspectionToolWrapper toolWrapper,  Class quickFixClass, String text) {
        myToolWrapper = toolWrapper;
        myQuickfixClass = quickFixClass;
        myText = text;
    }

    @Override
    
    public String getText() {
        return InspectionsBundle.message("fix.all.inspection.problems.in.file", myToolWrapper.getDisplayName());
    }

    @Override
    
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke( final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) return;
        final List<ProblemDescriptor> descriptions =
                ProgressManager.getInstance().runProcess(new Computable<List<ProblemDescriptor>>() {
                    @Override
                    public List<ProblemDescriptor> compute() {
                        InspectionManager inspectionManager = InspectionManager.getInstance(project);
                        return InspectionEngine.runInspectionOnFile(file, myToolWrapper, inspectionManager.createNewGlobalContext(false));
                    }
                }, new EmptyProgressIndicator());

        Collections.sort(descriptions, new Comparator<CommonProblemDescriptor>() {
            @Override
            public int compare(final CommonProblemDescriptor o1, final CommonProblemDescriptor o2) {
                final ProblemDescriptorBase d1 = (ProblemDescriptorBase)o1;
                final ProblemDescriptorBase d2 = (ProblemDescriptorBase)o2;
                return d2.getTextRange().getStartOffset() - d1.getTextRange().getStartOffset();
            }
        });
        boolean applicableFixFound = false;
        for (final ProblemDescriptor descriptor : descriptions) {
            final QuickFix[] fixes = descriptor.getFixes();
            if (fixes != null && fixes.length > 0) {
                for (final QuickFix<CommonProblemDescriptor> fix : fixes) {
                    if (fix != null && fix.getClass().isAssignableFrom(myQuickfixClass)) {
                        final PsiElement element = descriptor.getPsiElement();
                        if (element != null && element.isValid()) {
                            applicableFixFound = true;
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    fix.applyFix(project, descriptor);
                                }
                            });
                            PsiDocumentManager.getInstance(project).commitAllDocuments();
                        }
                        break;
                    }
                }
            }
        }

        if (!applicableFixFound) {
            HintManager.getInstance().showErrorHint(editor, "Unfortunately '" + myText + "' is currently not available for batch mode");
        }
    }

    @Override
    public boolean isAvailable( final Project project, final Editor editor, final PsiFile file) {
        return myQuickfixClass != EmptyIntentionAction.class &&
                !(myToolWrapper instanceof LocalInspectionToolWrapper && ((LocalInspectionToolWrapper)myToolWrapper).isUnfair());
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
