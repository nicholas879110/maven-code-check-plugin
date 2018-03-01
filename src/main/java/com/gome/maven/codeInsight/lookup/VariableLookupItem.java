package com.gome.maven.codeInsight.lookup;

import com.gome.maven.codeInsight.AutoPopupController;
import com.gome.maven.codeInsight.TailType;
import com.gome.maven.codeInsight.completion.*;
import com.gome.maven.codeInsight.daemon.impl.JavaColorProvider;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightControlFlowUtil;
import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.RecursionManager;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.controlFlow.ControlFlowUtil;
import com.gome.maven.psi.impl.source.PostprocessReformattingAspect;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.ColorIcon;

import java.awt.*;
import java.util.Collection;

/**
 * @author peter
 */
public class VariableLookupItem extends LookupItem<PsiVariable> implements TypedLookupItem, StaticallyImportable {
     private final MemberLookupHelper myHelper;
    private Color myColor;

    public VariableLookupItem(PsiVariable var) {
        super(var, var.getName());
        myHelper = null;
        myColor = getInitializerColor(var);
    }

    public VariableLookupItem(PsiField field, boolean shouldImport) {
        super(field, field.getName());
        myHelper = new MemberLookupHelper(field, field.getContainingClass(), shouldImport, false);
        if (!shouldImport) {
            forceQualify();
        }
        myColor = getInitializerColor(field);
    }

    
    private static Color getInitializerColor( PsiVariable var) {
        if (!JavaColorProvider.isColorType(var.getType())) {
            return null;
        }

        PsiElement navigationElement = var.getNavigationElement();
        if (navigationElement instanceof PsiVariable) {
            var = (PsiVariable)navigationElement;
        }
        return getExpressionColor(var.getInitializer());
    }

    
    private static Color getExpressionColor( PsiExpression expression) {
        if (expression instanceof PsiReferenceExpression) {
            final PsiElement target = ((PsiReferenceExpression)expression).resolve();
            if (target instanceof PsiVariable) {
                return RecursionManager.doPreventingRecursion(expression, true, new Computable<Color>() {
                    @Override
                    public Color compute() {
                        return getExpressionColor(((PsiVariable)target).getInitializer());
                    }
                });
            }
        }
        return JavaColorProvider.getJavaColorFromExpression(expression);
    }

    @Override
    public PsiType getType() {
        return getSubstitutor().substitute(getObject().getType());
    }

    
    public PsiSubstitutor getSubstitutor() {
        final PsiSubstitutor substitutor = (PsiSubstitutor)getAttribute(LookupItem.SUBSTITUTOR);
        return substitutor == null ? PsiSubstitutor.EMPTY : substitutor;
    }

    public void setSubstitutor( PsiSubstitutor substitutor) {
        setAttribute(SUBSTITUTOR, substitutor);
    }

    @Override
    public void setShouldBeImported(boolean shouldImportStatic) {
        assert myHelper != null;
        myHelper.setShouldBeImported(shouldImportStatic);
    }

    @Override
    public boolean canBeImported() {
        return myHelper != null;
    }

    @Override
    public boolean willBeImported() {
        return myHelper != null && myHelper.willBeImported();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        super.renderElement(presentation);
        if (myHelper != null) {
            myHelper.renderElement(presentation, getAttribute(FORCE_QUALIFY) != null ? Boolean.TRUE : null, getSubstitutor());
        }
        if (myColor != null) {
            presentation.setTypeText("", new ColorIcon(12, myColor));
        }
    }

    @Override
    public LookupItem<PsiVariable> forceQualify() {
        PsiVariable var = getObject();
        if (var instanceof PsiField) {
            for (String s : JavaCompletionUtil.getAllLookupStrings((PsiField)var)) {
                setLookupString(s); //todo set the string that will be inserted
            }
        }
        return super.forceQualify();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        PsiVariable variable = getObject();

        Document document = context.getDocument();
        document.replaceString(context.getStartOffset(), context.getTailOffset(), variable.getName());
        context.commitDocument();

        if (variable instanceof PsiField) {
            if (willBeImported()) {
                RangeMarker toDelete = JavaCompletionUtil.insertTemporary(context.getTailOffset(), document, " ");
                context.commitDocument();
                final PsiReferenceExpression
                        ref = PsiTreeUtil.findElementOfClassAtOffset(context.getFile(), context.getStartOffset(), PsiReferenceExpression.class, false);
                if (ref != null) {
                    ref.bindToElementViaStaticImport(((PsiField)variable).getContainingClass());
                    PostprocessReformattingAspect.getInstance(ref.getProject()).doPostponedFormatting();
                }
                if (toDelete.isValid()) {
                    document.deleteString(toDelete.getStartOffset(), toDelete.getEndOffset());
                }
                context.commitDocument();
            }
            else if (shouldQualify((PsiField)variable, context)) {
                qualifyFieldReference(context, (PsiField)variable);
            }
        }

        PsiReferenceExpression ref = PsiTreeUtil.findElementOfClassAtOffset(context.getFile(), context.getTailOffset() - 1, PsiReferenceExpression.class, false);
        if (ref != null) {
            JavaCodeStyleManager.getInstance(context.getProject()).shortenClassReferences(ref);
        }

        ref = PsiTreeUtil.findElementOfClassAtOffset(context.getFile(), context.getTailOffset() - 1, PsiReferenceExpression.class, false);
        PsiElement target = ref == null ? null : ref.resolve();
        if (target instanceof PsiLocalVariable || target instanceof PsiParameter) {
            makeFinalIfNeeded(context, (PsiVariable)target);
        }

        final char completionChar = context.getCompletionChar();
        if (completionChar == '=') {
            context.setAddCompletionChar(false);
            TailType.EQ.processTail(context.getEditor(), context.getTailOffset());
        }
        else if (completionChar == ',' && getAttribute(LookupItem.TAIL_TYPE_ATTR) != TailType.UNKNOWN) {
            context.setAddCompletionChar(false);
            TailType.COMMA.processTail(context.getEditor(), context.getTailOffset());
            AutoPopupController.getInstance(context.getProject()).autoPopupParameterInfo(context.getEditor(), null);
        }
        else if (completionChar == ':') {
            context.setAddCompletionChar(false);
            TailType.COND_EXPR_COLON.processTail(context.getEditor(), context.getTailOffset());
        }
        else if (completionChar == '.') {
            AutoPopupController.getInstance(context.getProject()).autoPopupMemberLookup(context.getEditor(), null);
        }
        else if (completionChar == '!' && PsiType.BOOLEAN.isAssignableFrom(variable.getType())) {
            context.setAddCompletionChar(false);
            if (ref != null) {
                FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EXCLAMATION_FINISH);
                document.insertString(ref.getTextRange().getStartOffset(), "!");
            }
        }
    }

    public static void makeFinalIfNeeded( InsertionContext context,  PsiVariable variable) {
        PsiElement place = context.getFile().findElementAt(context.getTailOffset() - 1);
        if (!Registry.is("java.completion.make.outer.variables.final") ||
                place == null || PsiUtil.isLanguageLevel8OrHigher(place) || JspPsiUtil.isInJspFile(place)) {
            return;
        }

        if (HighlightControlFlowUtil.getInnerClassVariableReferencedFrom(variable, place) != null &&
                !HighlightControlFlowUtil.isReassigned(variable, new HashMap<PsiElement, Collection<ControlFlowUtil.VariableInfo>>())) {
            PsiUtil.setModifierProperty(variable, PsiModifier.FINAL, true);
        }
    }

    private boolean shouldQualify(PsiField field, InsertionContext context) {
        if (myHelper != null && !myHelper.willBeImported()) {
            return true;
        }

        if (getAttribute(FORCE_QUALIFY) != null) {
            return true;
        }

        PsiReference reference = context.getFile().findReferenceAt(context.getStartOffset());
        if (reference instanceof PsiReferenceExpression && !((PsiReferenceExpression)reference).isQualified()) {
            final PsiVariable target = JavaPsiFacade.getInstance(context.getProject()).getResolveHelper()
                    .resolveReferencedVariable(field.getName(), (PsiElement)reference);
            return !field.getManager().areElementsEquivalent(target, CompletionUtil.getOriginalOrSelf(field));
        }
        return false;
    }

    private static void qualifyFieldReference(InsertionContext context, PsiField field) {
        context.commitDocument();
        PsiFile file = context.getFile();
        final PsiReference reference = file.findReferenceAt(context.getStartOffset());
        if (reference instanceof PsiJavaCodeReferenceElement && ((PsiJavaCodeReferenceElement)reference).isQualified()) {
            return;
        }

        PsiClass containingClass = field.getContainingClass();
        if (containingClass != null && containingClass.getName() != null) {
            OffsetKey oldStart = context.trackOffset(context.getStartOffset(), true);
            JavaCompletionUtil.insertClassReference(containingClass, file, context.getStartOffset());
            context.getDocument().insertString(context.getOffsetMap().getOffset(oldStart), ".");
            PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());
        }
    }
}