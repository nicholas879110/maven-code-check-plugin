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
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Sep 10, 2007
 * Time: 1:58:29 PM
 */
package com.gome.maven.lang.injection;

import com.gome.maven.lang.Language;
import com.gome.maven.psi.PsiLanguageInjectionHost;
import com.gome.maven.openapi.util.TextRange;

public interface MultiHostRegistrar {
     /*this*/ MultiHostRegistrar startInjecting( Language language);
     /*this*/ MultiHostRegistrar addPlace(  String prefix,   String suffix,  PsiLanguageInjectionHost host,  TextRange rangeInsideHost);
    void doneInjecting();
}