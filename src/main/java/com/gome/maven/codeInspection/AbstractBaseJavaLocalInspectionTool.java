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

import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiTreeUtil;

public abstract class AbstractBaseJavaLocalInspectionTool extends LocalInspectionTool {
    /**
     * Override this to report problems at method level.
     *
     * @param method     to check.
     * @param manager    InspectionManager to ask for ProblemDescriptors from.
     * @param isOnTheFly true if called during on the fly editor highlighting. Called from Inspect Code action otherwise.
     * @return <code>null</code> if no problems found or not applicable at method level.
     */
    
    public ProblemDescriptor[] checkMethod( PsiMethod method,  InspectionManager manager, boolean isOnTheFly) {
        return null;
    }

    /**
     * Override this to report problems at class level.
     *
     * @param aClass     to check.
     * @param manager    InspectionManager to ask for ProblemDescriptors from.
     * @param isOnTheFly true if called during on the fly editor highlighting. Called from Inspect Code action otherwise.
     * @return <code>null</code> if no problems found or not applicable at class level.
     */
    
    public ProblemDescriptor[] checkClass( PsiClass aClass,  InspectionManager manager, boolean isOnTheFly) {
        return null;
    }

    /**
     * Override this to report problems at field level.
     *
     * @param field      to check.
     * @param manager    InspectionManager to ask for ProblemDescriptors from.
     * @param isOnTheFly true if called during on the fly editor highlighting. Called from Inspect Code action otherwise.
     * @return <code>null</code> if no problems found or not applicable at field level.
     */
    
    public ProblemDescriptor[] checkField( PsiField field,  InspectionManager manager, boolean isOnTheFly) {
        return null;
    }

    /**
     * Override this to report problems at file level.
     *
     * @param file       to check.
     * @param manager    InspectionManager to ask for ProblemDescriptors from.
     * @param isOnTheFly true if called during on the fly editor highlighting. Called from Inspect Code action otherwise.
     * @return <code>null</code> if no problems found or not applicable at file level.
     */
    @Override
    
    public ProblemDescriptor[] checkFile( PsiFile file,  InspectionManager manager, boolean isOnTheFly) {
        return null;
    }

    @Override
    
    public PsiElementVisitor buildVisitor( final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                addDescriptors(checkMethod(method, holder.getManager(), isOnTheFly));
            }

            @Override
            public void visitClass(PsiClass aClass) {
                addDescriptors(checkClass(aClass, holder.getManager(), isOnTheFly));
            }

            @Override
            public void visitField(PsiField field) {
                addDescriptors(checkField(field, holder.getManager(), isOnTheFly));
            }

            @Override
            public void visitFile(PsiFile file) {
                addDescriptors(checkFile(file, holder.getManager(), isOnTheFly));
            }

            private void addDescriptors(final ProblemDescriptor[] descriptors) {
                if (descriptors != null) {
                    for (ProblemDescriptor descriptor : descriptors) {
                        holder.registerProblem(descriptor);
                    }
                }
            }
        };
    }

    @Override
    public PsiNamedElement getProblemElement(final PsiElement psiElement) {
        return PsiTreeUtil.getNonStrictParentOfType(psiElement, PsiFile.class, PsiClass.class, PsiMethod.class, PsiField.class);
    }
}
