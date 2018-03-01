/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.codeInsight.intention.impl.TypeExpression;
import com.gome.maven.codeInsight.template.TemplateBuilderImpl;
import com.gome.maven.openapi.actionSystem.Shortcut;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.ScrollType;
import com.gome.maven.openapi.keymap.Keymap;
import com.gome.maven.openapi.keymap.KeymapManager;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.codeStyle.VariableKind;
import com.gome.maven.psi.scope.processor.VariablesProcessor;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.refactoring.JavaRefactoringSettings;
import com.gome.maven.refactoring.RefactoringActionHandler;
import com.gome.maven.refactoring.introduceParameter.AbstractJavaInplaceIntroducer;
import com.gome.maven.refactoring.rename.ResolveSnapshotProvider;
import com.gome.maven.refactoring.rename.inplace.VariableInplaceRenamer;
import com.gome.maven.refactoring.ui.TypeSelectorManagerImpl;
import com.gome.maven.refactoring.util.RefactoringUtil;
import com.gome.maven.ui.NonFocusableCheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * User: anna
 * Date: 12/8/10
 */
public class JavaVariableInplaceIntroducer extends AbstractJavaInplaceIntroducer {

    private SmartPsiElementPointer<PsiDeclarationStatement> myPointer;

    private JCheckBox myCanBeFinalCb;
    private IntroduceVariableSettings mySettings;
    private SmartPsiElementPointer<PsiElement> myChosenAnchor;
    private final boolean myCantChangeFinalModifier;
    private boolean myHasTypeSuggestion;
    private ResolveSnapshotProvider.ResolveSnapshot myConflictResolver;
    private TypeExpression myExpression;
    private boolean myReplaceSelf;
    private boolean myDeleteSelf = true;
    private boolean mySkipTypeExpressionOnStart;

    public JavaVariableInplaceIntroducer(final Project project,
                                         IntroduceVariableSettings settings, PsiElement chosenAnchor, final Editor editor,
                                         final PsiExpression expr,
                                         final boolean cantChangeFinalModifier,
                                         final PsiExpression[] occurrences,
                                         final TypeSelectorManagerImpl selectorManager,
                                         final String title) {
        super(project, editor, RefactoringUtil.outermostParenthesizedExpression(expr), null, occurrences, selectorManager, title);
        mySettings = settings;
        myChosenAnchor = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(chosenAnchor);
        myCantChangeFinalModifier = cantChangeFinalModifier;
        myHasTypeSuggestion = selectorManager.getTypesForAll().length > 1;
        myTitle = title;
        myExpression = new TypeExpression(myProject, isReplaceAllOccurrences()
                ? myTypeSelectorManager.getTypesForAll()
                : myTypeSelectorManager.getTypesForOne());

        final List<RangeMarker> rangeMarkers = getOccurrenceMarkers();
        editor.putUserData(ReassignVariableUtil.OCCURRENCES_KEY,
                rangeMarkers.toArray(new RangeMarker[rangeMarkers.size()]));
        myReplaceSelf = myExpr.getParent() instanceof PsiExpressionStatement;
        mySkipTypeExpressionOnStart = !(myExpr instanceof PsiFunctionalExpression && myReplaceSelf);
    }

    @Override
    protected void beforeTemplateStart() {
        if (!mySkipTypeExpressionOnStart) {
            final PsiVariable variable = getVariable();
            final PsiTypeElement typeElement = variable != null ? variable.getTypeElement() : null;
            if (typeElement != null) {
                myEditor.getCaretModel().moveToOffset(typeElement.getTextOffset());
            }
        }
        super.beforeTemplateStart();
        final ResolveSnapshotProvider resolveSnapshotProvider = VariableInplaceRenamer.INSTANCE.forLanguage(myScope.getLanguage());
        myConflictResolver = resolveSnapshotProvider != null ? resolveSnapshotProvider.createSnapshot(myScope) : null;
    }

    
    protected PsiVariable getVariable() {
        final PsiDeclarationStatement declarationStatement = myPointer != null ? myPointer.getElement() : null;
        if (declarationStatement != null) {
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
            return declaredElements.length == 0 ? null : (PsiVariable)declaredElements[0];
        }
        return null;
    }

    @Override
    protected String getActionName() {
        return "IntroduceVariable";
    }

    @Override
    protected String getRefactoringId() {
        return "refactoring.extractVariable";
    }

    @Override
    protected void restoreState( PsiVariable psiField) {
        if (myDeleteSelf) return;
        super.restoreState(psiField);
    }

    @Override
    protected boolean ensureValid() {
        final PsiVariable variable = getVariable();
        return variable != null && isIdentifier(getInputName(), variable.getLanguage());
    }

    @Override
    protected void performCleanup() {
        super.performCleanup();
        PsiVariable variable = getVariable();
        if (variable != null) {
            super.restoreState(variable);
        }
    }

    @Override
    protected void deleteTemplateField(PsiVariable variable) {
        if (!myDeleteSelf) return;
        if (myReplaceSelf) {
            variable.replace(variable.getInitializer());
        } else {
            super.deleteTemplateField(variable);
        }
    }

    @Override
    protected PsiExpression getBeforeExpr() {
        final PsiVariable variable = getVariable();
        if (variable != null) {
            return variable.getInitializer();
        }
        return super.getBeforeExpr();
    }

    @Override
    protected void performIntroduce() {
        final PsiVariable psiVariable = getVariable();
        if (psiVariable == null) {
            return;
        }

        TypeSelectorManagerImpl.typeSelected(psiVariable.getType(), myTypeSelectorManager.getDefaultType());
        if (myCanBeFinalCb != null) {
            JavaRefactoringSettings.getInstance().INTRODUCE_LOCAL_CREATE_FINALS = psiVariable.hasModifierProperty(PsiModifier.FINAL);
        }

        final Document document = myEditor.getDocument();
        LOG.assertTrue(psiVariable.isValid());
        adjustLine(psiVariable, document);

        int startOffset = getExprMarker() != null && getExprMarker().isValid() ? getExprMarker().getStartOffset() : psiVariable.getTextOffset();
        final PsiFile file = psiVariable.getContainingFile();
        final PsiReference referenceAt = file.findReferenceAt(startOffset);
        if (referenceAt != null && referenceAt.resolve() instanceof PsiVariable) {
            startOffset = referenceAt.getElement().getTextRange().getEndOffset();
        }
        else {
            final PsiDeclarationStatement declarationStatement = PsiTreeUtil.getParentOfType(psiVariable, PsiDeclarationStatement.class);
            if (declarationStatement != null) {
                startOffset = declarationStatement.getTextRange().getEndOffset();
            }
        }

        myEditor.getCaretModel().moveToOffset(startOffset);
        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                if (psiVariable.getInitializer() != null) {
                    appendTypeCasts(getOccurrenceMarkers(), file, myProject, psiVariable);
                }
                if (myConflictResolver != null && myInsertedName != null && isIdentifier(myInsertedName, psiVariable.getLanguage())) {
                    myConflictResolver.apply(psiVariable.getName());
                }
            }
        });
    }

    @Override
    public boolean isReplaceAllOccurrences() {
        return mySettings.isReplaceAllOccurrences();
    }

    @Override
    public void setReplaceAllOccurrences(boolean allOccurrences) {}

    @Override
    protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return handler instanceof IntroduceVariableHandler && super.startsOnTheSameElement(handler, element);
    }

    
    protected JComponent getComponent() {
        if (!myCantChangeFinalModifier) {
            myCanBeFinalCb = new NonFocusableCheckBox("Declare final");
            myCanBeFinalCb.setSelected(createFinals());
            myCanBeFinalCb.setMnemonic('f');
            final FinalListener finalListener = new FinalListener(myEditor);
            myCanBeFinalCb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
                        @Override
                        protected void run(Result result) throws Throwable {
                            PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());
                            final PsiVariable variable = getVariable();
                            if (variable != null) {
                                finalListener.perform(myCanBeFinalCb.isSelected(), variable);
                            }
                        }
                    }.execute();
                }
            });
        } else {
            return null;
        }
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(null);

        if (myCanBeFinalCb != null) {
            panel.add(myCanBeFinalCb, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        }

        panel.add(Box.createVerticalBox(), new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));

        return panel;
    }

    protected void addAdditionalVariables(TemplateBuilderImpl builder) {
        final PsiVariable variable = getVariable();
        if (variable != null) {
            final PsiTypeElement typeElement = variable.getTypeElement();
            if (typeElement != null) {
                builder.replaceElement(typeElement, "Variable_Type", AbstractJavaInplaceIntroducer.createExpression(myExpression, typeElement.getText()), true, mySkipTypeExpressionOnStart);
            }
        }
    }

    @Override
    protected void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
        if (isReplaceAllOccurrences()) {
            for (PsiExpression expression : getOccurrences()) {
                LOG.assertTrue(expression.isValid(), expression.getText());
                stringUsages.add(Pair.<PsiElement, TextRange>create(expression, new TextRange(0, expression.getTextLength())));
            }
        } else if (getExpr() != null && !myReplaceSelf) {
            final PsiExpression expr = getExpr();
            LOG.assertTrue(expr.isValid(), expr.getText());
            stringUsages.add(Pair.<PsiElement, TextRange>create(expr, new TextRange(0, expr.getTextLength())));
        }
    }

    @Override
    protected void addReferenceAtCaret(Collection<PsiReference> refs) {
        if (!isReplaceAllOccurrences() && getExpr() == null && !myReplaceSelf) {
            return;
        }
        super.addReferenceAtCaret(refs);
    }

    private static void appendTypeCasts(List<RangeMarker> occurrenceMarkers,
                                        PsiFile file,
                                        Project project,
                                         PsiVariable psiVariable) {
        if (occurrenceMarkers != null) {
            for (RangeMarker occurrenceMarker : occurrenceMarkers) {
                final PsiElement refVariableElement = file.findElementAt(occurrenceMarker.getStartOffset());
                final PsiReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(refVariableElement, PsiReferenceExpression.class);
                if (referenceExpression != null) {
                    final PsiElement parent = referenceExpression.getParent();
                    if (parent instanceof PsiVariable) {
                        createCastInVariableDeclaration(project, (PsiVariable)parent);
                    }
                    else if (parent instanceof PsiReferenceExpression && psiVariable != null) {
                        final PsiExpression initializer = psiVariable.getInitializer();
                        LOG.assertTrue(initializer != null);
                        final PsiType type = initializer.getType();
                        if (((PsiReferenceExpression)parent).resolve() == null && type != null) {
                            final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                            final PsiExpression castedExpr =
                                    elementFactory.createExpressionFromText("((" + type.getCanonicalText() + ")" + referenceExpression.getText() + ")", parent);
                            JavaCodeStyleManager.getInstance(project).shortenClassReferences(referenceExpression.replace(castedExpr));
                        }
                    }
                }
            }
        }
        if (psiVariable != null && psiVariable.isValid()) {
            createCastInVariableDeclaration(project, psiVariable);
        }
    }

    private static void createCastInVariableDeclaration(Project project, PsiVariable psiVariable) {
        final PsiExpression initializer = psiVariable.getInitializer();
        LOG.assertTrue(initializer != null);
        final PsiType type = psiVariable.getType();
        final PsiType initializerType = initializer.getType();
        if (initializerType != null && !TypeConversionUtil.isAssignable(type, initializerType)) {
            final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
            final PsiExpression castExpr =
                    elementFactory.createExpressionFromText("(" + psiVariable.getType().getCanonicalText() + ")" + initializer.getText(), psiVariable);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(initializer.replace(castExpr));
        }
    }

    
    private static String getAdvertisementText(final PsiDeclarationStatement declaration,
                                               final PsiType type,
                                               final boolean hasTypeSuggestion) {
        final VariablesProcessor processor = ReassignVariableUtil.findVariablesOfType(declaration, type);
        final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (processor.size() > 0) {
            final Shortcut[] shortcuts = keymap.getShortcuts("IntroduceVariable");
            if (shortcuts.length > 0) {
                return "Press " + KeymapUtil.getShortcutText(shortcuts[0]) + " to reassign existing variable";
            }
        }
        if (hasTypeSuggestion) {
            final Shortcut[] shortcuts = keymap.getShortcuts("PreviousTemplateVariable");
            if  (shortcuts.length > 0) {
                return "Press " + KeymapUtil.getShortcutText(shortcuts[0]) + " to change type";
            }
        }
        return null;
    }


    protected boolean createFinals() {
        return IntroduceVariableBase.createFinals(myProject);
    }

    public static void adjustLine(final PsiVariable psiVariable, final Document document) {
        final int modifierListOffset = psiVariable.getTextRange().getStartOffset();
        final int varLineNumber = document.getLineNumber(modifierListOffset);

        ApplicationManager.getApplication().runWriteAction(new Runnable() { //adjust line indent if final was inserted and then deleted

            public void run() {
                PsiDocumentManager.getInstance(psiVariable.getProject()).doPostponedOperationsAndUnblockDocument(document);
                CodeStyleManager.getInstance(psiVariable.getProject()).adjustLineIndent(document, document.getLineStartOffset(varLineNumber));
            }
        });
    }

    @Override
    protected PsiVariable createFieldToStartTemplateOn(String[] names, PsiType psiType) {
        final PsiVariable variable = ApplicationManager.getApplication().runWriteAction(
                IntroduceVariableBase.introduce(myProject, myExpr, myEditor, myChosenAnchor.getElement(), getOccurrences(), mySettings));
        PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myEditor.getDocument());
        final PsiDeclarationStatement declarationStatement = PsiTreeUtil.getParentOfType(variable, PsiDeclarationStatement.class);
        myPointer = declarationStatement != null ? SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(declarationStatement) : null;
        myEditor.putUserData(ReassignVariableUtil.DECLARATION_KEY, myPointer);
        setAdvertisementText(getAdvertisementText(declarationStatement, variable.getType(), myHasTypeSuggestion));
        final PsiIdentifier identifier = variable.getNameIdentifier();
        if (identifier != null) {
            myEditor.getCaretModel().moveToOffset(identifier.getTextOffset());
        }
        try {
            myDeleteSelf = false;
            restoreState(variable);
        }
        finally {
            myDeleteSelf = true;
        }
        initOccurrencesMarkers();
        return variable;
    }

    @Override
    protected int getCaretOffset() {
        final PsiVariable variable = getVariable();
        if (variable != null) {
            final PsiIdentifier identifier = variable.getNameIdentifier();
            if (identifier != null) {
                return identifier.getTextOffset();
            }
        }
        return super.getCaretOffset();
    }

    @Override
    public void finish(boolean success) {
        super.finish(success);
        myEditor.putUserData(ReassignVariableUtil.DECLARATION_KEY, null);
    }

    @Override
    protected String[] suggestNames(PsiType defaultType, String propName) {
        return IntroduceVariableBase.getSuggestedName(defaultType, myExpr).names;
    }

    @Override
    protected VariableKind getVariableKind() {
        return VariableKind.LOCAL_VARIABLE;
    }
}
