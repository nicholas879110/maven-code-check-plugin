/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.psi.tree;

import com.gome.maven.lang.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;

/**
 * A token type which represents a fragment of text (possibly in a different language)
 * which is not parsed during the current lexer or parser pass and can be parsed later when
 * its contents is requested.
 *
 * @author max
 */

public class ILazyParseableElementType extends IElementType {
    public ILazyParseableElementType(  final String debugName) {
        this(debugName, null);
    }

    public ILazyParseableElementType(  final String debugName,  final Language language) {
        super(debugName, language);
    }

    public ILazyParseableElementType(  final String debugName,  final Language language, final boolean register) {
        super(debugName, language, register);
    }

    /**
     * Parses the contents of the specified chameleon node and returns the AST tree
     * representing the parsed contents.
     *
     * @param chameleon the node to parse.
     * @return the parsed contents of the node.
     */
    public ASTNode parseContents(final ASTNode chameleon) {
        final PsiElement parentElement = chameleon.getTreeParent().getPsi();
        assert parentElement != null : "Bad chameleon: " + chameleon;
        return doParseContents(chameleon, parentElement);
    }

    protected ASTNode doParseContents( final ASTNode chameleon,  final PsiElement psi) {
        final Project project = psi.getProject();
        Language languageForParser = getLanguageForParser(psi);
        final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.getChars());
        final PsiParser parser = LanguageParserDefinitions.INSTANCE.forLanguage(languageForParser).createParser(project);
        return parser.parse(this, builder).getFirstChildNode();
    }

    protected Language getLanguageForParser(PsiElement psi) {
        return getLanguage();
    }

    
    public ASTNode createNode(CharSequence text) {
        return null;
    }
}
