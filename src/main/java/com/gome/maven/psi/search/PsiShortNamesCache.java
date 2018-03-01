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
package com.gome.maven.psi.search;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashSet;
import com.gome.maven.util.indexing.IdFilter;

/**
 * Allows to retrieve files and Java classes, methods and fields in a project by
 * non-qualified names.
 *
 */
public abstract class PsiShortNamesCache {
    /**
     * Return the composite short names cache, uniting all short name cache instances registered via extensions.
     *
     * @param project the project to return the cache for.
     * @return the cache instance.
     */

    public static PsiShortNamesCache getInstance(Project project) {
        return ServiceManager.getService(project, PsiShortNamesCache.class);
    }

    public static final ExtensionPointName<PsiShortNamesCache> EP_NAME = ExtensionPointName.create("com.gome.maven.java.shortNamesCache");

    /**
     * Returns the list of files with the specified name.
     *
     * @param name the name of the files to find.
     * @return the list of files in the project which have the specified name.
     */
    
    public PsiFile[] getFilesByName( String name) {
        return PsiFile.EMPTY_ARRAY;
    }

    /**
     * Returns the list of names of all files in the project.
     *
     * @return the list of all file names in the project.
     */
    
    public String[] getAllFileNames() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    /**
     * Returns the list of all classes with the specified name in the specified scope.
     *
     * @param name  the non-qualified name of the classes to find.
     * @param scope the scope in which classes are searched.
     * @return the list of found classes.
     */
    
    public abstract PsiClass[] getClassesByName(  String name,  GlobalSearchScope scope);

    /**
     * Returns the list of names of all classes in the project and
     * (optionally) libraries.
     *
     * @return the list of all class names.
     */
    
    public abstract String[] getAllClassNames();

    public boolean processAllClassNames(Processor<String> processor) {
        return ContainerUtil.process(getAllClassNames(), processor);
    }

    public boolean processAllClassNames(Processor<String> processor, GlobalSearchScope scope, IdFilter filter) {
        return ContainerUtil.process(getAllClassNames(), processor);
    }

    /**
     * Adds the names of all classes in the project and (optionally) libraries
     * to the specified set.
     *
     * @param dest the set to add the names to.
     */
    public abstract void getAllClassNames( HashSet<String> dest);

    /**
     * Returns the list of all methods with the specified name in the specified scope.
     *
     * @param name  the name of the methods to find.
     * @param scope the scope in which methods are searched.
     * @return the list of found methods.
     */
    
    public abstract PsiMethod[] getMethodsByName(  String name,  GlobalSearchScope scope);

    
    public abstract PsiMethod[] getMethodsByNameIfNotMoreThan(  String name,  GlobalSearchScope scope, int maxCount);
    
    public abstract PsiField[] getFieldsByNameIfNotMoreThan(  String name,  GlobalSearchScope scope, int maxCount);

    public abstract boolean processMethodsWithName(  String name,  GlobalSearchScope scope,  Processor<PsiMethod> processor);

    public boolean processMethodsWithName(  String name,  final Processor<? super PsiMethod> processor,
                                           GlobalSearchScope scope,  IdFilter filter) {
        return processMethodsWithName(name, scope, new Processor<PsiMethod>() {
            @Override
            public boolean process(PsiMethod method) {
                return processor.process(method);
            }
        });
    }

    public boolean processAllMethodNames(Processor<String> processor, GlobalSearchScope scope, IdFilter filter) {
        return ContainerUtil.process(getAllFieldNames(), processor);
    }

    public boolean processAllFieldNames(Processor<String> processor, GlobalSearchScope scope, IdFilter filter) {
        return ContainerUtil.process(getAllFieldNames(), processor);
    }

    /**
     * Returns the list of names of all methods in the project and
     * (optionally) libraries.
     *
     * @return the list of all method names.
     */
    
    public abstract String[] getAllMethodNames();

    /**
     * Adds the names of all methods in the project and (optionally) libraries
     * to the specified set.
     *
     * @param set the set to add the names to.
     */
    public abstract void getAllMethodNames( HashSet<String> set);

    /**
     * Returns the list of all fields with the specified name in the specified scope.
     *
     * @param name  the name of the fields to find.
     * @param scope the scope in which fields are searched.
     * @return the list of found fields.
     */
    
    public abstract PsiField[] getFieldsByName(  String name,  GlobalSearchScope scope);

    /**
     * Returns the list of names of all fields in the project and
     * (optionally) libraries.
     *
     * @return the list of all field names.
     */
    
    public abstract String[] getAllFieldNames();

    /**
     * Adds the names of all methods in the project and (optionally) libraries
     * to the specified set.
     *
     * @param set the set to add the names to.
     */
    public abstract void getAllFieldNames( HashSet<String> set);

    public boolean processFieldsWithName( String name,  Processor<? super PsiField> processor,
                                          GlobalSearchScope scope,  IdFilter filter) {
        return ContainerUtil.process(getFieldsByName(name, scope), processor);
    }

    public boolean processClassesWithName( String name,  Processor<? super PsiClass> processor,
                                           GlobalSearchScope scope,  IdFilter filter) {
        return ContainerUtil.process(getClassesByName(name, scope), processor);
    }
}
