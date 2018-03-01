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
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;

/**
 * @author peter
 */
public abstract class AbstractConvertContext extends ConvertContext {

    @Override
    public final XmlTag getTag() {
        return getInvocationElement().getXmlTag();
    }

    @Override
    
    public XmlElement getXmlElement() {
        return getInvocationElement().getXmlElement();
    }

    @Override
    
    public final XmlFile getFile() {
        return DomUtil.getFile(getInvocationElement());
    }

    @Override
    public Module getModule() {
        final DomFileElement<DomElement> fileElement = DomUtil.getFileElement(getInvocationElement());
        if (fileElement == null) {
            final XmlElement xmlElement = getInvocationElement().getXmlElement();
            return xmlElement == null ? null : ModuleUtilCore.findModuleForPsiElement(xmlElement);
        }
        return fileElement.isValid() ? fileElement.getRootElement().getModule() : null;
    }

    @Override
    public PsiManager getPsiManager() {
        return getFile().getManager();
    }

    @Override
    
    public GlobalSearchScope getSearchScope() {
        GlobalSearchScope scope = null;

        Module[] modules = getConvertContextModules();
        if (modules.length != 0) {

            PsiFile file = getFile();
            file = file.getOriginalFile();
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                ProjectFileIndex fileIndex = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
                boolean tests = fileIndex.isInTestSourceContent(virtualFile);

                for (Module module : modules) {
                    if (scope == null) {
                        scope = module.getModuleRuntimeScope(tests);
                    }
                    else {
                        scope = scope.union(module.getModuleRuntimeScope(tests));
                    }
                }
            }
        }
        return scope; // ??? scope == null ? GlobalSearchScope.allScope(getProject()) : scope; ???
    }

    
    private Module[] getConvertContextModules() {
        Module[] modules = ModuleContextProvider.getModules(getFile());
        if (modules.length > 0) return modules;

        final Module module = getModule();
        if (module != null) return new Module[]{module};

        return Module.EMPTY_ARRAY;
    }
}
