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
package com.gome.maven.util.xml;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.xml.XmlAttribute;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;

/**
 * @author peter
 */
public abstract class ConvertContext {

    
    public abstract DomElement getInvocationElement();

    
    public abstract XmlTag getTag();

    
    public abstract XmlElement getXmlElement();

    
    public XmlElement getReferenceXmlElement() {
        final XmlElement element = getXmlElement();
        if (element instanceof XmlTag) {
            return element;
        }
        if (element instanceof XmlAttribute) {
            return ((XmlAttribute)element).getValueElement();
        }
        return null;
    }

    
    public abstract XmlFile getFile();

    
    public abstract Module getModule();

    
    public abstract GlobalSearchScope getSearchScope();

    public abstract PsiManager getPsiManager();

    public Project getProject() {
        return getPsiManager().getProject();
    }
}
