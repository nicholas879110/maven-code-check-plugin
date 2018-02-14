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
package com.gome.maven.psi.formatter;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.codeStyle.CodeStyleSettings;
import com.gome.maven.psi.impl.source.tree.LeafElement;

/**
 * Abstract common {@link WhiteSpaceFormattingStrategy} implementation that doesn't replace default strategy and doesn't
 * adjust white space and
 *
 * @author Denis Zhdanov
 * @since Sep 22, 2010 10:19:07 AM
 */
public abstract class AbstractWhiteSpaceFormattingStrategy implements WhiteSpaceFormattingStrategy {

    @Override
    public boolean replaceDefaultStrategy() {
        return false;
    }

    
    @Override
    public CharSequence adjustWhiteSpaceIfNecessary( CharSequence whiteSpaceText,
                                                     CharSequence text,
                                                    int startOffset,
                                                    int endOffset, CodeStyleSettings codeStyleSettings, ASTNode nodeAfter)
    {
        // Does nothing
        return whiteSpaceText;
    }

    @Override
    public CharSequence adjustWhiteSpaceIfNecessary( CharSequence whiteSpaceText,
                                                     PsiElement startElement,
                                                    final int startOffset,
                                                    final int endOffset, CodeStyleSettings codeStyleSettings)
    {
        assert startElement.getTextRange().contains(startOffset)
                : String.format("Element: %s, range: %s, offset: %d", startElement, startElement.getTextRange(), startOffset);

        // Collect target text from the PSI elements and delegate to the text-based method.
        StringBuilder buffer = new StringBuilder();
        for (PsiElement current = startElement; current != null && current.getTextRange().getStartOffset() < endOffset; current = next(current)) {
            final TextRange range = current.getTextRange();
            final String text = current.getText();
            if (StringUtil.isEmpty(text)) {
                continue;
            }

            int start = startOffset > range.getStartOffset() ? startOffset - range.getStartOffset() : 0;
            if (start >= text.length()) {
                continue;
            }

            int end = endOffset < range.getEndOffset() ? text.length() - (range.getEndOffset() - endOffset) : text.length();
            if (end <= start) {
                continue;
            }

            if (start == 0 && end == text.length()) {
                buffer.append(text);
            }
            else {
                buffer.append(text.substring(start, end));
            }
        }

        return adjustWhiteSpaceIfNecessary(whiteSpaceText, buffer, 0, endOffset - startOffset, codeStyleSettings, null);
    }

    
    private static PsiElement next( final PsiElement element) {
        for (PsiElement anchor = element; anchor != null; anchor = anchor.getParent()) {
            final PsiElement result = element.getNextSibling();
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public boolean containsWhitespacesOnly( ASTNode node) {
        return false;
    }

    @Override
    public boolean addWhitespace( ASTNode treePrev,  LeafElement whiteSpaceElement) {
        return false;
    }
}