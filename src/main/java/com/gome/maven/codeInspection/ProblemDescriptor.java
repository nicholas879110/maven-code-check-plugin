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

import com.gome.maven.lang.annotation.ProblemGroup;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;

/**
 * See {@link InspectionManager#createProblemDescriptor(com.gome.maven.psi.PsiElement, String, LocalQuickFix, ProblemHighlightType,boolean) } for method descriptions.
 */
public interface ProblemDescriptor extends CommonProblemDescriptor{
    ProblemDescriptor[] EMPTY_ARRAY = new ProblemDescriptor[0];

    PsiElement getPsiElement();
    PsiElement getStartElement();
    PsiElement getEndElement();
    TextRange getTextRangeInElement();
    int getLineNumber();
    
    ProblemHighlightType getHighlightType();
    boolean isAfterEndOfLine();

    /**
     * Sets custom attributes for highlighting the inspection result. Can be used only when the severity of the problem is INFORMATION.
     *
     * @param key the text attributes key for highlighting the result.
     * @since 9.0
     */
    void setTextAttributes(TextAttributesKey key);

    /**
     * Gets the unique object, which has the same {@link com.gome.maven.lang.annotation.ProblemGroup#getProblemName()} for all of the problems of this group
     *
     * @return the problem group
     */
    
    ProblemGroup getProblemGroup();

    /**
     * Sets the unique object, which has the same {@link com.gome.maven.lang.annotation.ProblemGroup#getProblemName()} for all of the problems of this group
     *
     * @param problemGroup the problemGroup
     */
    void setProblemGroup( ProblemGroup problemGroup);

    boolean showTooltip();
}
