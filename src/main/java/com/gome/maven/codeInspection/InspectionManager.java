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
package com.gome.maven.codeInspection;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;

/**
 * @author max
 */
public abstract class InspectionManager {
    public static final ExtensionPointName<Condition<PsiElement>> CANT_BE_STATIC_EXTENSION = ExtensionPointName.create("com.gome.maven.cantBeStatic");

    public static InspectionManager getInstance(Project project) {
        return ServiceManager.getService(project, InspectionManager.class);
    }

    
    public abstract Project getProject();

    
    public abstract CommonProblemDescriptor createProblemDescriptor(  String descriptionTemplate, QuickFix... fixes);

    /**
     * Factory method for ProblemDescriptor. Should be called from LocalInspectionTool.checkXXX() methods.
     * @param psiElement problem is reported against
     * @param descriptionTemplate problem message. Use <code>#ref</code> for a link to problem piece of code and <code>#loc</code> for location in source code.
     * @param fix should be null if no fix is provided.
     * @param onTheFly for local tools on batch run
     */
    
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                                String descriptionTemplate,
                                                              LocalQuickFix fix,
                                                               ProblemHighlightType highlightType,
                                                              boolean onTheFly);

    
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                                String descriptionTemplate,
                                                              boolean onTheFly,
                                                              LocalQuickFix[] fixes,
                                                               ProblemHighlightType highlightType);

    
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                                String descriptionTemplate,
                                                              LocalQuickFix[] fixes,
                                                               ProblemHighlightType highlightType,
                                                              boolean onTheFly,
                                                              boolean isAfterEndOfLine);

    
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement startElement,
                                                               PsiElement endElement,
                                                                String descriptionTemplate,
                                                               ProblemHighlightType highlightType,
                                                              boolean onTheFly,
                                                              LocalQuickFix... fixes);

    
    public abstract ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                              TextRange rangeInElement,
                                                                String descriptionTemplate,
                                                               ProblemHighlightType highlightType,
                                                              boolean onTheFly,
                                                              LocalQuickFix... fixes);

    
    public abstract ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                                String descriptionTemplate,
                                                              final boolean showTooltip,
                                                               ProblemHighlightType highlightType,
                                                              boolean onTheFly,
                                                              final LocalQuickFix... fixes);
    @Deprecated
    
    /**
     * use {@link #createProblemDescriptor(PsiElement, String, boolean, LocalQuickFix, ProblemHighlightType)} instead
     */
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                               String descriptionTemplate,
                                                              LocalQuickFix fix,
                                                               ProblemHighlightType highlightType);

    @Deprecated
    
    /**
     * use {@link #createProblemDescriptor(PsiElement, String, boolean, LocalQuickFix[], ProblemHighlightType)} instead
     */
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                               String descriptionTemplate,
                                                              LocalQuickFix[] fixes,
                                                               ProblemHighlightType highlightType);

    @Deprecated
    
    /**
     * use {@link #createProblemDescriptor(PsiElement, String, LocalQuickFix[], ProblemHighlightType, boolean, boolean)} instead
     */
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement psiElement,
                                                               String descriptionTemplate,
                                                              LocalQuickFix[] fixes,
                                                               ProblemHighlightType highlightType,
                                                              boolean isAfterEndOfLine);

    @Deprecated
    
    /**
     * use {@link #createProblemDescriptor(PsiElement, PsiElement, String, ProblemHighlightType, boolean, LocalQuickFix...)} instead
     */
    public abstract ProblemDescriptor createProblemDescriptor( PsiElement startElement,
                                                               PsiElement endElement,
                                                               String descriptionTemplate,
                                                               ProblemHighlightType highlightType,
                                                              LocalQuickFix... fixes);


    @Deprecated
    
    /**
     * use {@link #createProblemDescriptor(PsiElement, TextRange, String, ProblemHighlightType, boolean, LocalQuickFix...)} instead
     */
    public abstract ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                              final TextRange rangeInElement,
                                                               final String descriptionTemplate,
                                                               ProblemHighlightType highlightType,
                                                              final LocalQuickFix... fixes);

    @Deprecated
    
    /**
     * use {@link #createProblemDescriptor(PsiElement, String, boolean, ProblemHighlightType, boolean, LocalQuickFix...)} instead
     */
    public abstract ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                               final String descriptionTemplate,
                                                              final boolean showTooltip,
                                                               ProblemHighlightType highlightType,
                                                              final LocalQuickFix... fixes);

    
    public abstract GlobalInspectionContext createNewGlobalContext(boolean reuse);
}
