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
package com.gome.maven.usages;

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.PlainSyntaxHighlighter;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighterFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.usageView.UsageTreeColors;
import com.gome.maven.usageView.UsageTreeColorsScheme;
import com.gome.maven.usages.impl.SyntaxHighlighterOverEditorHighlighter;
import com.gome.maven.usages.impl.rules.UsageType;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.FactoryMap;
import com.gome.maven.util.text.CharArrayUtil;
import com.gome.maven.util.text.StringFactory;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class ChunkExtractor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.usages.ChunkExtractor");
    public static final int MAX_LINE_LENGTH_TO_SHOW = 200;
    public static final int OFFSET_BEFORE_TO_SHOW_WHEN_LONG_LINE = 1;
    public static final int OFFSET_AFTER_TO_SHOW_WHEN_LONG_LINE = 1;

    private final EditorColorsScheme myColorsScheme;

    private final Document myDocument;
    private long myDocumentStamp;
    private final SyntaxHighlighterOverEditorHighlighter myHighlighter;

    private abstract static class WeakFactory<T> {
        private WeakReference<T> myRef;

        
        protected abstract T create();

        
        public T getValue() {
            final T cur = SoftReference.dereference(myRef);
            if (cur != null) return cur;
            final T result = create();
            myRef = new WeakReference<T>(result);
            return result;
        }
    }

    private static final ThreadLocal<WeakFactory<Map<PsiFile, ChunkExtractor>>> ourExtractors = new ThreadLocal<WeakFactory<Map<PsiFile, ChunkExtractor>>>() {
        @Override
        protected WeakFactory<Map<PsiFile, ChunkExtractor>> initialValue() {
            return new WeakFactory<Map<PsiFile, ChunkExtractor>>() {
                
                @Override
                protected Map<PsiFile, ChunkExtractor> create() {
                    return new FactoryMap<PsiFile, ChunkExtractor>() {
                        @Override
                        protected ChunkExtractor create(PsiFile psiFile) {
                            return new ChunkExtractor(psiFile);
                        }
                    };
                }
            };
        }
    };

    
    public static TextChunk[] extractChunks( PsiFile file,  UsageInfo2UsageAdapter usageAdapter) {
        return getExtractor(file).extractChunks(usageAdapter, file);
    }

    
    public static ChunkExtractor getExtractor( PsiFile file) {
        return ourExtractors.get().getValue().get(file);
    }

    private ChunkExtractor( PsiFile file) {
        myColorsScheme = UsageTreeColorsScheme.getInstance().getScheme();

        Project project = file.getProject();
        myDocument = PsiDocumentManager.getInstance(project).getDocument(file);
        LOG.assertTrue(myDocument != null);
        final FileType fileType = file.getFileType();
        SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, file.getVirtualFile());
        highlighter = highlighter == null ? new PlainSyntaxHighlighter() : highlighter;
        myHighlighter = new SyntaxHighlighterOverEditorHighlighter(highlighter, file.getVirtualFile(), project);
        myDocumentStamp = -1;
    }

    public static int getStartOffset(final List<RangeMarker> rangeMarkers) {
        LOG.assertTrue(!rangeMarkers.isEmpty());
        int minStart = Integer.MAX_VALUE;
        for (RangeMarker rangeMarker : rangeMarkers) {
            if (!rangeMarker.isValid()) continue;
            final int startOffset = rangeMarker.getStartOffset();
            if (startOffset < minStart) minStart = startOffset;
        }
        return minStart == Integer.MAX_VALUE ? -1 : minStart;
    }

    
    private TextChunk[] extractChunks( UsageInfo2UsageAdapter usageInfo2UsageAdapter,  PsiFile file) {
        int absoluteStartOffset = usageInfo2UsageAdapter.getNavigationOffset();
        if (absoluteStartOffset == -1) return TextChunk.EMPTY_ARRAY;

        Document visibleDocument = myDocument instanceof DocumentWindow ? ((DocumentWindow)myDocument).getDelegate() : myDocument;
        int visibleStartOffset = myDocument instanceof DocumentWindow ? ((DocumentWindow)myDocument).injectedToHost(absoluteStartOffset) : absoluteStartOffset;

        int lineNumber = myDocument.getLineNumber(absoluteStartOffset);
        int visibleLineNumber = visibleDocument.getLineNumber(visibleStartOffset);
        int visibleColumnNumber = visibleStartOffset - visibleDocument.getLineStartOffset(visibleLineNumber);
        final List<TextChunk> result = new ArrayList<TextChunk>();
        appendPrefix(result, visibleLineNumber, visibleColumnNumber);

        int fragmentToShowStart = myDocument.getLineStartOffset(lineNumber);
        int fragmentToShowEnd = fragmentToShowStart < myDocument.getTextLength() ? myDocument.getLineEndOffset(lineNumber) : 0;
        if (fragmentToShowStart > fragmentToShowEnd) return TextChunk.EMPTY_ARRAY;

        final CharSequence chars = myDocument.getCharsSequence();
        if (fragmentToShowEnd - fragmentToShowStart > MAX_LINE_LENGTH_TO_SHOW) {
            final int lineStartOffset = fragmentToShowStart;
            fragmentToShowStart = Math.max(lineStartOffset, absoluteStartOffset - OFFSET_BEFORE_TO_SHOW_WHEN_LONG_LINE);

            final int lineEndOffset = fragmentToShowEnd;
            Segment segment = usageInfo2UsageAdapter.getUsageInfo().getSegment();
            int usage_length = segment != null ? segment.getEndOffset() - segment.getStartOffset():0;
            fragmentToShowEnd = Math.min(lineEndOffset, absoluteStartOffset + usage_length + OFFSET_AFTER_TO_SHOW_WHEN_LONG_LINE);

            // if we search something like a word, then expand shown context from one symbol before / after at least for word boundary
            // this should not cause restarts of the lexer as the tokens are usually words
            if (usage_length > 0 &&
                    StringUtil.isJavaIdentifierStart(chars.charAt(absoluteStartOffset)) &&
                    StringUtil.isJavaIdentifierStart(chars.charAt(absoluteStartOffset + usage_length - 1))) {
                while(fragmentToShowEnd < lineEndOffset && StringUtil.isJavaIdentifierStart(chars.charAt(fragmentToShowEnd - 1))) ++fragmentToShowEnd;
                while(fragmentToShowStart > lineStartOffset && StringUtil.isJavaIdentifierStart(chars.charAt(fragmentToShowStart))) --fragmentToShowStart;
                if (fragmentToShowStart != lineStartOffset) ++fragmentToShowStart;
                if (fragmentToShowEnd != lineEndOffset) --fragmentToShowEnd;
            }
        }
        if (myDocument instanceof DocumentWindow) {
            List<TextRange> editable = InjectedLanguageManager.getInstance(file.getProject())
                    .intersectWithAllEditableFragments(file, new TextRange(fragmentToShowStart, fragmentToShowEnd));
            for (TextRange range : editable) {
                createTextChunks(usageInfo2UsageAdapter, chars, range.getStartOffset(), range.getEndOffset(), true, result);
            }
            return result.toArray(new TextChunk[result.size()]);
        }
        return createTextChunks(usageInfo2UsageAdapter, chars, fragmentToShowStart, fragmentToShowEnd, true, result);
    }

    
    public TextChunk[] createTextChunks( UsageInfo2UsageAdapter usageInfo2UsageAdapter,
                                         CharSequence chars,
                                        int start,
                                        int end,
                                        boolean selectUsageWithBold,
                                         List<TextChunk> result) {
        final Lexer lexer = myHighlighter.getHighlightingLexer();
        final SyntaxHighlighterOverEditorHighlighter highlighter = myHighlighter;

        LOG.assertTrue(start <= end);

        int i = StringUtil.indexOf(chars, '\n', start, end);
        if (i != -1) end = i;

        if (myDocumentStamp != myDocument.getModificationStamp()) {
            highlighter.restart(chars);
            myDocumentStamp = myDocument.getModificationStamp();
        } else if(lexer.getTokenType() == null || lexer.getTokenStart() > start) {
            highlighter.resetPosition(0);  // todo restart from nearest position with initial state
        }

        boolean isBeginning = true;

        for(;lexer.getTokenType() != null; lexer.advance()) {
            int hiStart = lexer.getTokenStart();
            int hiEnd = lexer.getTokenEnd();

            if (hiStart >= end) break;

            hiStart = Math.max(hiStart, start);
            hiEnd = Math.min(hiEnd, end);
            if (hiStart >= hiEnd) { continue; }

            if (isBeginning) {
                String text = chars.subSequence(hiStart, hiEnd).toString();
                if(text.trim().isEmpty()) continue;
            }
            isBeginning = false;
            IElementType tokenType = lexer.getTokenType();
            TextAttributesKey[] tokenHighlights = highlighter.getTokenHighlights(tokenType);

            processIntersectingRange(usageInfo2UsageAdapter, chars, hiStart, hiEnd, tokenHighlights, selectUsageWithBold, result);
        }

        return result.toArray(new TextChunk[result.size()]);
    }

    private void processIntersectingRange( UsageInfo2UsageAdapter usageInfo2UsageAdapter,
                                           final CharSequence chars,
                                          int hiStart,
                                          final int hiEnd,
                                           final TextAttributesKey[] tokenHighlights,
                                          final boolean selectUsageWithBold,
                                           final List<TextChunk> result) {
        final TextAttributes originalAttrs = convertAttributes(tokenHighlights);
        if (selectUsageWithBold) {
            originalAttrs.setFontType(Font.PLAIN);
        }

        final int[] lastOffset = {hiStart};
        usageInfo2UsageAdapter.processRangeMarkers(new Processor<Segment>() {
            @Override
            public boolean process(Segment segment) {
                int usageStart = segment.getStartOffset();
                int usageEnd = segment.getEndOffset();
                if (rangeIntersect(lastOffset[0], hiEnd, usageStart, usageEnd)) {
                    addChunk(chars, lastOffset[0], Math.max(lastOffset[0], usageStart), originalAttrs, false, null, result);

                    UsageType usageType = isHighlightedAsString(tokenHighlights)
                            ? UsageType.LITERAL_USAGE
                            : isHighlightedAsComment(tokenHighlights) ? UsageType.COMMENT_USAGE : null;
                    addChunk(chars, Math.max(lastOffset[0], usageStart), Math.min(hiEnd, usageEnd), originalAttrs, selectUsageWithBold, usageType, result);
                    lastOffset[0] = usageEnd;
                    if (usageEnd > hiEnd) {
                        return false;
                    }
                }
                return true;
            }
        });
        if (lastOffset[0] < hiEnd) {
            addChunk(chars, lastOffset[0], hiEnd, originalAttrs, false, null, result);
        }
    }

    public static boolean isHighlightedAsComment(TextAttributesKey... keys) {
        for (TextAttributesKey key : keys) {
            if (key == DefaultLanguageHighlighterColors.DOC_COMMENT ||
                    key == SyntaxHighlighterColors.DOC_COMMENT ||
                    key == DefaultLanguageHighlighterColors.LINE_COMMENT ||
                    key == SyntaxHighlighterColors.LINE_COMMENT ||
                    key == DefaultLanguageHighlighterColors.BLOCK_COMMENT ||
                    key == SyntaxHighlighterColors.JAVA_BLOCK_COMMENT
                    ) {
                return true;
            }
            if (key == null) continue;
            final TextAttributesKey fallbackAttributeKey = key.getFallbackAttributeKey();
            if (fallbackAttributeKey != null && isHighlightedAsComment(fallbackAttributeKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHighlightedAsString(TextAttributesKey... keys) {
        for (TextAttributesKey key : keys) {
            if (key == DefaultLanguageHighlighterColors.STRING || key == SyntaxHighlighterColors.STRING) {
                return true;
            }
            if (key == null) continue;
            final TextAttributesKey fallbackAttributeKey = key.getFallbackAttributeKey();
            if (fallbackAttributeKey != null && isHighlightedAsString(fallbackAttributeKey)) {
                return true;
            }
        }
        return false;
    }

    private static void addChunk( CharSequence chars,
                                 int start,
                                 int end,
                                  TextAttributes originalAttrs,
                                 boolean bold,
                                  UsageType usageType,
                                  List<TextChunk> result) {
        if (start >= end) return;

        TextAttributes attrs = bold
                ? TextAttributes.merge(originalAttrs, new TextAttributes(null, null, null, null, Font.BOLD))
                : originalAttrs;
        result.add(new TextChunk(attrs, StringFactory.createShared(CharArrayUtil.fromSequence(chars, start, end)), usageType));
    }

    private static boolean rangeIntersect(int s1, int e1, int s2, int e2) {
        return s2 < s1 && s1 < e2 || s2 < e1 && e1 < e2
                || s1 < s2 && s2 < e1 || s1 < e2 && e2 < e1
                || s1 == s2 && e1 == e2;
    }

    
    private TextAttributes convertAttributes( TextAttributesKey[] keys) {
        TextAttributes attrs = myColorsScheme.getAttributes(HighlighterColors.TEXT);

        for (TextAttributesKey key : keys) {
            TextAttributes attrs2 = myColorsScheme.getAttributes(key);
            if (attrs2 != null) {
                attrs = TextAttributes.merge(attrs, attrs2);
            }
        }

        attrs = attrs.clone();
        return attrs;
    }

    private void appendPrefix( List<TextChunk> result, int lineNumber, int columnNumber) {
        String prefix = "(" + (lineNumber + 1) + ": " + (columnNumber + 1) + ") ";
        TextChunk prefixChunk = new TextChunk(myColorsScheme.getAttributes(UsageTreeColors.USAGE_LOCATION), prefix);
        result.add(prefixChunk);
    }
}
