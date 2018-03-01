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

import com.gome.maven.codeInsight.completion.CompletionProcess;
import com.gome.maven.codeInsight.completion.CompletionService;
import com.gome.maven.psi.tree.IElementType;

public class DefaultWordCompletionFilter implements WordCompletionElementFilter {
    @Override
    public boolean isWordCompletionEnabledIn(final IElementType element) {
        final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(element.getLanguage());
        return parserDefinition != null && parserDefinition.getCommentTokens().contains(element);
    }
}