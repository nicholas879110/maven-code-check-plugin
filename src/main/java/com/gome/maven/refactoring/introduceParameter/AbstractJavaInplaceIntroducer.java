package com.gome.maven.refactoring.introduceParameter;

import com.gome.maven.codeInsight.intention.impl.TypeExpression;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.template.Expression;
import com.gome.maven.codeInsight.template.ExpressionContext;
import com.gome.maven.codeInsight.template.Result;
import com.gome.maven.codeInsight.template.TextResult;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.codeStyle.VariableKind;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.refactoring.introduce.inplace.AbstractInplaceIntroducer;
import com.gome.maven.refactoring.ui.TypeSelectorManagerImpl;
import com.gome.maven.util.ArrayUtil;

/**
 * User: anna
 */
public abstract class AbstractJavaInplaceIntroducer extends AbstractInplaceIntroducer<PsiVariable, PsiExpression> {
    protected TypeSelectorManagerImpl myTypeSelectorManager;

    public AbstractJavaInplaceIntroducer(final Project project,
                                         Editor editor,
                                         PsiExpression expr,
                                         PsiVariable localVariable,
                                         PsiExpression[] occurrences,
                                         TypeSelectorManagerImpl typeSelectorManager, String title) {
        super(project, InjectedLanguageUtil.getTopLevelEditor(editor), expr, localVariable, occurrences, title, StdFileTypes.JAVA);
        myTypeSelectorManager = typeSelectorManager;
    }

    protected abstract PsiVariable createFieldToStartTemplateOn(String[] names, PsiType psiType);
    protected abstract String[] suggestNames(PsiType defaultType, String propName);
    protected abstract VariableKind getVariableKind();



    @Override
    protected String[] suggestNames(boolean replaceAll, PsiVariable variable) {
        final PsiType defaultType = getType();
        final String propertyName = variable != null
                ? JavaCodeStyleManager.getInstance(myProject).variableNameToPropertyName(variable.getName(), VariableKind.LOCAL_VARIABLE)
                : null;
        final String[] names = suggestNames(defaultType, propertyName);
        if (propertyName != null && names.length > 1) {
            final JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(myProject);
            final String paramName = javaCodeStyleManager.propertyNameToVariableName(propertyName, getVariableKind());
            final int idx = ArrayUtil.find(names, paramName);
            if (idx > -1) {
                ArrayUtil.swap(names, 0, idx);
            }
        }
        return names;
    }

    @Override
    protected PsiVariable createFieldToStartTemplateOn(boolean replaceAll, String[] names) {
        myTypeSelectorManager.setAllOccurrences(replaceAll);
        return createFieldToStartTemplateOn(names, getType());
    }

    @Override
    protected void correctExpression() {
        final PsiElement parent = getExpr().getParent();
        if (parent instanceof PsiExpressionStatement && parent.getLastChild() instanceof PsiErrorElement) {
            myExpr = ((PsiExpressionStatement)ApplicationManager.getApplication().runWriteAction(new Computable<PsiElement>() {
                @Override
                public PsiElement compute() {
                    return parent.replace(JavaPsiFacade.getElementFactory(myProject).createStatementFromText(parent.getText() + ";", parent));
                }
            })).getExpression();
            myEditor.getCaretModel().moveToOffset(myExpr.getTextRange().getStartOffset());
        }
    }

    @Override
    public PsiExpression restoreExpression(PsiFile containingFile, PsiVariable psiVariable, RangeMarker marker, String exprText) {
        return restoreExpression(containingFile, psiVariable, JavaPsiFacade.getElementFactory(myProject), marker, exprText);
    }

    @Override
    protected void restoreState( PsiVariable psiField) {
        final SmartTypePointer typePointer = SmartTypePointerManager.getInstance(myProject).createSmartTypePointer(getType());
        super.restoreState(psiField);
        for (PsiExpression occurrence : myOccurrences) {
            if (!occurrence.isValid()) return;
        }
        try {
            myTypeSelectorManager = myExpr != null
                    ? new TypeSelectorManagerImpl(myProject, typePointer.getType(), myExpr, myOccurrences)
                    : new TypeSelectorManagerImpl(myProject, typePointer.getType(), myOccurrences);
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    @Override
    protected void saveSettings( PsiVariable psiVariable) {
        TypeSelectorManagerImpl.typeSelected(psiVariable.getType(), getType());//myDefaultType.getType());
        myTypeSelectorManager = null;
    }

    public PsiType getType() {
        return myTypeSelectorManager.getDefaultType();
    }

    public static String[] appendUnresolvedExprName(String[] names, final PsiExpression expr) {
        if (expr instanceof PsiReferenceExpression && ((PsiReferenceExpression)expr).resolve() == null) {
            final String name = expr.getText();
            if (PsiNameHelper.getInstance(expr.getProject()).isIdentifier(name, LanguageLevel.HIGHEST)) {
                names = ArrayUtil.mergeArrays(new String[]{name}, names);
            }
        }
        return names;
    }

    
    public static PsiExpression restoreExpression(PsiFile containingFile,
                                                  PsiVariable psiVariable,
                                                  PsiElementFactory elementFactory,
                                                  RangeMarker marker, String exprText) {
        if (exprText == null) return null;
        if (psiVariable == null || !psiVariable.isValid()) return null;
        final PsiElement refVariableElement = containingFile.findElementAt(marker.getStartOffset());
        final PsiElement refVariableElementParent = refVariableElement != null ? refVariableElement.getParent() : null;
        PsiExpression expression = refVariableElement instanceof PsiKeyword && refVariableElementParent instanceof PsiNewExpression
                ? (PsiNewExpression)refVariableElementParent
                : refVariableElementParent instanceof PsiParenthesizedExpression
                ? ((PsiParenthesizedExpression)refVariableElementParent).getExpression()
                : PsiTreeUtil.getParentOfType(refVariableElement, PsiReferenceExpression.class);
        if (expression instanceof PsiReferenceExpression && !(expression.getParent() instanceof PsiMethodCallExpression)) {
            final String referenceName = ((PsiReferenceExpression)expression).getReferenceName();
            if (((PsiReferenceExpression)expression).resolve() == psiVariable ||
                    Comparing.strEqual(psiVariable.getName(), referenceName) ||
                    Comparing.strEqual(exprText, referenceName)) {
                return (PsiExpression)expression.replace(elementFactory.createExpressionFromText(exprText, psiVariable));
            }
        }
        if (expression == null) {
            expression = PsiTreeUtil.getParentOfType(refVariableElement, PsiExpression.class);
        }
        while (expression instanceof PsiReferenceExpression || expression instanceof PsiMethodCallExpression) {
            final PsiElement parent = expression.getParent();
            if (parent instanceof PsiMethodCallExpression) {
                if (parent.getText().equals(exprText)) return (PsiExpression)parent;
            }
            if (parent instanceof PsiExpression) {
                expression = (PsiExpression)parent;
                if (expression.getText().equals(exprText)) {
                    return expression;
                }
            } else if (expression instanceof PsiReferenceExpression) {
                return null;
            } else {
                break;
            }
        }
        if (expression != null && expression.isValid() && expression.getText().equals(exprText)) {
            return expression;
        }

        if (refVariableElementParent instanceof PsiExpression && refVariableElementParent.getText().equals(exprText)) {
            return (PsiExpression)refVariableElementParent;
        }

        return null;
    }

    public static Expression createExpression(final TypeExpression expression, final String defaultType) {
        return new Expression() {
            @Override
            public Result calculateResult(ExpressionContext context) {
                return new TextResult(defaultType);
            }

            @Override
            public Result calculateQuickResult(ExpressionContext context) {
                return new TextResult(defaultType);
            }

            @Override
            public LookupElement[] calculateLookupItems(ExpressionContext context) {
                return expression.calculateLookupItems(context);
            }

            @Override
            public String getAdvertisingText() {
                return null;
            }
        };
    }

    protected String chooseName(String[] names, Language language) {
        String inputName = getInputName();
        if (inputName != null && !isIdentifier(inputName, language)) {
            inputName = null;
        }
        return inputName != null ? inputName : names[0];
    }
}
