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
 * @author max
 */
package com.gome.maven.psi.impl.java.stubs;

import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.stubs.NamedStub;

public interface PsiClassStub<T extends PsiClass> extends NamedStub<T> {
    
    
    String getQualifiedName();

    
    
    String getBaseClassReferenceText();

    boolean isDeprecated();
    boolean hasDeprecatedAnnotation();
    boolean isInterface();
    boolean isEnum();
    boolean isEnumConstantInitializer();
    boolean isAnonymous();
    boolean isAnonymousInQualifiedNew();
    boolean isAnnotationType();

    LanguageLevel getLanguageLevel();
    String getSourceFileName();
}