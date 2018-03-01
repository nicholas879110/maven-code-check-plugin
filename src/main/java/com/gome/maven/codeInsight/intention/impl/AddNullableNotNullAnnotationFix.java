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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jul 20, 2007
 * Time: 2:57:59 PM
 */
package com.gome.maven.codeInsight.intention.impl;

import com.gome.maven.codeInsight.AnnotationUtil;
import com.gome.maven.codeInsight.intention.AddAnnotationPsiFix;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;

public class AddNullableNotNullAnnotationFix extends AddAnnotationPsiFix {
    public AddNullableNotNullAnnotationFix( String fqn,  PsiModifierListOwner owner,  String... annotationToRemove) {
        super(fqn, owner, PsiNameValuePair.EMPTY_ARRAY, annotationToRemove);
    }

    @Override
    public boolean isAvailable( Project project,
                                PsiFile file,
                                PsiElement startElement,
                                PsiElement endElement) {
        if (!super.isAvailable(project, file, startElement, endElement)) {
            return false;
        }
        PsiModifierListOwner owner = getContainer(file, startElement.getTextRange().getStartOffset());
        if (owner == null || AnnotationUtil.isAnnotated(owner, getAnnotationsToRemove()[0], false, false)) {
            return false;
        }
        if (owner instanceof PsiMethod) {
            PsiType returnType = ((PsiMethod)owner).getReturnType();

            return returnType != null && !(returnType instanceof PsiPrimitiveType);
        }
        return true;
    }
}
