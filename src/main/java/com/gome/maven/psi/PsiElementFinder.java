/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.Processor;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Allows to extend the mechanism of locating classes and packages by full-qualified name.
 * Implementations of this interface need to be registered as extensions in order
 * to be picked up by {@link JavaPsiFacade}.
 */
public abstract class PsiElementFinder {
    public static final ExtensionPointName<PsiElementFinder> EP_NAME = ExtensionPointName.create("com.gome.maven.java.elementFinder");

    /**
     * Searches the specified scope within the project for a class with the specified full-qualified
     * name and returns one if it is found.
     *
     * @param qualifiedName the full-qualified name of the class to find.
     * @param scope the scope to search.
     * @return the PSI class, or null if no class with such name is found.
     * @see JavaPsiFacade#findClass(String, GlobalSearchScope)
     */
    
    public abstract PsiClass findClass( String qualifiedName,  GlobalSearchScope scope);

    /**
     * Searches the specified scope within the project for classes with the specified full-qualified
     * name and returns all found classes.
     *
     * @param qualifiedName the full-qualified name of the class to find.
     * @param scope the scope to search.
     * @return the array of found classes, or an empty array if no classes are found.
     * @see JavaPsiFacade#findClasses(String, GlobalSearchScope)
     */
    
    public abstract PsiClass[] findClasses( String qualifiedName,  GlobalSearchScope scope);

    /**
     * Searches the project for the package with the specified full-qualified name and returns one
     * if it is found.
     *
     * @param qualifiedName the full-qualified name of the package to find.
     * @return the PSI package, or null if no package with such name is found.
     * @see JavaPsiFacade#findPackage(String)
     */
    
    public PsiPackage findPackage( String qualifiedName) {
        return null;
    }

    /**
     * Returns the list of subpackages of the specified package in the specified search scope.
     *
     * @param psiPackage the package to return the list of subpackages for.
     * @param scope the scope in which subpackages are searched.
     * @return the list of subpackages.
     * @see PsiPackage#getSubPackages(GlobalSearchScope)
     */
    
    public PsiPackage[] getSubPackages( PsiPackage psiPackage,  GlobalSearchScope scope) {
        return PsiPackage.EMPTY_ARRAY;
    }

    /**
     * Returns the list of classes in the specified package and in the specified search scope.
     *
     * @param psiPackage the package to return the list of classes in.
     * @param scope the scope in which classes are searched.
     * @return the list of classes.
     * @see PsiPackage#getClasses(GlobalSearchScope)
     */
    
    public PsiClass[] getClasses( PsiPackage psiPackage,  GlobalSearchScope scope) {
        return PsiClass.EMPTY_ARRAY;
    }

    /**
     * Returns the filter to exclude classes, for example derived classes.
     *
     * @param scope the scope in which classes are searched.
     * @return the filter to use, or null if no additional filtering is necessary
     */
    
    public Condition<PsiClass> getClassesFilter( GlobalSearchScope scope) {
        return null;
    }

    /**
     * Returns a list of files belonging to the specified package which are not located in any of the package directories.
     *
     * @param psiPackage the package to return the list of files for.
     * @param scope      the scope in which files are searched.
     * @return the list of files.
     * @since 14.1
     */
    
    public PsiFile[] getPackageFiles( PsiPackage psiPackage,  GlobalSearchScope scope) {
        return PsiFile.EMPTY_ARRAY;
    }

    /**
     * Returns the filter to use for filtering the list of files in the directories belonging to a package to exclude files
     * that actually belong to a different package. (For example, in Kotlin the package of a file is determined by its
     * package statement and not by its location in the directory structure, so the files which have a differring package
     * statement need to be excluded.)
     *
     * @param psiPackage the package for which the list of files is requested.
     * @param scope      the scope in which children are requested.
     * @return the filter to use, or null if no additional filtering is necessary.
     * @since 14.1
     */
    
    public Condition<PsiFile> getPackageFilesFilter( PsiPackage psiPackage,  GlobalSearchScope scope) {
        return null;
    }

    /**
     * A method to optimize resolve (to only search classes in a package which might be there)
     */
    
    public Set<String> getClassNames( PsiPackage psiPackage,  GlobalSearchScope scope) {
        return getClassNames(getClasses(psiPackage, scope));
    }

    
    protected static Set<String> getClassNames( PsiClass[] classes) {
        if (classes.length == 0) {
            return Collections.emptySet();
        }

        final HashSet<String> names = new HashSet<String>();
        for (PsiClass aClass : classes) {
            ContainerUtil.addIfNotNull(aClass.getName(), names);
        }
        return names;
    }

    public boolean processPackageDirectories( PsiPackage psiPackage,
                                              GlobalSearchScope scope,
                                              Processor<PsiDirectory> consumer) {
        return processPackageDirectories(psiPackage, scope, consumer, false);
    }

    public boolean processPackageDirectories( PsiPackage psiPackage,
                                              GlobalSearchScope scope,
                                              Processor<PsiDirectory> consumer,
                                             boolean includeLibrarySources) {
        return true;
    }

    /**
     * Returns the list of classes in the specified package and in the specified search scope.
     *
     * @param className short name of the class
     * @param psiPackage the package to return the list of classes in.
     * @param scope the scope in which classes are searched.
     * @return the list of classes.
     * @see PsiPackage#getClasses(GlobalSearchScope)
     */
    
    public PsiClass[] getClasses( String className,  PsiPackage psiPackage,  GlobalSearchScope scope) {
        PsiClass[] allClasses = getClasses(psiPackage, scope);
        if (className == null) return allClasses;
        return filterByName(className, allClasses);
    }

    
    public static PsiClass[] filterByName( String className,  PsiClass[] classes) {
        if (classes.length == 0) return PsiClass.EMPTY_ARRAY;
        if (classes.length == 1) {
            return className.equals(classes[0].getName()) ? classes : PsiClass.EMPTY_ARRAY;
        }
        List<PsiClass> foundClasses = new SmartList<PsiClass>();
        for (PsiClass psiClass : classes) {
            if (className.equals(psiClass.getName())) {
                foundClasses.add(psiClass);
            }
        }
        return foundClasses.isEmpty() ? PsiClass.EMPTY_ARRAY : foundClasses.toArray(new PsiClass[foundClasses.size()]);
    }

}