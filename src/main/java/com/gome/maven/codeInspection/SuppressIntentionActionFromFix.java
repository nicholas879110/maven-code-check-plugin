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
package com.gome.maven.codeInspection;

import com.gome.maven.codeInsight.daemon.impl.actions.AbstractBatchSuppressByNoInspectionCommentFix;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Function;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.containers.ContainerUtil;

public class SuppressIntentionActionFromFix extends SuppressIntentionAction {
    private final SuppressQuickFix myFix;

    private SuppressIntentionActionFromFix( SuppressQuickFix fix) {
        myFix = fix;
    }

    
    public static SuppressIntentionAction convertBatchToSuppressIntentionAction( final SuppressQuickFix fix) {
        return new SuppressIntentionActionFromFix(fix);
    }

    
    public static SuppressIntentionAction[] convertBatchToSuppressIntentionActions( SuppressQuickFix[] actions) {
        return ContainerUtil.map2Array(actions, SuppressIntentionAction.class, new Function<SuppressQuickFix, SuppressIntentionAction>() {
            @Override
            public SuppressIntentionAction fun(SuppressQuickFix fix) {
                return convertBatchToSuppressIntentionAction(fix);
            }
        });
    }

    @Override
    public void invoke( Project project, Editor editor,  PsiElement element) throws IncorrectOperationException {
        PsiElement container = getContainer(element);
        boolean caretWasBeforeStatement = editor != null && container != null && editor.getCaretModel().getOffset() == container.getTextRange().getStartOffset();
        InspectionManager inspectionManager = InspectionManager.getInstance(project);
        ProblemDescriptor descriptor = inspectionManager.createProblemDescriptor(element, element, "", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false);
        myFix.applyFix(project, descriptor);

        if (caretWasBeforeStatement) {
            editor.getCaretModel().moveToOffset(container.getTextRange().getStartOffset());
        }
    }

    public ThreeState isShouldBeAppliedToInjectionHost() {
        return myFix instanceof InjectionAwareSuppressQuickFix
                ? ((InjectionAwareSuppressQuickFix)myFix).isShouldBeAppliedToInjectionHost()
                : ThreeState.UNSURE;
    }

    public PsiElement getContainer(PsiElement element) {
        return myFix instanceof AbstractBatchSuppressByNoInspectionCommentFix
                ? ((AbstractBatchSuppressByNoInspectionCommentFix)myFix).getContainer(element) : null;
    }

    @Override
    public boolean isAvailable( Project project, Editor editor,  PsiElement element) {
        return myFix.isAvailable(project, element);
    }

    
    @Override
    public String getText() {
        return myFix.getName() + (isShouldBeAppliedToInjectionHost() == ThreeState.NO ? " in injection" : "");
    }

    
    @Override
    public String getFamilyName() {
        return myFix.getFamilyName();
    }
}
