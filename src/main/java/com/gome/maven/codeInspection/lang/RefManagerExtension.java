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
 * User: anna
 * Date: 20-Dec-2007
 */
package com.gome.maven.codeInspection.lang;

import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefVisitor;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiElement;
import org.jdom.Element;

public interface RefManagerExtension<T> {
    
    Key<T> getID();

    
    Language getLanguage();

    void iterate( RefVisitor visitor);

    void cleanup();

    void removeReference(RefElement refElement);

    
    RefElement createRefElement(PsiElement psiElement);

    
    RefEntity getReference(final String type, final String fqName);

    
    String getType(RefEntity entity);

    
    RefEntity getRefinedElement( RefEntity ref);

    void visitElement(final PsiElement element);

    
    String getGroupName(final RefEntity entity);

    boolean belongsToScope(final PsiElement psiElement);

    void export( RefEntity refEntity,  Element element);

    void onEntityInitialized(RefElement refEntity, PsiElement psiElement);
}
