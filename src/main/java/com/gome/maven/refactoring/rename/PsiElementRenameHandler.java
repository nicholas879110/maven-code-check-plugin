/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.refactoring.rename;

import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.ide.scratch.ScratchFileType;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ScrollType;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.meta.PsiWritableMetaData;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.actions.BaseRefactoringAction;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.usageView.UsageViewUtil;

import java.util.Arrays;

/**
 * created at Nov 13, 2001
 *
 * @author Jeka, dsl
 */
public class PsiElementRenameHandler implements RenameHandler {
    private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.rename.PsiElementRenameHandler");

    public static final ExtensionPointName<Condition<PsiElement>> VETO_RENAME_CONDITION_EP = ExtensionPointName.create("com.intellij.vetoRenameCondition");
    public static DataKey<String> DEFAULT_NAME = DataKey.create("DEFAULT_NAME");

    @Override
    public void invoke( Project project, Editor editor, PsiFile file, DataContext dataContext) {
        PsiElement element = getElement(dataContext);
        if (element == null) {
            element = BaseRefactoringAction.getElementAtCaret(editor, file);
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            final String newName = DEFAULT_NAME.getData(dataContext);
            if (newName != null) {
                rename(element, project, element, editor, newName);
                return;
            }
        }

        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        final PsiElement nameSuggestionContext = InjectedLanguageUtil.findElementAtNoCommit(file, editor.getCaretModel().getOffset());
        invoke(element, project, nameSuggestionContext, editor);
    }

    @Override
    public void invoke( Project project,  PsiElement[] elements, DataContext dataContext) {
        PsiElement element = elements.length == 1 ? elements[0] : null;
        if (element == null) element = getElement(dataContext);
        LOG.assertTrue(element != null);
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            final String newName = DEFAULT_NAME.getData(dataContext);
            LOG.assertTrue(newName != null);
            rename(element, project, element, editor, newName);
        }
        else {
            invoke(element, project, element, editor);
        }
    }

    public static void invoke(PsiElement element, Project project, PsiElement nameSuggestionContext,  Editor editor) {
        if (element != null && !canRename(project, editor, element)) {
            return;
        }

        VirtualFile contextFile = PsiUtilCore.getVirtualFile(nameSuggestionContext);

        if (nameSuggestionContext != null &&
                nameSuggestionContext.isPhysical() &&
                (contextFile == null || contextFile.getFileType() != ScratchFileType.INSTANCE) &&
                !PsiManager.getInstance(project).isInProject(nameSuggestionContext)) {
            final String message = "Selected element is used from non-project files. These usages won't be renamed. Proceed anyway?";
            if (ApplicationManager.getApplication().isUnitTestMode()) throw new CommonRefactoringUtil.RefactoringErrorHintException(message);
            if (Messages.showYesNoDialog(project, message,
                    RefactoringBundle.getCannotRefactorMessage(null), Messages.getWarningIcon()) != Messages.YES) {
                return;
            }
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed("refactoring.rename");

        rename(element, project, nameSuggestionContext, editor);
    }

    static boolean canRename(Project project, Editor editor, PsiElement element) throws CommonRefactoringUtil.RefactoringErrorHintException {
        String message = renameabilityStatus(project, element);
        if (StringUtil.isNotEmpty(message)) {
            showErrorMessage(project, editor, message);
            return false;
        }
        return true;
    }

    
    static String renameabilityStatus(Project project, PsiElement element) {
        if (element == null) return "";

        boolean hasRenameProcessor = RenamePsiElementProcessor.forElement(element) != RenamePsiElementProcessor.DEFAULT;
        boolean hasWritableMetaData = element instanceof PsiMetaOwner && ((PsiMetaOwner)element).getMetaData() instanceof PsiWritableMetaData;

        if (!hasRenameProcessor && !hasWritableMetaData && !(element instanceof PsiNamedElement)) {
            return RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.symbol.to.rename"));
        }

        if (!PsiManager.getInstance(project).isInProject(element)) {
            if (element.isPhysical()) {
                VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
                if (!(virtualFile != null && NonProjectFileWritingAccessProvider.isWriteAccessAllowedExplicitly(virtualFile, project))) {
                    String message = RefactoringBundle.message("error.out.of.project.element", UsageViewUtil.getType(element));
                    return RefactoringBundle.getCannotRefactorMessage(message);
                }
            }

            if (!element.isWritable()) {
                return RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.cannot.be.renamed"));
            }
        }

        if (InjectedLanguageUtil.isInInjectedLanguagePrefixSuffix(element)) {
            final String message = RefactoringBundle.message("error.in.injected.lang.prefix.suffix", UsageViewUtil.getType(element));
            return RefactoringBundle.getCannotRefactorMessage(message);
        }

        return null;
    }

    static void showErrorMessage(Project project,  Editor editor, String message) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("rename.title"), null);
    }

    public static void rename(PsiElement element, final Project project, PsiElement nameSuggestionContext, Editor editor) {
        rename(element, project, nameSuggestionContext, editor, null);
    }

    public static void rename(PsiElement element, final Project project, PsiElement nameSuggestionContext, Editor editor, String defaultName) {
        RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(element);
        PsiElement substituted = processor.substituteElementToRename(element, editor);
        if (substituted == null || !canRename(project, editor, substituted)) return;

        RenameDialog dialog = processor.createRenameDialog(project, substituted, nameSuggestionContext, editor);

        if (defaultName == null && ApplicationManager.getApplication().isUnitTestMode()) {
            String[] strings = dialog.getSuggestedNames();
            if (strings != null && strings.length > 0) {
                Arrays.sort(strings);
                defaultName = strings[0];
            } else {
                defaultName = "undefined"; // need to avoid show dialog in test
            }
        }

        if (defaultName != null) {
            try {
                dialog.performRename(defaultName);
            }
            finally {
                dialog.close(DialogWrapper.CANCEL_EXIT_CODE); // to avoid dialog leak
            }
        }
        else {
            dialog.show();
        }
    }

    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        return !isVetoed(getElement(dataContext));
    }

    public static boolean isVetoed(PsiElement element) {
        if (element == null || element instanceof SyntheticElement) return true;
        for(Condition<PsiElement> condition: Extensions.getExtensions(VETO_RENAME_CONDITION_EP)) {
            if (condition.value(element)) return true;
        }
        return false;
    }

    
    public static PsiElement getElement(final DataContext dataContext) {
        PsiElement[] elementArray = BaseRefactoringAction.getPsiElementArray(dataContext);

        if (elementArray.length != 1) {
            return null;
        }
        return elementArray[0];
    }

    @Override
    public boolean isRenaming(DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }
}
