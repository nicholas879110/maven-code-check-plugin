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
package com.gome.maven.codeInspection;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.profile.codeInspection.InspectionProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiElement;

public abstract class InspectionManagerBase extends InspectionManager {
    private final Project myProject;
     protected String myCurrentProfileName;

    public InspectionManagerBase(Project project) {
        myProject = project;
    }

    @Override
    
    public Project getProject() {
        return myProject;
    }

    @Override
    
    public CommonProblemDescriptor createProblemDescriptor( String descriptionTemplate, QuickFix... fixes) {
        return new CommonProblemDescriptorImpl(fixes, descriptionTemplate);
    }

    @Override
    
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     LocalQuickFix fix,
                                                      ProblemHighlightType highlightType,
                                                     boolean onTheFly) {
        LocalQuickFix[] quickFixes = fix != null ? new LocalQuickFix[]{fix} : null;
        return createProblemDescriptor(psiElement, descriptionTemplate, onTheFly, quickFixes, highlightType);
    }

    @Override
    
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     boolean onTheFly,
                                                     LocalQuickFix[] fixes,
                                                      ProblemHighlightType highlightType) {
        return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, onTheFly, false);
    }

    @Override
    
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     LocalQuickFix[] fixes,
                                                      ProblemHighlightType highlightType,
                                                     boolean onTheFly,
                                                     boolean isAfterEndOfLine) {
        return new ProblemDescriptorBase(psiElement, psiElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, null, true, onTheFly);
    }

    @Override
    
    public ProblemDescriptor createProblemDescriptor( PsiElement startElement,
                                                      PsiElement endElement,
                                                      String descriptionTemplate,
                                                      ProblemHighlightType highlightType,
                                                     boolean onTheFly,
                                                     LocalQuickFix... fixes) {
        return new ProblemDescriptorBase(startElement, endElement, descriptionTemplate, fixes, highlightType, false, null, true, onTheFly);
    }

    
    @Override
    public ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                     final TextRange rangeInElement,
                                                      final String descriptionTemplate,
                                                      final ProblemHighlightType highlightType,
                                                     boolean onTheFly,
                                                     final LocalQuickFix... fixes) {
        return new ProblemDescriptorBase(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, rangeInElement, true, onTheFly);
    }

    
    @Override
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     boolean showTooltip,
                                                      ProblemHighlightType highlightType,
                                                     boolean onTheFly,
                                                     LocalQuickFix... fixes) {
        return new ProblemDescriptorBase(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, null, showTooltip, onTheFly);
    }

    @Override
    @Deprecated
    
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     LocalQuickFix fix,
                                                      ProblemHighlightType highlightType) {
        LocalQuickFix[] quickFixes = fix != null ? new LocalQuickFix[]{fix} : null;
        return createProblemDescriptor(psiElement, descriptionTemplate, false, quickFixes, highlightType);
    }

    @Override
    @Deprecated
    
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     LocalQuickFix[] fixes,
                                                      ProblemHighlightType highlightType) {
        return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, false, false);
    }

    @Override
    @Deprecated
    
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     LocalQuickFix[] fixes,
                                                      ProblemHighlightType highlightType,
                                                     boolean isAfterEndOfLine) {
        return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, true, isAfterEndOfLine);
    }

    @Override
    @Deprecated
    
    public ProblemDescriptor createProblemDescriptor( PsiElement startElement,
                                                      PsiElement endElement,
                                                      String descriptionTemplate,
                                                      ProblemHighlightType highlightType,
                                                     LocalQuickFix... fixes) {
        return createProblemDescriptor(startElement, endElement, descriptionTemplate, highlightType, true, fixes);
    }

    
    @Override
    @Deprecated
    public ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                     final TextRange rangeInElement,
                                                      final String descriptionTemplate,
                                                      final ProblemHighlightType highlightType,
                                                     final LocalQuickFix... fixes) {
        return createProblemDescriptor(psiElement, rangeInElement, descriptionTemplate, highlightType, true, fixes);
    }

    
    @Deprecated
    @Override
    public ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                      String descriptionTemplate,
                                                     boolean showTooltip,
                                                      ProblemHighlightType highlightType,
                                                     LocalQuickFix... fixes) {
        return createProblemDescriptor(psiElement, descriptionTemplate, showTooltip, highlightType, true, fixes);
    }

    public String getCurrentProfile() {
        if (myCurrentProfileName == null) {
            final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(getProject());
            myCurrentProfileName = profileManager.getProjectProfile();
            if (myCurrentProfileName == null) {
                myCurrentProfileName = InspectionProfileManager.getInstance().getRootProfile().getName();
            }
        }
        return myCurrentProfileName;
    }
}
