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
package com.gome.maven.lang;

public class CodeInsightActions {
    public static final LanguageExtension<LanguageCodeInsightActionHandler>
            IMPLEMENT_METHOD = new LanguageExtension<LanguageCodeInsightActionHandler>("com.gome.maven.codeInsight.implementMethod");

    public static final LanguageExtension<LanguageCodeInsightActionHandler>
            OVERRIDE_METHOD = new LanguageExtension<LanguageCodeInsightActionHandler>("com.gome.maven.codeInsight.overrideMethod");

    public static final LanguageExtension<LanguageCodeInsightActionHandler>
            DELEGATE_METHODS = new LanguageExtension<LanguageCodeInsightActionHandler>("com.gome.maven.codeInsight.delegateMethods");

    public static final LanguageExtension<LanguageCodeInsightActionHandler>
            GOTO_SUPER = new LanguageExtension<LanguageCodeInsightActionHandler>("com.gome.maven.codeInsight.gotoSuper");

    private CodeInsightActions() {
    }
}