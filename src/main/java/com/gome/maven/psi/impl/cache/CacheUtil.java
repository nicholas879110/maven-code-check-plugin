/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.gome.maven.psi.impl.cache;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageParserDefinitions;
import com.gome.maven.lang.ParserDefinition;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;

public class CacheUtil {

    public static boolean isInComments(final IElementType tokenType) {
        final Language language = tokenType.getLanguage();
        boolean inComments = false;

        final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);

        if (parserDefinition != null) {
            final TokenSet commentTokens = parserDefinition.getCommentTokens();

            if (commentTokens.contains(tokenType)) {
                inComments = true;
            }
        }
        return inComments;
    }
}
