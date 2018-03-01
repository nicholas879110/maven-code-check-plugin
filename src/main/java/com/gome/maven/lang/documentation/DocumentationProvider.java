/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.lang.documentation;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;

import java.util.List;

/**
 * @see com.gome.maven.lang.LanguageDocumentation
 * @see DocumentationProviderEx
 * @see AbstractDocumentationProvider
 */
public interface DocumentationProvider {

    /**
     * Please use {@link com.gome.maven.lang.LanguageDocumentation} instead of this for language-specific documentation
     */
    ExtensionPointName<DocumentationProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.documentationProvider");

    /**
     * Returns the text to show in the Ctrl-hover popup for the specified element.
     *
     * @param element         the element for which the documentation is requested (for example, if the mouse is over
     *                        a method reference, this will be the method to which the reference is resolved).
     * @param originalElement the element under the mouse cursor
     * @return the documentation to show, or null if the provider can't provide any documentation for this element.
     */
    
    String getQuickNavigateInfo(PsiElement element, PsiElement originalElement);

    /**
     * Returns the list of possible URLs to show as external documentation for the specified element.
     * @param element         the element for which the documentation is requested (for example, if the mouse is over
     *                        a method reference, this will be the method to which the reference is resolved).
     * @param originalElement the element under the mouse cursor
     * @return the list of URLs to open in the browser. If the list contains a single URL, it will be opened.
     *         If the list contains multiple URls, the user will be prompted to choose one of them.
     */
    
    List<String> getUrlFor(PsiElement element, PsiElement originalElement);

    /**
     * Callback for asking the doc provider for the complete documentation.
     * <p/>
     * Underlying implementation may be time-consuming, that's why this method is expected not to be called from EDT.
     *
     * @param element         the element for which the documentation is requested (for example, if the mouse is over
     *                        a method reference, this will be the method to which the reference is resolved).
     * @param originalElement the element under the mouse cursor
     * @return                target element's documentation (if any)
     */
    
    String generateDoc(PsiElement element,  PsiElement originalElement);

    
    PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element);

    
    PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context);
}
