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

import com.gome.maven.openapi.util.Pair;
import com.gome.maven.util.ArrayFactory;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.pom.PomRenameableTarget;

import java.util.Collection;
import java.util.List;

/**
 * Represents a Java class or interface.
 *
 * @see PsiJavaFile#getClasses()
 */
public interface PsiClass
        extends PsiNameIdentifierOwner, PsiModifierListOwner, PsiDocCommentOwner, PsiTypeParameterListOwner, PsiTarget, PomRenameableTarget<PsiElement> {
    /**
     * The empty array of PSI classes which can be reused to avoid unnecessary allocations.
     */
     PsiClass[] EMPTY_ARRAY = new PsiClass[0];

    ArrayFactory<PsiClass> ARRAY_FACTORY = new ArrayFactory<PsiClass>() {
        
        @Override
        public PsiClass[] create(final int count) {
            return count == 0 ? EMPTY_ARRAY : new PsiClass[count];
        }
    };

    /**
     * Returns the fully qualified name of the class.
     *
     * @return the qualified name of the class, or null for anonymous and local classes, and for type parameters
     */
     
    String getQualifiedName();

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    boolean isInterface();

    /**
     * Checks if the class is an annotation type.
     *
     * @return true if the class is an annotation type, false otherwise
     */
    boolean isAnnotationType();

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    boolean isEnum();

    /**
     * Returns the list of classes that this class or interface extends.
     *
     * @return the extends list, or null for anonymous classes.
     */
    
    PsiReferenceList getExtendsList();

    /**
     * Returns the list of interfaces that this class implements.
     *
     * @return the implements list, or null for anonymous classes
     */
    
    PsiReferenceList getImplementsList();

    /**
     * Returns the list of class types for the classes that this class or interface extends.
     *
     * @return the list of extended class types, or an empty list for anonymous classes.
     */
    
    PsiClassType[] getExtendsListTypes();

    /**
     * Returns the list of class types for the interfaces that this class implements.
     *
     * @return the list of extended class types, or an empty list for anonymous classes,
     *         enums and annotation types
     */
    
    PsiClassType[] getImplementsListTypes();

    /**
     * Returns the base class of this class.
     *
     * @return the base class. May return null when jdk is not configured, so no java.lang.Object is found,
     *         or for java.lang.Object itself
     */
    
    PsiClass getSuperClass();

    /**
     * Returns the list of interfaces implemented by the class, or extended by the interface.
     *
     * @return the list of interfaces.
     */
    PsiClass[] getInterfaces();

    /**
     * Returns the list of classes and interfaces extended or implemented by the class.
     *
     * @return the list of classes or interfaces. May return zero elements when jdk is
     *         not configured, so no java.lang.Object is found
     */
     PsiClass[] getSupers();

    /**
     * Returns the list of class types for the classes and interfaces extended or
     * implemented by the class.
     *
     * @return the list of class types for the classes or interfaces.
     *         For the class with no explicit extends list, the returned list always contains at least one element for the java.lang.Object type.
     *         If psiClass is java.lang.Object, returned list is empty.
     */
     PsiClassType[] getSuperTypes();

    /**
     * Returns the list of fields in the class.
     *
     * @return the list of fields.
     */
    
    PsiField[] getFields();

    /**
     * Returns the list of methods in the class.
     *
     * @return the list of methods.
     */
    
    PsiMethod[] getMethods();

    /**
     * Returns the list of constructors for the class.
     *
     * @return the list of constructors,
     */
    
    PsiMethod[] getConstructors();

    /**
     * Returns the list of inner classes for the class.
     *
     * @return the list of inner classes.
     */
     PsiClass[] getInnerClasses();

    /**
     * Returns the list of class initializers for the class.
     *
     * @return the list of class initializers.
     */
     PsiClassInitializer[] getInitializers();

    /**
     * Returns the list of fields in the class and all its superclasses.
     *
     * @return the list of fields.
     */
     PsiField[] getAllFields();

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
     PsiMethod[] getAllMethods();

    /**
     * Returns the list of inner classes for the class and all its superclasses..
     *
     * @return the list of inner classes.
     */
     PsiClass[] getAllInnerClasses();

    /**
     * Searches the class (and optionally its superclasses) for the field with the specified name.
     *
     * @param name       the name of the field to find.
     * @param checkBases if true, the field is also searched in the base classes of the class.
     * @return the field instance, or null if the field cannot be found.
     */
    
    PsiField findFieldByName( String name, boolean checkBases);

    /**
     * Searches the class (and optionally its superclasses) for the method with
     * the signature matching the signature of the specified method.
     *
     * @param patternMethod the method used as a pattern for the search.
     * @param checkBases    if true, the method is also searched in the base classes of the class.
     * @return the method instance, or null if the method cannot be found.
     */
    
    PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases);

    /**
     * Searches the class (and optionally its superclasses) for the methods with the signature
     * matching the signature of the specified method. If the superclasses are not searched,
     * the method returns multiple results only in case of a syntax error (duplicate method).
     *
     * @param patternMethod the method used as a pattern for the search.
     * @param checkBases    if true, the method is also searched in the base classes of the class.
     * @return the found methods, or an empty array if no methods are found.
     */
    
    PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases);

    /**
     * Searches the class (and optionally its superclasses) for the methods with the specified name.
     *
     * @param name       the name of the methods to find.
     * @param checkBases if true, the methods are also searched in the base classes of the class.
     * @return the found methods, or an empty array if no methods are found.
     */
    
    PsiMethod[] findMethodsByName( String name, boolean checkBases);

    /**
     * Searches the class (and optionally its superclasses) for the methods with the specified name
     * and returns the methods along with their substitutors.
     *
     * @param name       the name of the methods to find.
     * @param checkBases if true, the methods are also searched in the base classes of the class.
     * @return the found methods and their substitutors, or an empty list if no methods are found.
     */
    
    List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName( String name, boolean checkBases);

    /**
     * Returns the list of methods in the class and all its superclasses, along with their
     * substitutors.
     *
     * @return the list of methods and their substitutors
     */
    
    List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors();

    /**
     * Searches the class (and optionally its superclasses) for the inner class with the specified name.
     *
     * @param name       the name of the inner class to find.
     * @param checkBases if true, the inner class is also searched in the base classes of the class.
     * @return the inner class instance, or null if the inner class cannot be found.
     */
    
    PsiClass findInnerClassByName( String name, boolean checkBases);

    /**
     * Returns the token representing the opening curly brace of the class.
     *
     * @return the token instance, or null if the token is missing in the source code file.
     */
    
    PsiElement getLBrace();

    /**
     * Returns the token representing the closing curly brace of the class.
     *
     * @return the token instance, or null if the token is missing in the source code file.
     */
    
    PsiElement getRBrace();

    /**
     * Returns the name identifier of the class.
     *
     * @return the name identifier, or null if the class is anonymous or synthetic jsp class
     */
    @Override
    
    PsiIdentifier getNameIdentifier();

    /**
     * Returns the PSI member in which the class has been declared (for example,
     * the method containing the anonymous inner class, or the file containing a regular
     * class, or the class owning a type parameter).
     *
     * @return the member in which the class has been declared.
     */
    PsiElement getScope();

    /**
     * Checks if this class is an inheritor of the specified base class.
     * Only java inheritance rules are considered.
     * Note that {@link com.gome.maven.psi.search.searches.ClassInheritorsSearch}
     *  may return classes that are inheritors in broader, e.g. in ejb sense, but not in java sense.
     *
     * @param baseClass the base class to check the inheritance.
     * @param checkDeep if false, only direct inheritance is checked; if true, the base class is
     *                  searched in the entire inheritance chain
     * @return true if the class is an inheritor, false otherwise
     */
    boolean isInheritor( PsiClass baseClass, boolean checkDeep);

    /**
     * Checks if this class is a deep inheritor of the specified base class possibly bypassing a class
     * when checking inheritance chain.
     * Only java inheritance rules are considered.
     * Note that {@link com.gome.maven.psi.search.searches.ClassInheritorsSearch}
     *  may return classes that are inheritors in broader, e.g. in ejb sense, but not in java sense.
     *
     * @param baseClass the base class to check the inheritance.
     *                  searched in the entire inheritance chain
     * @param classToByPass class to bypass the inheratance check for
     * @return true if the class is an inheritor, false otherwise
     */
    boolean isInheritorDeep(PsiClass baseClass,  PsiClass classToByPass);

    /**
     * For an inner class, returns its containing class.
     *
     * @return the containing class, or null if the class is not an inner class.
     */
    @Override
    
    PsiClass getContainingClass();

    /**
     * Returns the hierarchical signatures for all methods in the specified class and
     * its superclasses and superinterfaces.
     *
     * @return the collection of signatures.
     * @since 5.1
     */
    
    Collection<HierarchicalMethodSignature> getVisibleSignatures();

    @Override
    PsiElement setName(  String name) throws IncorrectOperationException;
}
