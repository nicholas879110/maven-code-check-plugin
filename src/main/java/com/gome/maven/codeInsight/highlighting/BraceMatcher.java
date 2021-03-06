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

package com.gome.maven.codeInsight.highlighting;

import com.gome.maven.openapi.editor.highlighter.HighlighterIterator;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeExtensionPoint;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.tree.IElementType;

public interface BraceMatcher {
    ExtensionPointName<FileTypeExtensionPoint<BraceMatcher>> EP_NAME = new ExtensionPointName<FileTypeExtensionPoint<BraceMatcher>>("com.gome.maven.braceMatcher");

    int getBraceTokenGroupId(IElementType tokenType);
    boolean isLBraceToken(HighlighterIterator iterator,CharSequence fileText, FileType fileType);
    boolean isRBraceToken(HighlighterIterator iterator,CharSequence fileText, FileType fileType);
    boolean isPairBraces(IElementType tokenType,IElementType tokenType2);
    boolean isStructuralBrace(HighlighterIterator iterator,CharSequence text, FileType fileType);
     IElementType getOppositeBraceTokenType( IElementType type);
    boolean isPairedBracesAllowedBeforeType( IElementType lbraceType,  IElementType contextType);

    /**
     * Returns the start offset of the code construct which owns the opening structural brace at the specified offset. For example,
     * if the opening brace belongs to an 'if' statement, returns the start offset of the 'if' statement.
     *
     * @param file the file in which brace matching is performed.
     * @param openingBraceOffset the offset of an opening structural brace.
     * @return the offset of corresponding code construct, or the same offset if not defined.
     */
    int getCodeConstructStart(final PsiFile file, int openingBraceOffset);
}
