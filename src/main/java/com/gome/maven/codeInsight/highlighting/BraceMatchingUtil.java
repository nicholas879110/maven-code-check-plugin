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

package com.gome.maven.codeInsight.highlighting;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageBraceMatching;
import com.gome.maven.lang.PairedBraceMatcher;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.HighlighterIterator;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeExtensionPoint;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.util.containers.Stack;

import java.util.HashMap;
import java.util.Map;

public class BraceMatchingUtil {
    public static final int UNDEFINED_TOKEN_GROUP = -1;

    private BraceMatchingUtil() {
    }

    public static boolean isPairedBracesAllowedBeforeTypeInFileType( IElementType lbraceType,
                                                                    final IElementType tokenType,
                                                                     FileType fileType) {
        try {
            return getBraceMatcher(fileType, lbraceType).isPairedBracesAllowedBeforeType(lbraceType, tokenType);
        }
        catch (AbstractMethodError incompatiblePluginThatWeDoNotCare) {
            // Do nothing
        }
        return true;
    }

    private static final Map<FileType, BraceMatcher> BRACE_MATCHERS = new HashMap<FileType, BraceMatcher>();

    public static void registerBraceMatcher( FileType fileType,  BraceMatcher braceMatcher) {
        BRACE_MATCHERS.put(fileType, braceMatcher);
    }

    
    public static int getMatchedBraceOffset( Editor editor, boolean forward,  PsiFile file) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        EditorHighlighter editorHighlighter = ((EditorEx)editor).getHighlighter();
        HighlighterIterator iterator = editorHighlighter.createIterator(offset);
        boolean matched = matchBrace(document.getCharsSequence(), file.getFileType(), iterator, forward);
        assert matched;
        return iterator.getStart();
    }

    private static class MatchBraceContext {
        private final CharSequence fileText;
        private final FileType fileType;
        private final HighlighterIterator iterator;
        private final boolean forward;

        private final IElementType brace1Token;
        private final int group;
        private final String brace1TagName;
        private final boolean isStrict;
        private final boolean isCaseSensitive;
        
        private final BraceMatcher myMatcher;

        private final Stack<IElementType> myBraceStack = new Stack<IElementType>();
        private final Stack<String> myTagNameStack = new Stack<String>();

        MatchBraceContext( CharSequence fileText,  FileType fileType,  HighlighterIterator iterator, boolean forward) {
            this(fileText, fileType, iterator, forward,isStrictTagMatching(getBraceMatcher(fileType, iterator), fileType, getTokenGroup(iterator.getTokenType(), fileType)));
        }

        MatchBraceContext( CharSequence fileText,  FileType fileType,  HighlighterIterator iterator, boolean forward, boolean strict) {
            this.fileText = fileText;
            this.fileType = fileType;
            this.iterator = iterator;
            this.forward = forward;

            myMatcher = getBraceMatcher(fileType, iterator);
            brace1Token = this.iterator.getTokenType();
            group = getTokenGroup(brace1Token, this.fileType);
            brace1TagName = getTagName(myMatcher, this.fileText, this.iterator);

            isCaseSensitive = areTagsCaseSensitive(myMatcher, this.fileType, group);
            isStrict = strict;
        }

        boolean doBraceMatch() {
            myBraceStack.clear();
            myTagNameStack.clear();
            myBraceStack.push(brace1Token);
            if (isStrict) {
                myTagNameStack.push(brace1TagName);
            }
            boolean matched = false;
            while (true) {
                if (!forward) {
                    iterator.retreat();
                }
                else {
                    iterator.advance();
                }
                if (iterator.atEnd()) {
                    break;
                }

                IElementType tokenType = iterator.getTokenType();

                if (getTokenGroup(tokenType, fileType) != group) {
                    continue;
                }
                String tagName = getTagName(myMatcher, fileText, iterator);
                if (!isStrict && !Comparing.equal(brace1TagName, tagName, isCaseSensitive)) continue;
                if (forward ? isLBraceToken(iterator, fileText, fileType) : isRBraceToken(iterator, fileText, fileType)) {
                    myBraceStack.push(tokenType);
                    if (isStrict) {
                        myTagNameStack.push(tagName);
                    }
                }
                else if (forward ? isRBraceToken(iterator, fileText, fileType) : isLBraceToken(iterator, fileText, fileType)) {
                    IElementType topTokenType = myBraceStack.pop();
                    String topTagName = null;
                    if (isStrict) {
                        topTagName = myTagNameStack.pop();
                    }

                    if (!isStrict) {
                        final IElementType baseType = myMatcher.getOppositeBraceTokenType(tokenType);
                        if (myBraceStack.contains(baseType)) {
                            while (!isPairBraces(topTokenType, tokenType, fileType) && !myBraceStack.empty()) {
                                topTokenType = myBraceStack.pop();
                            }
                        }
                        else if ((brace1TagName == null || !brace1TagName.equals(tagName)) && !isPairBraces(topTokenType, tokenType, fileType)) {
                            // Ignore non-matched opposite-direction brace for non-strict processing.
                            myBraceStack.push(topTokenType);
                            continue;
                        }
                    }

                    if (!isPairBraces(topTokenType, tokenType, fileType)
                            || isStrict && !Comparing.equal(topTagName, tagName, isCaseSensitive))
                    {
                        matched = false;
                        break;
                    }

                    if (myBraceStack.isEmpty()) {
                        matched = true;
                        break;
                    }
                }
            }
            return matched;
        }
    }

    public static synchronized boolean matchBrace( CharSequence fileText,
                                                   FileType fileType,
                                                   HighlighterIterator iterator,
                                                  boolean forward) {
        return new MatchBraceContext(fileText, fileType, iterator, forward).doBraceMatch();
    }


    public static synchronized boolean matchBrace( CharSequence fileText,
                                                   FileType fileType,
                                                   HighlighterIterator iterator,
                                                  boolean forward,
                                                  boolean isStrict) {
        return new MatchBraceContext(fileText, fileType, iterator, forward, isStrict).doBraceMatch();
    }

    public static boolean findStructuralLeftBrace( FileType fileType,  HighlighterIterator iterator,  CharSequence fileText) {
        final Stack<IElementType> braceStack = new Stack<IElementType>();
        final Stack<String> tagNameStack = new Stack<String>();

        BraceMatcher matcher = getBraceMatcher(fileType, iterator);

        while (!iterator.atEnd()) {
            if (isStructuralBraceToken(fileType, iterator, fileText)) {
                if (isRBraceToken(iterator, fileText, fileType)) {
                    braceStack.push(iterator.getTokenType());
                    tagNameStack.push(getTagName(matcher, fileText, iterator));
                }
                if (isLBraceToken(iterator, fileText, fileType)) {
                    if (braceStack.isEmpty()) return true;

                    final int group = matcher.getBraceTokenGroupId(iterator.getTokenType());

                    final IElementType topTokenType = braceStack.pop();
                    final IElementType tokenType = iterator.getTokenType();

                    boolean isStrict = isStrictTagMatching(matcher, fileType, group);
                    boolean isCaseSensitive = areTagsCaseSensitive(matcher, fileType, group);

                    String topTagName = null;
                    String tagName = null;
                    if (isStrict) {
                        topTagName = tagNameStack.pop();
                        tagName = getTagName(matcher, fileText, iterator);
                    }

                    if (!isPairBraces(topTokenType, tokenType, fileType)
                            || isStrict && !Comparing.equal(topTagName, tagName, isCaseSensitive)) {
                        return false;
                    }
                }
            }

            iterator.retreat();
        }

        return false;
    }

    public static boolean isStructuralBraceToken( FileType fileType,  HighlighterIterator iterator,  CharSequence text) {
        BraceMatcher matcher = getBraceMatcher(fileType, iterator);
        return matcher.isStructuralBrace(iterator, text, fileType);
    }

    public static boolean isLBraceToken( HighlighterIterator iterator,  CharSequence fileText,  FileType fileType) {
        final BraceMatcher braceMatcher = getBraceMatcher(fileType, iterator);

        return braceMatcher.isLBraceToken(iterator, fileText, fileType);
    }

    public static boolean isRBraceToken( HighlighterIterator iterator,  CharSequence fileText,  FileType fileType) {
        final BraceMatcher braceMatcher = getBraceMatcher(fileType, iterator);

        return braceMatcher.isRBraceToken(iterator, fileText, fileType);
    }

    public static boolean isPairBraces( IElementType tokenType1,  IElementType tokenType2,  FileType fileType) {
        BraceMatcher matcher = getBraceMatcher(fileType, tokenType1);
        return matcher.isPairBraces(tokenType1, tokenType2);
    }

    private static int getTokenGroup(IElementType tokenType, FileType fileType) {
        BraceMatcher matcher = getBraceMatcher(fileType, tokenType);
        return matcher.getBraceTokenGroupId(tokenType);
    }

    // TODO: better name for this method
    public static int findLeftmostLParen(HighlighterIterator iterator,
                                         IElementType lparenTokenType,
                                         CharSequence fileText,
                                         FileType fileType) {
        int lastLbraceOffset = -1;

        Stack<IElementType> braceStack = new Stack<IElementType>();
        for (; !iterator.atEnd(); iterator.retreat()) {
            final IElementType tokenType = iterator.getTokenType();

            if (isLBraceToken(iterator, fileText, fileType)) {
                if (!braceStack.isEmpty()) {
                    IElementType topToken = braceStack.pop();
                    if (!isPairBraces(tokenType, topToken, fileType)) {
                        break; // unmatched braces
                    }
                }
                else {
                    if (tokenType == lparenTokenType) {
                        lastLbraceOffset = iterator.getStart();
                    }
                    else {
                        break;
                    }
                }
            }
            else if (isRBraceToken(iterator, fileText, fileType)) {
                braceStack.push(iterator.getTokenType());
            }
        }

        return lastLbraceOffset;
    }

    public static int findLeftLParen(HighlighterIterator iterator,
                                     IElementType lparenTokenType,
                                     CharSequence fileText,
                                     FileType fileType) {
        int lastLbraceOffset = -1;

        Stack<IElementType> braceStack = new Stack<IElementType>();
        for (; !iterator.atEnd(); iterator.retreat()) {
            final IElementType tokenType = iterator.getTokenType();

            if (isLBraceToken(iterator, fileText, fileType)) {
                if (!braceStack.isEmpty()) {
                    IElementType topToken = braceStack.pop();
                    if (!isPairBraces(tokenType, topToken, fileType)) {
                        break; // unmatched braces
                    }
                }
                else {
                    if (tokenType == lparenTokenType) {
                        return iterator.getStart();
                    }
                    else {
                        break;
                    }
                }
            }
            else if (isRBraceToken(iterator, fileText, fileType)) {
                braceStack.push(iterator.getTokenType());
            }
        }

        return lastLbraceOffset;
    }

    // TODO: better name for this method
    public static int findRightmostRParen(HighlighterIterator iterator,
                                          IElementType rparenTokenType,
                                          CharSequence fileText,
                                          FileType fileType) {
        int lastRbraceOffset = -1;

        Stack<IElementType> braceStack = new Stack<IElementType>();
        for (; !iterator.atEnd(); iterator.advance()) {
            final IElementType tokenType = iterator.getTokenType();

            if (isRBraceToken(iterator, fileText, fileType)) {
                if (!braceStack.isEmpty()) {
                    IElementType topToken = braceStack.pop();
                    if (!isPairBraces(tokenType, topToken, fileType)) {
                        break; // unmatched braces
                    }
                }
                else {
                    if (tokenType == rparenTokenType) {
                        lastRbraceOffset = iterator.getStart();
                    }
                    else {
                        break;
                    }
                }
            }
            else if (isLBraceToken(iterator, fileText, fileType)) {
                braceStack.push(iterator.getTokenType());
            }
        }

        return lastRbraceOffset;
    }

    private static class BraceMatcherHolder {
        private static final BraceMatcher ourDefaultBraceMatcher = new DefaultBraceMatcher();
    }

    
    public static BraceMatcher getBraceMatcher( FileType fileType,  HighlighterIterator iterator) {
        return getBraceMatcher(fileType, iterator.getTokenType());
    }

    
    public static BraceMatcher getBraceMatcher( FileType fileType,  IElementType type) {
        return getBraceMatcher(fileType, type.getLanguage());
    }

    
    public static BraceMatcher getBraceMatcher( FileType fileType,  Language lang) {
        PairedBraceMatcher matcher = LanguageBraceMatching.INSTANCE.forLanguage(lang);
        if (matcher != null) {
            if (matcher instanceof XmlAwareBraceMatcher) {
                return (XmlAwareBraceMatcher)matcher;
            }
            else if (matcher instanceof PairedBraceMatcherAdapter) {
                return (BraceMatcher)matcher;
            }
            else {
                return new PairedBraceMatcherAdapter(matcher, lang);
            }
        }

        final BraceMatcher byFileType = getBraceMatcherByFileType(fileType);
        if (byFileType != null) return byFileType;

        if (fileType instanceof LanguageFileType) {
            final Language language = ((LanguageFileType)fileType).getLanguage();
            if (lang != language) {
                final FileType type1 = lang.getAssociatedFileType();
                if (type1 != null) {
                    final BraceMatcher braceMatcher = getBraceMatcherByFileType(type1);
                    if (braceMatcher != null) {
                        return braceMatcher;
                    }
                }

                matcher = LanguageBraceMatching.INSTANCE.forLanguage(language);
                if (matcher != null) {
                    return new PairedBraceMatcherAdapter(matcher, language);
                }
            }
        }

        return BraceMatcherHolder.ourDefaultBraceMatcher;
    }

    
    private static BraceMatcher getBraceMatcherByFileType( FileType fileType) {
        BraceMatcher braceMatcher = BRACE_MATCHERS.get(fileType);
        if (braceMatcher != null) return braceMatcher;

        for (FileTypeExtensionPoint<BraceMatcher> ext : Extensions.getExtensions(BraceMatcher.EP_NAME)) {
            if (fileType.getName().equals(ext.filetype)) {
                braceMatcher = ext.getInstance();
                BRACE_MATCHERS.put(fileType, braceMatcher);
                return braceMatcher;
            }
        }
        return null;
    }

    private static boolean isStrictTagMatching( BraceMatcher matcher,  FileType fileType, final int group) {
        return matcher instanceof XmlAwareBraceMatcher && ((XmlAwareBraceMatcher)matcher).isStrictTagMatching(fileType, group);
    }

    private static boolean areTagsCaseSensitive( BraceMatcher matcher,  FileType fileType, final int tokenGroup) {
        return matcher instanceof XmlAwareBraceMatcher && ((XmlAwareBraceMatcher)matcher).areTagsCaseSensitive(fileType, tokenGroup);
    }

    
    private static String getTagName( BraceMatcher matcher,  CharSequence fileText,  HighlighterIterator iterator) {
        if (matcher instanceof XmlAwareBraceMatcher) return ((XmlAwareBraceMatcher)matcher).getTagName(fileText, iterator);
        return null;
    }

    private static class DefaultBraceMatcher implements BraceMatcher {
        @Override
        public int getBraceTokenGroupId(final IElementType tokenType) {
            return UNDEFINED_TOKEN_GROUP;
        }

        @Override
        public boolean isLBraceToken(final HighlighterIterator iterator, final CharSequence fileText, final FileType fileType) {
            return false;
        }

        @Override
        public boolean isRBraceToken(final HighlighterIterator iterator, final CharSequence fileText, final FileType fileType) {
            return false;
        }

        @Override
        public boolean isPairBraces(final IElementType tokenType, final IElementType tokenType2) {
            return false;
        }

        @Override
        public boolean isStructuralBrace(final HighlighterIterator iterator, final CharSequence text, final FileType fileType) {
            return false;
        }

        @Override
        public IElementType getOppositeBraceTokenType( final IElementType type) {
            return null;
        }

        @Override
        public boolean isPairedBracesAllowedBeforeType( final IElementType lbraceType,  final IElementType contextType) {
            return true;
        }

        @Override
        public int getCodeConstructStart(final PsiFile file, final int openingBraceOffset) {
            return openingBraceOffset;
        }
    }
}
