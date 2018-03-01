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
package com.gome.maven.psi;

import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.impl.source.resolve.ParameterTypeInferencePolicy;

/**
 * User: anna
 */
public interface PsiInferenceHelper {
    /**
     * @return {@link PsiType#NULL} iff no type could be inferred
     *         null         iff the type inferred is raw
     *         inferred type otherwise
     */
    PsiType inferTypeForMethodTypeParameter( PsiTypeParameter typeParameter,
                                             PsiParameter[] parameters,
                                             PsiExpression[] arguments,
                                             PsiSubstitutor partialSubstitutor,
                                             PsiElement parent,
                                             ParameterTypeInferencePolicy policy);

    
    PsiSubstitutor inferTypeArguments( PsiTypeParameter[] typeParameters,
                                       PsiParameter[] parameters,
                                       PsiExpression[] arguments,
                                       PsiSubstitutor partialSubstitutor,
                                       PsiElement parent,
                                       ParameterTypeInferencePolicy policy,
                                       LanguageLevel languageLevel);

    
    PsiSubstitutor inferTypeArguments( PsiTypeParameter[] typeParameters,
                                       PsiType[] leftTypes,
                                       PsiType[] rightTypes,
                                       LanguageLevel languageLevel);

    PsiType getSubstitutionForTypeParameter(PsiTypeParameter typeParam,
                                            PsiType param,
                                            PsiType arg,
                                            boolean isContraVariantPosition,
                                            LanguageLevel languageLevel);
}
