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
package com.gome.maven.ide.util;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * User: anna
 * Date: Jan 25, 2005
 */
public abstract class TreeClassChooserFactory {

    public static TreeClassChooserFactory getInstance(Project project) {
        return ServiceManager.getService(project, TreeClassChooserFactory.class);
    }


    
    public abstract TreeClassChooser createWithInnerClassesScopeChooser(String title,
                                                                        GlobalSearchScope scope,
                                                                        final ClassFilter classFilter,
                                                                         PsiClass initialClass);


    
    public abstract TreeClassChooser createNoInnerClassesScopeChooser(String title,
                                                                      GlobalSearchScope scope,
                                                                      ClassFilter classFilter,
                                                                       PsiClass initialClass);


    
    public abstract TreeClassChooser createProjectScopeChooser(String title,  PsiClass initialClass);


    
    public abstract TreeClassChooser createProjectScopeChooser(String title);


    
    public abstract TreeClassChooser createAllProjectScopeChooser(String title);


    
    public abstract TreeClassChooser createInheritanceClassChooser(String title,
                                                                   GlobalSearchScope scope,
                                                                   PsiClass base,
                                                                   boolean acceptsSelf,
                                                                   boolean acceptInner,
                                                                   
                                                                           Condition<? super PsiClass> additionalCondition);

    
    public abstract TreeClassChooser createInheritanceClassChooser(String title,
                                                                   GlobalSearchScope scope,
                                                                   PsiClass base,
                                                                   PsiClass initialClass);

    
    public abstract TreeClassChooser createInheritanceClassChooser(String title,
                                                                   GlobalSearchScope scope,
                                                                   PsiClass base,
                                                                   PsiClass initialClass,
                                                                   ClassFilter classFilter);


    
    public abstract TreeFileChooser createFileChooser( String title,
                                                       PsiFile initialFile,
                                                       FileType fileType,
                                                       TreeFileChooser.PsiFileFilter filter);


    
    public abstract TreeFileChooser createFileChooser( String title,
                                                       PsiFile initialFile,
                                                       FileType fileType,
                                                       TreeFileChooser.PsiFileFilter filter,
                                                      boolean disableStructureProviders);


    
    public abstract TreeFileChooser createFileChooser( String title,
                                                       PsiFile initialFile,
                                                       FileType fileType,
                                                       TreeFileChooser.PsiFileFilter filter,
                                                      boolean disableStructureProviders,
                                                      boolean showLibraryContents);
}
