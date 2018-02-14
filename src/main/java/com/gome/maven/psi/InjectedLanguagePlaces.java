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

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.util.TextRange;

/**
 * Storage for places where PSI language being injected to.
 * @see com.gome.maven.psi.LanguageInjector for usage examples.
 * @see com.gome.maven.psi.PsiLanguageInjectionHost
 */
public interface InjectedLanguagePlaces {
    /**
     * Informs IDEA of the language injected inside the host element, which must be instanceof {@link com.gome.maven.psi.PsiLanguageInjectionHost}
     * @param language to inject inside the host element.
     * @param rangeInsideHost where to inject the language. Offsets are relative to the host element text range.
     *        E.g. for {@link com.gome.maven.psi.PsiLiteralExpression} it usually is <code>new TextRange(1, psiLiteral.getTextLength()-1)</code>,
     *        for injecting the language in string literal inside double quotes.  
     * @param prefix Optional header to be handed on to the language parser before the host element text.
     *        Might be useful e.g. for making the text parsable or providing some context.
     * @param suffix Optional footer to be passed on to the language parser after the host element text.
     *        Might be useful e.g. for making the text parsable or providing some context.
     */
    void addPlace( Language language,  TextRange rangeInsideHost,  String prefix,  String suffix);
}