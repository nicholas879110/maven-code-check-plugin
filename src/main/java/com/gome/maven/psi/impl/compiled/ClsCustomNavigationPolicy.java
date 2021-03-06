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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiElement;

/** @deprecated API no longer supported, use {@link com.gome.maven.psi.compiled.ClassFileDecompilers} instead (to remove in IDEA 14) */
@SuppressWarnings({"UnusedDeclaration", "deprecation"})
public interface ClsCustomNavigationPolicy {
    ExtensionPointName<ClsCustomNavigationPolicy> EP_NAME = ExtensionPointName.create("com.gome.maven.psi.clsCustomNavigationPolicy");

    
    PsiElement getNavigationElement( ClsClassImpl clsClass);

    
    PsiElement getNavigationElement( ClsMethodImpl clsMethod);

    
    PsiElement getNavigationElement( ClsFieldImpl clsField);
}
