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

package com.gome.maven.codeInsight.daemon.impl.actions;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInspection.InjectionAwareSuppressQuickFix;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.SuppressionUtil;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.command.undo.UndoUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.psi.PsiComment;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.PsiWhiteSpace;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.ThreeState;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author Roman.Chernyatchik
 * @date Aug 13, 2009
 */
public abstract class AbstractBatchSuppressByNoInspectionCommentFix implements InjectionAwareSuppressQuickFix, Iconable {
     protected final String myID;
    private final boolean myReplaceOtherSuppressionIds;
    private ThreeState myShouldBeAppliedToInjectionHost = ThreeState.UNSURE;

    
    public abstract PsiElement getContainer(final PsiElement context);

    /**
     * @param ID                         Inspection ID
     * @param replaceOtherSuppressionIds Merge suppression policy. If false new tool id will be append to the end
     *                                   otherwise replace other ids
     */
    public AbstractBatchSuppressByNoInspectionCommentFix( String ID, final boolean replaceOtherSuppressionIds) {
        myID = ID;
        myReplaceOtherSuppressionIds = replaceOtherSuppressionIds;
    }

    public void setShouldBeAppliedToInjectionHost(ThreeState shouldBeAppliedToInjectionHost) {
        myShouldBeAppliedToInjectionHost = shouldBeAppliedToInjectionHost;
    }

    @Override
    public ThreeState isShouldBeAppliedToInjectionHost() {
        return myShouldBeAppliedToInjectionHost;
    }

    
    @Override
    public String getName() {
        return getText();
    }

    @Override
    public Icon getIcon(int flags) {
        return AllIcons.General.InspectionsOff;
    }

    private String myText = "";
    
    public String getText() {
        return myText;
    }

    protected void setText( String text) {
        myText = text;
    }

    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public String toString() {
        return getText();
    }

    @Override
    public void applyFix( Project project,  ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getStartElement();
        if (element == null) return;
        invoke(project, element);
    }

    protected final void replaceSuppressionComment( final PsiElement comment) {
        SuppressionUtil.replaceSuppressionComment(comment, myID, myReplaceOtherSuppressionIds, getCommentLanguage(comment));
    }

    protected void createSuppression( Project project,
                                      PsiElement element,
                                      PsiElement container) throws IncorrectOperationException {
        SuppressionUtil.createSuppression(project, container, myID, getCommentLanguage(element));
    }

    /**
     * @param element quickfix target or existing comment element
     * @return language that will be used for comment creating.
     * In common case language will be the same as language of quickfix target
     */
    
    protected Language getCommentLanguage( PsiElement element) {
        return element.getLanguage();
    }

    @Override
    public boolean isAvailable( final Project project,  final PsiElement context) {
        return context.isValid() && PsiManager.getInstance(project).isInProject(context) && getContainer(context) != null;
    }

    public void invoke( final Project project,  final PsiElement element) throws IncorrectOperationException {
        if (!isAvailable(project, element)) return;
        PsiElement container = getContainer(element);
        if (container == null) return;

        if (!FileModificationService.getInstance().preparePsiElementForWrite(container)) return;

        if (replaceSuppressionComments(container)) return;

        createSuppression(project, element, container);
        UndoUtil.markPsiFileForUndo(element.getContainingFile());
    }

    protected boolean replaceSuppressionComments(PsiElement container) {
        final List<? extends PsiElement> comments = getCommentsFor(container);
        if (comments != null) {
            for (PsiElement comment : comments) {
                if (comment instanceof PsiComment && SuppressionUtil.isSuppressionComment(comment)) {
                    replaceSuppressionComment(comment);
                    return true;
                }
            }
        }
        return false;
    }

    
    protected List<? extends PsiElement> getCommentsFor( final PsiElement container) {
        final PsiElement prev = PsiTreeUtil.skipSiblingsBackward(container, PsiWhiteSpace.class);
        if (prev == null) {
            return null;
        }
        return Collections.singletonList(prev);
    }


    @Override
    
    public String getFamilyName() {
        return InspectionsBundle.message("suppress.inspection.family");
    }
}
