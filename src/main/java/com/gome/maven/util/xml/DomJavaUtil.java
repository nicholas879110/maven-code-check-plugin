/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.util.xml;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.JavaPsiFacade;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * @author peter
 */
public class DomJavaUtil {
    private DomJavaUtil() {
    }

    
    public static PsiClass findClass( String name,  PsiFile file,  final Module module,  final GlobalSearchScope searchScope) {
        if (name == null) return null;
        if (name.indexOf('$')>=0) name = name.replace('$', '.');

        final GlobalSearchScope scope;
        if (searchScope == null) {

            if (module != null) {
                file = file.getOriginalFile();
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile == null) {
                    scope = GlobalSearchScope.moduleRuntimeScope(module, true);
                }
                else {
                    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
                    boolean tests = fileIndex.isInTestSourceContent(virtualFile);
                    scope = module.getModuleRuntimeScope(tests);
                }
            }
            else {
                scope = file.getResolveScope();
            }
        }
        else {
            scope = searchScope;
        }

        final PsiClass aClass = JavaPsiFacade.getInstance(file.getProject()).findClass(name, scope);
        if (aClass != null) {
            assert aClass.isValid() : name;
        }
        return aClass;
    }

    
    public static PsiClass findClass( String name,  DomElement element) {
        assert element.isValid();
        if (DomUtil.hasXml(element)) {
            return findClass(name, DomUtil.getFile(element), element.getModule(), element.getResolveScope());
        }
        return null;
    }
}
