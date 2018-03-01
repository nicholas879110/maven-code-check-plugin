package com.gome.maven.codeInsight;

import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiMethod;

public interface MemberImplementorExplorer {
     PsiMethod[] getMethodsToImplement(PsiClass aClass);
}
