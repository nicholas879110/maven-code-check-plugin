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

package com.gome.maven.psi;

import com.gome.maven.openapi.extensions.ExtensionPointName;

/**
 * Describes logic for injecting language inside hosting PSI element.
 * E.g. "inject XPath language into all XML attributes named 'select' that sit inside XML tag prefixed with 'xsl:'".
 * @see com.gome.maven.psi.PsiLanguageInjectionHost
 * @see com.gome.maven.lang.injection.MultiHostInjector
 */
public interface LanguageInjector {
    ExtensionPointName<LanguageInjector> EXTENSION_POINT_NAME = ExtensionPointName.create("com.gome.maven.languageInjector");

    /**
     * @param host PSI element inside which your language will be injected.
     * @param injectionPlacesRegistrar stores places where injection occurs. <br>
     *        Call its {@link com.gome.maven.psi.InjectedLanguagePlaces#addPlace(com.gome.maven.lang.Language, com.gome.maven.openapi.util.TextRange, String, String)}
     *        method to register particular injection place.
     *        For example, to inject your language in string literal inside quotes, you might want to <br>
     *        <code>injectionPlacesRegistrar.addPlace(myLanguage, new TextRange(1,host.getTextLength()-1))</code>
     */
    void getLanguagesToInject( PsiLanguageInjectionHost host,  InjectedLanguagePlaces injectionPlacesRegistrar);
}
