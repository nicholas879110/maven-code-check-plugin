/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.codeInspection.nullable;

import com.gome.maven.codeInsight.AnnotationUtil;
import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.intention.AddAnnotationPsiFix;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiNameValuePair;
import com.gome.maven.psi.PsiParameter;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.searches.OverridingMethodsSearch;
import com.gome.maven.psi.util.ClassUtil;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.IncorrectOperationException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
public class AnnotateOverriddenMethodParameterFix implements LocalQuickFix {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.AnnotateMethodFix");
    private final String myAnnotation;
    private final String[] myAnnosToRemove;

    public AnnotateOverriddenMethodParameterFix(final String fqn, String... annosToRemove) {
        myAnnotation = fqn;
        myAnnosToRemove = annosToRemove;
    }

    @Override
    
    public String getName() {
        return InspectionsBundle.message("annotate.overridden.methods.parameters", ClassUtil.extractClassName(myAnnotation));
    }

    @Override
    public void applyFix( Project project,  ProblemDescriptor descriptor) {
        final PsiElement psiElement = descriptor.getPsiElement();

        PsiParameter parameter = PsiTreeUtil.getParentOfType(psiElement, PsiParameter.class, false);
        if (parameter == null) return;
        PsiMethod method = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
        if (method == null) return;
        PsiParameter[] parameters = method.getParameterList().getParameters();
        int index = ArrayUtilRt.find(parameters, parameter);

        List<PsiParameter> toAnnotate = new ArrayList<PsiParameter>();

        PsiMethod[] methods = OverridingMethodsSearch.search(method, GlobalSearchScope.allScope(project), true).toArray(PsiMethod.EMPTY_ARRAY);
        for (PsiMethod psiMethod : methods) {
            PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
            if (index >= psiParameters.length) continue;
            PsiParameter psiParameter = psiParameters[index];
            if (!AnnotationUtil.isAnnotated(psiParameter, myAnnotation, false, false) && psiMethod.getManager().isInProject(psiMethod)) {
                toAnnotate.add(psiParameter);
            }
        }

        FileModificationService.getInstance().preparePsiElementsForWrite(toAnnotate);
        for (PsiParameter psiParam : toAnnotate) {
            try {
                assert psiParam != null : toAnnotate;
                if (AnnotationUtil.isAnnotatingApplicable(psiParam, myAnnotation)) {
                    AddAnnotationPsiFix fix = new AddAnnotationPsiFix(myAnnotation, psiParam, PsiNameValuePair.EMPTY_ARRAY, myAnnosToRemove);
                    fix.invoke(project, psiParam.getContainingFile(), psiParam, psiParam);
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }

    @Override
    
    public String getFamilyName() {
        return getName();
    }
}