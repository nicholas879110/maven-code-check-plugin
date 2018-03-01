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

package com.gome.maven.util.xml.reflect;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.pom.PomTarget;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.xml.AnnotatedElement;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.DomNameStrategy;
import com.gome.maven.util.xml.ElementPresentationTemplate;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author peter
 */
public interface AbstractDomChildrenDescription extends AnnotatedElement, PomTarget {
    
    List<? extends DomElement> getValues( DomElement parent);

    
    List<? extends DomElement> getStableValues( DomElement parent);

    
    Type getType();

    
    DomNameStrategy getDomNameStrategy( DomElement parent);

    <T> T getUserData(Key<T> key);

    
    ElementPresentationTemplate getPresentationTemplate();

    
    PsiElement getDeclaration(Project project);

    
    DomElement getDomDeclaration();

    boolean isStubbed();
}
