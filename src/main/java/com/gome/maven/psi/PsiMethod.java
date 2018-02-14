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

import com.gome.maven.pom.PomRenameableTarget;
import com.gome.maven.psi.util.MethodSignature;
import com.gome.maven.psi.util.MethodSignatureBackedByPsiMethod;
import com.gome.maven.util.ArrayFactory;
import com.gome.maven.util.IncorrectOperationException;

import java.util.List;

/**
 * Represents a Java method or constructor.
 *
 * @see PsiClass#getMethods()
 */
public interface PsiMethod extends PsiMember, PsiNameIdentifierOwner, PsiModifierListOwner, PsiDocCommentOwner, PsiTypeParameterListOwner,
        PomRenameableTarget<PsiElement>, PsiTarget {
    /**
     * The empty array of PSI methods which can be reused to avoid unnecessary allocations.
     */
    PsiMethod[] EMPTY_ARRAY = new PsiMethod[0];

    ArrayFactory<PsiMethod> ARRAY_FACTORY = new ArrayFactory<PsiMethod>() {
        
        @Override
        public PsiMethod[] create(final int count) {
            return count == 0 ? EMPTY_ARRAY : new PsiMethod[count];
        }
    };

    /**
     * Returns the return type of the method.
     *
     * @return the method return type, or null if the method is a constructor.
     */
    
    PsiType getReturnType();

    /**
     * Returns the type element for the return type of the method.
     *
     * @return the type element for the return type, or null if the method is a constructor.
     */
    
    PsiTypeElement getReturnTypeElement();

    /**
     * Returns the parameter list for the method.
     *
     * @return the parameter list instance.
     */
    
    PsiParameterList getParameterList();

    /**
     * Returns the list of thrown exceptions for the method.
     *
     * @return the list of thrown exceptions instance.
     */
    
    PsiReferenceList getThrowsList();

    /**
     * Returns the body of the method.
     *
     * @return the method body, or null if the method belongs to a compiled class.
     */
    
    PsiCodeBlock getBody();

    /**
     * Checks if the method is a constructor.
     *
     * @return true if the method is a constructor, false otherwise
     */
    boolean isConstructor();

    /**
     * Checks if the method accepts a variable number of arguments.
     *
     * @return true if the method is varargs, false otherwise.
     */
    boolean isVarArgs();

    /**
     * Returns the signature of this method, using the specified substitutor to specify
     * values of generic type parameters.
     *
     * @param substitutor the substitutor.
     * @return the method signature instance.
     */
    
    MethodSignature getSignature( PsiSubstitutor substitutor);

    /**
     * Returns the name identifier for the method.
     *
     * @return the name identifier instance.
     */
    @Override
    
    PsiIdentifier getNameIdentifier();

    /**
     * Searches the superclasses and base interfaces of the containing class to find
     * the methods which this method overrides or implements. Can return multiple results
     * if the base class and/or one or more of the implemented interfaces have a method
     * with the same signature. If the overridden method in turn overrides another method,
     * only the directly overridden method is returned.
     *
     * @return the array of super methods, or an empty array if no methods are found.
     */
    
    PsiMethod[] findSuperMethods();

    /**
     * Searches the superclasses and base interfaces of the containing class to find
     * the methods which this method overrides or implements, optionally omitting
     * the accessibility check. Can return multiple results if the base class and/or
     * one or more of the implemented interfaces have a method with the same signature.
     * If the overridden method in turn overrides another method, only the directly
     * overridden method is returned.
     *
     * @param checkAccess if false, the super methods are searched even if this method
     *                    is private. If true, an empty result list is returned for private methods.
     * @return the array of super methods, or an empty array if no methods are found.
     */
    
    PsiMethod[] findSuperMethods(boolean checkAccess);

    /**
     * Searches the superclasses and base interfaces of the specified class to find
     * the methods which this method can override or implement. Can return multiple results
     * if the base class and/or one or more of the implemented interfaces have a method
     * with the same signature.
     *
     * @param parentClass the class to search for super methods.
     * @return the array of super methods, or an empty array if no methods are found.
     */
    
    PsiMethod[] findSuperMethods(PsiClass parentClass);

    /**
     * Searches the superclasses and base interfaces of the containing class to find
     * static and instance methods with the signature matching the signature of this method.
     * Can return multiple results if the base class and/or one or more of the implemented
     * interfaces have a method with the same signature. If the overridden method in turn
     * overrides another method, only the directly overridden method is returned.
     *
     * @param checkAccess if false, the super methods are searched even if this method
     *                    is private. If true, an empty result list is returned for private methods.
     * @return the array of matching method signatures, or an empty array if no methods are found.
     */
    
    List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess);

    /**
     * Returns the method in the deepest base superclass or interface of the containing class which
     * this method overrides or implements.
     *
     * @return the overridden or implemented method, or null if this method does not override
     *         or implement any other method.
     * @deprecated use {@link #findDeepestSuperMethods()} instead
     */
    
    PsiMethod findDeepestSuperMethod();

    
    PsiMethod[] findDeepestSuperMethods();

    @Override
    
    PsiModifierList getModifierList();

    @Override
    
    
    String getName();

    @Override
    PsiElement setName(  String name) throws IncorrectOperationException;

    
    HierarchicalMethodSignature getHierarchicalMethodSignature();
}
