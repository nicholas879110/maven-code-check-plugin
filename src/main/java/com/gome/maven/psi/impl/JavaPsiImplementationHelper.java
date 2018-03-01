/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.psi.impl;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;

/**
 * @author yole
 */
public abstract class JavaPsiImplementationHelper {
    public static JavaPsiImplementationHelper getInstance(Project project) {
        return ServiceManager.getService(project, JavaPsiImplementationHelper.class);
    }

    public abstract PsiClass getOriginalClass(PsiClass psiClass);

    
    public abstract PsiElement getClsFileNavigationElement(PsiJavaFile clsFile);

    
    public abstract LanguageLevel getEffectiveLanguageLevel( VirtualFile virtualFile);

    public abstract ASTNode getDefaultImportAnchor(PsiImportList list, PsiImportStatementBase statement);

    
    public abstract PsiElement getDefaultMemberAnchor( PsiClass psiClass,  PsiMember firstPsi);

    public abstract void setupCatchBlock( String exceptionName,  PsiType exceptionType,
                                          PsiElement context,  PsiCatchSection element);
}
