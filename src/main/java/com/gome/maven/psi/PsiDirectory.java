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

import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.IncorrectOperationException;

/**
 * Represents a file system directory and allows to access its contents.
 */
public interface PsiDirectory extends PsiFileSystemItem {
    /**
     * The empty array of PSI directories which can be reused to avoid unnecessary allocations.
     */
    PsiDirectory[] EMPTY_ARRAY = new PsiDirectory[0];

    /**
     * Returns the virtual file represented by the PSI directory.
     *
     * @return the virtual file instance.
     */
    @Override
    
    VirtualFile getVirtualFile();

    @Override
    
    String getName();

    @Override
    
    PsiElement setName( String name) throws IncorrectOperationException;

    /**
     * Returns the parent directory of the directory.
     *
     * @return the parent directory, or null if the directory has no parent.
     */
    
    PsiDirectory getParentDirectory();

    @Override
    
    PsiDirectory getParent();

    /**
     * Returns the list of subdirectories of this directory.
     *
     * @return the array of subdirectories.
     */
    
    PsiDirectory[] getSubdirectories();

    /**
     * Returns the list of files in the directory.
     *
     * @return the array of files.
     */
    
    PsiFile[] getFiles();

    /**
     * Finds the subdirectory of this directory with the specified name.
     *
     * @param name the name of the subdirectory to find.
     * @return the subdirectory instance, or null if no subdirectory with such a name is found.
     */
    
    PsiDirectory findSubdirectory( String name);

    /**
     * Finds a file with the specified name in this directory.
     *
     * @param name the name of the file to find.
     * @return the file instance, or null if no file with such a name is found.
     */
    
    PsiFile findFile(  String name);

    /**
     * Creates a subdirectory with the specified name in the directory.
     *
     * @param name the name of the subdirectory to create.
     * @return the created directory instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     */
     PsiDirectory createSubdirectory( String name) throws IncorrectOperationException;

    /**
     * Checks if it's possible to create a subdirectory with the specified name in the directory,
     * and throws an exception if the creation is not possible. Does not actually modify
     * anything.
     *
     * @param name the name of the subdirectory to check creation possibility.
     * @throws IncorrectOperationException if the creation is not possible.
     */
    void checkCreateSubdirectory( String name) throws IncorrectOperationException;

    /**
     * Creates a file with the specified name in the directory.
     *
     * @param name the name of the file to create.
     * @return the created file instance.
     * @throws IncorrectOperationException if the operation failed for some reason.
     */
     PsiFile createFile(  String name) throws IncorrectOperationException;

     PsiFile copyFileFrom( String newName,  PsiFile originalFile) throws IncorrectOperationException;

    /**
     * Checks if it's possible to create a file with the specified name in the directory,
     * and throws an exception if the creation is not possible. Does not actually modify
     * anything.
     *
     * @param name the name of the file to check creation possibility.
     * @throws IncorrectOperationException if the creation is not possible.
     */
    void checkCreateFile( String name) throws IncorrectOperationException;
}
