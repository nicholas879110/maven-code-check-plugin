/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.generation;

import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiSubstitutor;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.util.PsiFormatUtil;
import com.gome.maven.psi.util.PsiFormatUtilBase;

/**
 * @author peter
 */
public class PsiMethodMember extends PsiElementClassMember<PsiMethod>{
    private static final int PARAM_OPTIONS = PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_TYPE | PsiFormatUtilBase.TYPE_AFTER;
    private static final int METHOD_OPTIONS = PARAM_OPTIONS | PsiFormatUtilBase.SHOW_PARAMETERS;

    public PsiMethodMember( PsiMethod method) {
        this(method, PsiSubstitutor.EMPTY);
    }

    public PsiMethodMember( CandidateInfo info) {
        this((PsiMethod)info.getElement(), info.getSubstitutor());
    }

    public PsiMethodMember( PsiMethod method,  PsiSubstitutor substitutor) {
        super(method, substitutor, PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, METHOD_OPTIONS, PARAM_OPTIONS));
    }

}
