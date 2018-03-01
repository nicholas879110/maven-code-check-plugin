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
package com.gome.maven.refactoring.introduceVariable;

import com.gome.maven.codeInsight.highlighting.HighlightManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.refactoring.HelpID;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.introduce.inplace.OccurrencesChooser;
import com.gome.maven.refactoring.ui.ConflictsDialog;
import com.gome.maven.refactoring.ui.TypeSelectorManagerImpl;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.util.containers.MultiMap;

import java.util.ArrayList;

public class IntroduceVariableHandler extends IntroduceVariableBase {

    public void invoke( final Project project, final Editor editor, final PsiExpression expression) {
        invokeImpl(project, expression, editor);
    }

    @Override
    public IntroduceVariableSettings getSettings(Project project, Editor editor,
                                                 PsiExpression expr, PsiExpression[] occurrences,
                                                 TypeSelectorManagerImpl typeSelectorManager,
                                                 boolean declareFinalIfAll,
                                                 boolean anyAssignmentLHS,
                                                 final InputValidator validator,
                                                 PsiElement anchor, final OccurrencesChooser.ReplaceChoice replaceChoice) {
        if (replaceChoice != null) {
            return super.getSettings(project, editor, expr, occurrences, typeSelectorManager, declareFinalIfAll, anyAssignmentLHS, validator,
                    anchor, replaceChoice);
        }
        ArrayList<RangeHighlighter> highlighters = new ArrayList<RangeHighlighter>();
        HighlightManager highlightManager = null;
        if (editor != null) {
            highlightManager = HighlightManager.getInstance(project);
            EditorColorsManager colorsManager = EditorColorsManager.getInstance();
            TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
            if (occurrences.length > 1) {
                highlightManager.addOccurrenceHighlights(editor, occurrences, attributes, true, highlighters);
            }
        }

        IntroduceVariableDialog dialog = new IntroduceVariableDialog(
                project, expr, occurrences.length, anyAssignmentLHS, declareFinalIfAll,
                typeSelectorManager,
                validator);
        if (!dialog.showAndGet()) {
            if (occurrences.length > 1) {
                WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
            }
        }
        else {
            if (editor != null) {
                for (RangeHighlighter highlighter : highlighters) {
                    highlightManager.removeSegmentHighlighter(editor, highlighter);
                }
            }
        }

        return dialog;
    }

    protected void showErrorMessage(final Project project, Editor editor, String message) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INTRODUCE_VARIABLE);
    }

    protected boolean reportConflicts(final MultiMap<PsiElement,String> conflicts, final Project project, IntroduceVariableSettings dialog) {
        ConflictsDialog conflictsDialog = new ConflictsDialog(project, conflicts);
        conflictsDialog.show();
        final boolean ok = conflictsDialog.isOK();
        if (!ok && conflictsDialog.isShowConflicts()) {
            if (dialog instanceof DialogWrapper) ((DialogWrapper)dialog).close(DialogWrapper.CANCEL_EXIT_CODE);
        }
        return ok;
    }
}
