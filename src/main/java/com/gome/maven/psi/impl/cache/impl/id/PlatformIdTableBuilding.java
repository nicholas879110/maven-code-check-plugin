/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.cache.impl.id;

import com.gome.maven.ide.highlighter.HighlighterFactory;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageParserDefinitions;
import com.gome.maven.lang.ParserDefinition;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.ex.util.LexerEditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.HighlighterIterator;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.InternalFileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.CustomHighlighterTokenType;
import com.gome.maven.psi.impl.cache.CacheUtil;
import com.gome.maven.psi.impl.cache.impl.BaseFilterLexer;
import com.gome.maven.psi.impl.cache.impl.IndexPatternUtil;
import com.gome.maven.psi.impl.cache.impl.OccurrenceConsumer;
import com.gome.maven.psi.impl.cache.impl.todo.TodoIndexEntry;
import com.gome.maven.psi.impl.cache.impl.todo.TodoIndexers;
import com.gome.maven.psi.impl.cache.impl.todo.VersionedTodoIndexer;
import com.gome.maven.psi.search.IndexPattern;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.indexing.DataIndexer;
import com.gome.maven.util.indexing.FileContent;
import com.gome.maven.util.indexing.SubstitutedFileType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: dmitrylomov
 */
public abstract class PlatformIdTableBuilding {
    public static final Key<EditorHighlighter> EDITOR_HIGHLIGHTER = new Key<EditorHighlighter>("Editor");
    private static final Map<FileType, DataIndexer<TodoIndexEntry, Integer, FileContent>> ourTodoIndexers = new HashMap<FileType, DataIndexer<TodoIndexEntry, Integer, FileContent>>();
    private static final TokenSet ABSTRACT_FILE_COMMENT_TOKENS = TokenSet.create(CustomHighlighterTokenType.LINE_COMMENT, CustomHighlighterTokenType.MULTI_LINE_COMMENT);

    private PlatformIdTableBuilding() {}

    
    public static DataIndexer<TodoIndexEntry, Integer, FileContent> getTodoIndexer(FileType fileType, final VirtualFile virtualFile) {
        final DataIndexer<TodoIndexEntry, Integer, FileContent> indexer = ourTodoIndexers.get(fileType);

        if (indexer != null) {
            return indexer;
        }

        final DataIndexer<TodoIndexEntry, Integer, FileContent> extIndexer;
        if (fileType instanceof SubstitutedFileType && !((SubstitutedFileType)fileType).isSameFileType()) {
            SubstitutedFileType sft = (SubstitutedFileType)fileType;
            extIndexer =
                    new CompositeTodoIndexer(getTodoIndexer(sft.getOriginalFileType(), virtualFile), getTodoIndexer(sft.getFileType(), virtualFile));
        }
        else {
            extIndexer = TodoIndexers.INSTANCE.forFileType(fileType);
        }
        if (extIndexer != null) {
            return extIndexer;
        }

        if (fileType instanceof LanguageFileType) {
            final Language lang = ((LanguageFileType)fileType).getLanguage();
            final ParserDefinition parserDef = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
            final TokenSet commentTokens = parserDef != null ? parserDef.getCommentTokens() : null;
            if (commentTokens != null) {
                return new TokenSetTodoIndexer(commentTokens, virtualFile);
            }
        }

        if (fileType instanceof CustomSyntaxTableFileType) {
            return new TokenSetTodoIndexer(ABSTRACT_FILE_COMMENT_TOKENS, virtualFile);
        }

        return null;
    }

    public static boolean checkCanUseCachedEditorHighlighter(final CharSequence chars, final EditorHighlighter editorHighlighter) {
        assert editorHighlighter instanceof LexerEditorHighlighter;
        final boolean b = ((LexerEditorHighlighter)editorHighlighter).checkContentIsEqualTo(chars);
        if (!b) {
            final Logger logger = Logger.getInstance(IdTableBuilding.class.getName());
            logger.warn("Unexpected mismatch of editor highlighter content with indexing content");
        }
        return b;
    }

    @Deprecated
    public static void registerTodoIndexer( FileType fileType, DataIndexer<TodoIndexEntry, Integer, FileContent> indexer) {
        ourTodoIndexers.put(fileType, indexer);
    }

    public static boolean isTodoIndexerRegistered( FileType fileType) {
        return ourTodoIndexers.containsKey(fileType) || TodoIndexers.INSTANCE.forFileType(fileType) != null || fileType instanceof InternalFileType;
    }

    private static class CompositeTodoIndexer extends VersionedTodoIndexer {
        private final DataIndexer<TodoIndexEntry, Integer, FileContent>[] indexers;

        public CompositeTodoIndexer( DataIndexer<TodoIndexEntry, Integer, FileContent>... indexers) {
            this.indexers = indexers;
        }

        
        @Override
        public Map<TodoIndexEntry, Integer> map( FileContent inputData) {
            Map<TodoIndexEntry, Integer> result = ContainerUtil.newTroveMap();
            for (DataIndexer<TodoIndexEntry, Integer, FileContent> indexer : indexers) {
                for (Map.Entry<TodoIndexEntry, Integer> entry : indexer.map(inputData).entrySet()) {
                    TodoIndexEntry key = entry.getKey();
                    if (result.containsKey(key)) {
                        result.put(key, result.get(key) + entry.getValue());
                    } else {
                        result.put(key, entry.getValue());
                    }
                }
            }
            return result;
        }

        @Override
        public int getVersion() {
            int version = super.getVersion();
            for(DataIndexer dataIndexer:indexers) {
                version += dataIndexer instanceof VersionedTodoIndexer ? ((VersionedTodoIndexer)dataIndexer).getVersion() : 0xFF;
            }
            return version;
        }
    }

    private static class TokenSetTodoIndexer extends VersionedTodoIndexer {
         private final TokenSet myCommentTokens;
        private final VirtualFile myFile;

        public TokenSetTodoIndexer( final TokenSet commentTokens,  final VirtualFile file) {
            myCommentTokens = commentTokens;
            myFile = file;
        }

        @Override
        
        public Map<TodoIndexEntry, Integer> map( final FileContent inputData) {
            if (IndexPatternUtil.getIndexPatternCount() > 0) {
                final CharSequence chars = inputData.getContentAsText();
                final OccurrenceConsumer occurrenceConsumer = new OccurrenceConsumer(null, true);
                EditorHighlighter highlighter;

                final EditorHighlighter editorHighlighter = inputData.getUserData(EDITOR_HIGHLIGHTER);
                if (editorHighlighter != null && checkCanUseCachedEditorHighlighter(chars, editorHighlighter)) {
                    highlighter = editorHighlighter;
                }
                else {
                    highlighter = HighlighterFactory.createHighlighter(inputData.getProject(), myFile);
                    highlighter.setText(chars);
                }

                final int documentLength = chars.length();
                BaseFilterLexer.TodoScanningState todoScanningState = null;
                final HighlighterIterator iterator = highlighter.createIterator(0);

                while (!iterator.atEnd()) {
                    final IElementType token = iterator.getTokenType();

                    if (myCommentTokens.contains(token) || CacheUtil.isInComments(token)) {
                        int start = iterator.getStart();
                        if (start >= documentLength) break;
                        int end = iterator.getEnd();

                        todoScanningState = BaseFilterLexer.advanceTodoItemsCount(
                                chars.subSequence(start, Math.min(end, documentLength)),
                                occurrenceConsumer,
                                todoScanningState
                        );
                        if (end > documentLength) break;
                    }
                    iterator.advance();
                }
                final Map<TodoIndexEntry, Integer> map = new HashMap<TodoIndexEntry, Integer>();
                for (IndexPattern pattern : IndexPatternUtil.getIndexPatterns()) {
                    final int count = occurrenceConsumer.getOccurrenceCount(pattern);
                    if (count > 0) {
                        map.put(new TodoIndexEntry(pattern.getPatternString(), pattern.isCaseSensitive()), count);
                    }
                }
                return map;
            }
            return Collections.emptyMap();
        }
    }
}
