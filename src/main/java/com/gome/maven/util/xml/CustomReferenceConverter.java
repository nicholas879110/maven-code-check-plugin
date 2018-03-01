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

import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.PsiElement;

/**
 * Can be implemented by {@link com.gome.maven.util.xml.Converter} instance, or used with
 * {@link @com.gome.maven.util.xml.Referencing} annotation.
 *
 * @author peter
 */
public interface CustomReferenceConverter<T> {

    /**
     * Will be called on creating {@link com.gome.maven.psi.PsiReference}s for {@link com.gome.maven.util.xml.GenericDomValue}
     * Returned {@link com.gome.maven.psi.PsiReference}s should be soft ({@link com.gome.maven.psi.PsiReference#isSoft()} should return <code>true</code>).
     * To highlight unresolved references, create a {@link com.gome.maven.util.xml.highlighting.DomElementsInspection} and register it.
     *
     * @param value GenericDomValue in question
     * @param element corresponding PSI element
     * @param context {@link com.gome.maven.util.xml.ConvertContext}
     * @return custom {@link com.gome.maven.psi.PsiReference}s for the value
     */

    PsiReference[] createReferences(GenericDomValue<T> value, PsiElement element, ConvertContext context);
}
