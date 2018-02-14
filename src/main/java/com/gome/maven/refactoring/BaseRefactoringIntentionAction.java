package com.gome.maven.refactoring;

import com.gome.maven.codeInsight.intention.HighPriorityAction;
import com.gome.maven.codeInsight.intention.PsiElementBaseIntentionAction;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.util.Iconable;

import javax.swing.*;

/**
 * User: anna
 * Date: 11/11/11
 */
public abstract class BaseRefactoringIntentionAction extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

    @Override
    public Icon getIcon(int flags) {
        return AllIcons.Actions.RefactoringBulb;
    }
}
