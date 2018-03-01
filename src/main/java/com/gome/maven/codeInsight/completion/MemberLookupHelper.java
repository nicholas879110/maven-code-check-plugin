package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupElementPresentation;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiFormatUtil;
import com.gome.maven.psi.util.PsiFormatUtilBase;

import java.util.List;

/**
 * @author peter
 */
public class MemberLookupHelper {
    private final PsiMember myMember;
    private final boolean myMergedOverloads;
     private final PsiClass myContainingClass;
    private boolean myShouldImport = false;

    public MemberLookupHelper(List<PsiMethod> overloads, PsiClass containingClass, boolean shouldImport) {
        this(overloads.get(0), containingClass, shouldImport, true);
    }

    public MemberLookupHelper(PsiMember member, PsiClass containingClass, boolean shouldImport, final boolean mergedOverloads) {
        myMember = member;
        myContainingClass = containingClass;
        myShouldImport = shouldImport;
        myMergedOverloads = mergedOverloads;
    }

    public PsiMember getMember() {
        return myMember;
    }

    
    public PsiClass getContainingClass() {
        return myContainingClass;
    }

    public void setShouldBeImported(boolean shouldImportStatic) {
        myShouldImport = shouldImportStatic;
    }

    public boolean willBeImported() {
        return myShouldImport;
    }

    public void renderElement(LookupElementPresentation presentation,  Boolean qualify, PsiSubstitutor substitutor) {
        final String className = myContainingClass == null ? "???" : myContainingClass.getName();

        final String memberName = myMember.getName();
        if (!Boolean.FALSE.equals(qualify) && (!myShouldImport && StringUtil.isNotEmpty(className) || Boolean.TRUE.equals(qualify))) {
            presentation.setItemText(className + "." + memberName);
        } else {
            presentation.setItemText(memberName);
        }

        final String qname = myContainingClass == null ? "" : myContainingClass.getQualifiedName();
        String pkg = qname == null ? "" : StringUtil.getPackageName(qname);
        String location = Boolean.FALSE.equals(qualify) || StringUtil.isEmpty(pkg) ? "" : " (" + pkg + ")";

        final String params = myMergedOverloads
                ? "(...)"
                : myMember instanceof PsiMethod
                ? PsiFormatUtil.formatMethod((PsiMethod)myMember, substitutor,
                PsiFormatUtilBase.SHOW_PARAMETERS,
                PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_TYPE)
                : "";
        presentation.clearTail();
        presentation.appendTailText(params, false);
        if (myShouldImport && StringUtil.isNotEmpty(className)) {
            presentation.appendTailText(" in " + className + location, true);
        } else {
            presentation.appendTailText(location, true);
        }

        final PsiType type = myMember instanceof PsiMethod ? ((PsiMethod)myMember).getReturnType() : ((PsiField) myMember).getType();
        if (type != null) {
            presentation.setTypeText(substitutor.substitute(type).getPresentableText());
        }
    }


}
