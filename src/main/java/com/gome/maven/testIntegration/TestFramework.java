/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.gome.maven.testIntegration;

import com.gome.maven.ide.fileTemplates.FileTemplateDescriptor;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;

public interface TestFramework {
    ExtensionPointName<TestFramework> EXTENSION_NAME = ExtensionPointName.create("com.gome.maven.testFramework");

    
    String getName();

    
    Icon getIcon();

    boolean isLibraryAttached( Module module);

    
    String getLibraryPath();

    
    String getDefaultSuperClass();

    boolean isTestClass( PsiElement clazz);

    boolean isPotentialTestClass( PsiElement clazz);

    
    PsiElement findSetUpMethod( PsiElement clazz);

    
    PsiElement findTearDownMethod( PsiElement clazz);

    
    PsiElement findOrCreateSetUpMethod( PsiElement clazz) throws IncorrectOperationException;

    FileTemplateDescriptor getSetUpMethodFileTemplateDescriptor();

    FileTemplateDescriptor getTearDownMethodFileTemplateDescriptor();

    FileTemplateDescriptor getTestMethodFileTemplateDescriptor();

    /**
     * should be checked for abstract method error
     */
    boolean isIgnoredMethod(PsiElement element);

    /**
     * should be checked for abstract method error
     */
    boolean isTestMethod(PsiElement element);

    
    Language getLanguage();
}
