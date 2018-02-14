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
package com.gome.maven.psi;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.RecursionGuard;
import com.gome.maven.openapi.util.RecursionManager;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.impl.source.resolve.ParameterTypeInferencePolicy;
import com.gome.maven.psi.infos.CandidateInfo;

/**
 * Service for resolving references to declarations.
 *
 * @see JavaPsiFacade#getResolveHelper()
 */
public interface PsiResolveHelper {
    RecursionGuard ourGuard = RecursionManager.createGuard("typeArgInference");
    RecursionGuard ourGraphGuard = RecursionManager.createGuard("graphTypeArgInference");

    class SERVICE {
        private SERVICE() {
        }

        public static PsiResolveHelper getInstance(Project project) {
            return ServiceManager.getService(project, PsiResolveHelper.class);
        }
    }

    /**
     * Resolves a constructor.
     * The resolved constructor is not necessarily accessible from the point of the call,
     * but accessible constructors have a priority.
     *
     * @param type              the class containing the constructor
     * @param argumentList      list of arguments of the call or new expression
     * @param place             place where constructor is invoked (used for checking access)
     * @return the result of the resolve, or {@link JavaResolveResult#EMPTY} if the resolve failed.
     */
    
    JavaResolveResult resolveConstructor(PsiClassType type, PsiExpressionList argumentList, PsiElement place);

    /**
     * Resolves a constructor and returns all variants for the resolve.
     * The resolved constructors are not necessarily accessible from the point of the call,
     * but accessible constructors have a priority.
     *
     * @param type              the class containing the constructor
     * @param argumentList      list of arguments of the call or new expression
     * @param place             place where constructor is invoked (used for checking access)
     * @return the result of the resolve, or {@link JavaResolveResult#EMPTY} if the resolve failed.
     */
    
    JavaResolveResult[] multiResolveConstructor( PsiClassType type,  PsiExpressionList argumentList,  PsiElement place);

    /**
     * Resolves a call expression and returns an array of possible resolve results.
     *
     * @param call the call expression to resolve.
     * @param dummyImplicitConstructor if true, implicit empty constructor which does not actually exist
     * can be returned as a candidate for the resolve.
     * @return the array of resolve results.
     */
    
    CandidateInfo[] getReferencedMethodCandidates( PsiCallExpression call, boolean dummyImplicitConstructor);

    /**
     * Resolves a call expression and returns an array of possible resolve results.
     *
     * @param call the call expression to resolve.
     * @param dummyImplicitConstructor if true, implicit empty constructor which does not actually exist
     * can be returned as a candidate for the resolve.
     * @param checkVarargs true if varargs method should lead to 2 candidates in the result array
     * @return the array of resolve results.
     */
    
    CandidateInfo[] getReferencedMethodCandidates( PsiCallExpression call, boolean dummyImplicitConstructor, boolean checkVarargs);

    /**
     * Resolves a reference to a class, given the text of the reference and the context
     * in which it was encountered.
     *
     * @param referenceText the text of the reference.
     * @param context       the context in which the reference is found.
     * @return the resolve result, or null if the resolve was not successful.
     */
    
    PsiClass resolveReferencedClass( String referenceText, PsiElement context);

    /**
     * Resolves a reference to a variable, given the text of the reference and the context
     * in which it was encountered.
     *
     * @param referenceText the text of the reference.
     * @param context       the context in which the reference is found.
     * @return the resolve result, or null if the resolve was not successful.
     */
    
    PsiVariable resolveReferencedVariable( String referenceText, PsiElement context);

    /**
     * Resolves a reference to a variable, given the text of the reference and the context
     * in which it was encountered.
     *
     * @param referenceText the text of the reference.
     * @param context       the context in which the reference is found.
     * @return the resolve result, or null if the resolve was not successful or resolved variable is not accessible in a given context.
     */
    
    PsiVariable resolveAccessibleReferencedVariable( String referenceText, PsiElement context);

    boolean isAccessible( PsiMember member,  PsiModifierList modifierList,
                          PsiElement place,  PsiClass accessObjectClass,  PsiElement currentFileResolveScope);

    boolean isAccessible( PsiMember member,  PsiElement place,  PsiClass accessObjectClass);

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
