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

package com.gome.maven.util.xml.converters.values;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.ProjectScope;
import com.gome.maven.util.xml.*;

public abstract class ClassValueConverter extends Converter<PsiClass> implements CustomReferenceConverter {

    public static ClassValueConverter getClassValueConverter() {
        return ServiceManager.getService(ClassValueConverter.class);
    }

    public PsiClass fromString(  String s, final ConvertContext context) {
        if (s == null) return null;
        final Module module = context.getModule();
        final PsiFile psiFile = context.getFile();
        final Project project = psiFile.getProject();
        return DomJavaUtil.findClass(s, context.getFile(), context.getModule(), getScope(project, module, psiFile));
    }

    public String toString( PsiClass psiClass, final ConvertContext context) {
        return psiClass == null? null : psiClass.getQualifiedName();
    }

    
    public abstract PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context);

    public static GlobalSearchScope getScope(Project project,  Module module,  PsiFile psiFile) {
        if (module == null || psiFile == null) {
            return ProjectScope.getAllScope(project);
        }
        VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
        if (file == null) {
            return ProjectScope.getAllScope(project);
        }
        final boolean inTests = ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(file);

        return module.getModuleRuntimeScope(inTests);
    }
}
