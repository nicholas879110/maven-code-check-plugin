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
 * @author max
 */
package com.gome.maven.psi;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Map;

public abstract class JavaDirectoryService {
    public static JavaDirectoryService getInstance() {
        return ServiceManager.getService(JavaDirectoryService.class);
    }

    /**
     * Returns the package corresponding to the directory.
     *
     * @return the package instance, or null if the directory does not correspond to any package.
     */
    
    public abstract PsiPackage getPackage( PsiDirectory dir);

    /**
     * Returns the list of Java classes contained in the directory.
     *
     * @return the array of classes.
     */
    
    public abstract PsiClass[] getClasses( PsiDirectory dir);

    /**
     * Creates a class with the specified name in the directory.
     *
     * @param name the name of the class to create (not including the file extension).
     * @return the created class instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     */
    
    public abstract PsiClass createClass( PsiDirectory dir,  String name) throws IncorrectOperationException;

    /**
     * Creates a class with the specified name in the directory.
     *
     * @param name the name of the class to create (not including the file extension).
     * @param templateName custom file template to create class text based on.
     * @return the created class instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     * @since 5.1
     */
    
    public abstract PsiClass createClass( PsiDirectory dir,  String name,  String templateName) throws IncorrectOperationException;

    /**
     * @param askForUndefinedVariables
     *  true show dialog asking for undefined variables
     *  false leave them blank
     */
    public abstract PsiClass createClass( PsiDirectory dir,  String name,  String templateName, boolean askForUndefinedVariables) throws IncorrectOperationException;

    /**
     * @param additionalProperties additional properties to be substituted in the template
     */
    public abstract PsiClass createClass( PsiDirectory dir,
                                          String name,
                                          String templateName,
                                         boolean askForUndefinedVariables,
                                          final Map<String, String> additionalProperties) throws IncorrectOperationException;

    /**
     * Checks if it's possible to create a class with the specified name in the directory,
     * and throws an exception if the creation is not possible. Does not actually modify
     * anything.
     *
     * @param name the name of the class to check creation possibility (not including the file extension).
     * @throws IncorrectOperationException if the creation is not possible.
     */
    public abstract void checkCreateClass( PsiDirectory dir,  String name) throws IncorrectOperationException;

    /**
     * Creates an interface class with the specified name in the directory.
     *
     * @param name the name of the interface to create (not including the file extension).
     * @return the created interface instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     */
    
    public abstract PsiClass createInterface( PsiDirectory dir,  String name) throws IncorrectOperationException;

    /**
     * Creates an enumeration class with the specified name in the directory.
     *
     * @param name the name of the enumeration class to create (not including the file extension).
     * @return the created class instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     */
    
    public abstract PsiClass createEnum( PsiDirectory dir,  String name) throws IncorrectOperationException;

    /**
     * Creates an annotation class with the specified name in the directory.
     *
     * @param name the name of the annotation class to create (not including the file extension).
     * @return the created class instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     */
    
    public abstract PsiClass createAnnotationType( PsiDirectory dir,  String name) throws IncorrectOperationException;


    /**
     * Checks if the directory is a source root for the project to which it belongs.
     *
     * @return true if the directory is a source root, false otherwise
     */
    public abstract boolean isSourceRoot( PsiDirectory dir);

    public abstract LanguageLevel getLanguageLevel( PsiDirectory dir);
}