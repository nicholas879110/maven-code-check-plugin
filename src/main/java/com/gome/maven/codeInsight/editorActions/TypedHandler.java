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

package com.gome.maven.codeInsight.editorActions;

import com.gome.maven.codeInsight.AutoPopupController;
import com.gome.maven.codeInsight.CodeInsightSettings;
import com.gome.maven.codeInsight.CodeInsightUtilBase;
import com.gome.maven.codeInsight.completion.CompletionContributor;
import com.gome.maven.codeInsight.highlighting.BraceMatcher;
import com.gome.maven.codeInsight.highlighting.BraceMatchingUtil;
import com.gome.maven.codeInsight.highlighting.NontrivialBraceMatcher;
import com.gome.maven.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.lang.*;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.actionSystem.TypedActionHandler;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.HighlighterIterator;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.fileTypes.PlainTextLanguage;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.psi.util.PsiUtilBase;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.text.CharArrayUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypedHandler extends TypedActionHandlerBase {

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.editorActions.TypedHandler");

    private static final Map<FileType,QuoteHandler> quoteHandlers = new HashMap<FileType, QuoteHandler>();

    private static final Map<Class<? extends Language>, QuoteHandler> ourBaseLanguageQuoteHandlers = new HashMap<Class<? extends Language>, QuoteHandler>();

    public TypedHandler(TypedActionHandler originalHandler){
        super(originalHandler);
    }

    
    public static QuoteHandler getQuoteHandler( PsiFile file,  Editor editor) {
        FileType fileType = getFileType(file, editor);
        QuoteHandler quoteHandler = getQuoteHandlerForType(fileType);
        if (quoteHandler == null) {
            FileType fileFileType = file.getFileType();
            if (fileFileType != fileType) {
                quoteHandler = getQuoteHandlerForType(fileFileType);
            }
        }
        if (quoteHandler == null) {
            final Language baseLanguage = file.getViewProvider().getBaseLanguage();
            for (Map.Entry<Class<? extends Language>, QuoteHandler> entry : ourBaseLanguageQuoteHandlers.entrySet()) {
                if (entry.getKey().isInstance(baseLanguage)) {
                    return entry.getValue();
                }
            }
            return LanguageQuoteHandling.INSTANCE.forLanguage(baseLanguage);
        }
        return quoteHandler;
    }

    private static FileType getFileType( PsiFile file,  Editor editor) {
        FileType fileType = file.getFileType();
        Language language = PsiUtilBase.getLanguageInEditor(editor, file.getProject());
        if (language != null && language != PlainTextLanguage.INSTANCE) {
            LanguageFileType associatedFileType = language.getAssociatedFileType();
            if (associatedFileType != null) fileType = associatedFileType;
        }
        return fileType;
    }

    public static void registerBaseLanguageQuoteHandler( Class<? extends Language> languageClass,  QuoteHandler quoteHandler) {
        ourBaseLanguageQuoteHandlers.put(languageClass, quoteHandler);
    }

    public static QuoteHandler getQuoteHandlerForType( FileType fileType) {
        if (!quoteHandlers.containsKey(fileType)) {
            QuoteHandler handler = null;
            final QuoteHandlerEP[] handlerEPs = Extensions.getExtensions(QuoteHandlerEP.EP_NAME);
            for(QuoteHandlerEP ep: handlerEPs) {
                if (ep.fileType.equals(fileType.getName())) {
                    handler = ep.getHandler();
                    break;
                }
            }
            quoteHandlers.put(fileType, handler);
        }
        return quoteHandlers.get(fileType);
    }

    /** @see QuoteHandlerEP */
    @Deprecated
    public static void registerQuoteHandler( FileType fileType,  QuoteHandler quoteHandler) {
        quoteHandlers.put(fileType, quoteHandler);
    }

    @Override
    public void execute( final Editor originalEditor, final char charTyped,  final DataContext dataContext) {
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final PsiFile originalFile;

        if (project == null || (originalFile = PsiUtilBase.getPsiFileInEditor(originalEditor, project)) == null) {
            if (myOriginalHandler != null){
                myOriginalHandler.execute(originalEditor, charTyped, dataContext);
            }
            return;
        }

        if (!CodeInsightUtilBase.prepareEditorForWrite(originalEditor)) return;
        if (!FileDocumentManager.getInstance().requestWriting(originalEditor.getDocument(), project)) {
            return;
        }

        final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        final Document originalDocument = originalEditor.getDocument();
        originalEditor.getCaretModel().runForEachCaret(new CaretAction() {
            @Override
            public void perform(Caret caret) {
                if (psiDocumentManager.isDocumentBlockedByPsi(originalDocument)) {
                    psiDocumentManager.doPostponedOperationsAndUnblockDocument(originalDocument); // to clean up after previous caret processing
                }

                Editor editor = injectedEditorIfCharTypedIsSignificant(charTyped, originalEditor, originalFile);
                PsiFile file = editor == originalEditor ? originalFile : psiDocumentManager.getPsiFile(editor.getDocument());


                final TypedHandlerDelegate[] delegates = Extensions.getExtensions(TypedHandlerDelegate.EP_NAME);

                boolean handled = false;
                for (TypedHandlerDelegate delegate : delegates) {
                    final TypedHandlerDelegate.Result result = delegate.checkAutoPopup(charTyped, project, editor, file);
                    handled = result == TypedHandlerDelegate.Result.STOP;
                    if (result != TypedHandlerDelegate.Result.CONTINUE) {
                        break;
                    }
                }

                if (!handled) {
                    autoPopupCompletion(editor, charTyped, project, file);
                    autoPopupParameterInfo(editor, charTyped, project, file);
                }

                if (!editor.isInsertMode()) {
                    type(originalEditor, charTyped);
                    return;
                }

                EditorModificationUtil.deleteSelectedText(editor);

                FileType fileType = getFileType(file, editor);

                for (TypedHandlerDelegate delegate : delegates) {
                    final TypedHandlerDelegate.Result result = delegate.beforeCharTyped(charTyped, project, editor, file, fileType);
                    if (result == TypedHandlerDelegate.Result.STOP) {
                        return;
                    }
                    if (result == TypedHandlerDelegate.Result.DEFAULT) {
                        break;
                    }
                }

                if (')' == charTyped || ']' == charTyped || '}' == charTyped) {
                    if (FileTypes.PLAIN_TEXT != fileType) {
                        if (handleRParen(editor, fileType, charTyped)) return;
                    }
                }
                else if ('"' == charTyped || '\'' == charTyped || '`' == charTyped/* || '/' == charTyped*/) {
                    if (handleQuote(editor, charTyped, file)) return;
                }

                long modificationStampBeforeTyping = editor.getDocument().getModificationStamp();
                type(originalEditor, charTyped);
                AutoHardWrapHandler.getInstance().wrapLineIfNecessary(editor, dataContext, modificationStampBeforeTyping);

                if (('(' == charTyped || '[' == charTyped || '{' == charTyped) &&
                        CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET &&
                        fileType != FileTypes.PLAIN_TEXT) {
                    handleAfterLParen(editor, fileType, charTyped);
                }
                else if ('}' == charTyped) {
                    indentClosingBrace(project, editor);
                }
                else if (')' == charTyped) {
                    indentClosingParenth(project, editor);
                }

                for (TypedHandlerDelegate delegate : delegates) {
                    final TypedHandlerDelegate.Result result = delegate.charTyped(charTyped, project, editor, file);
                    if (result == TypedHandlerDelegate.Result.STOP) {
                        return;
                    }
                    if (result == TypedHandlerDelegate.Result.DEFAULT) {
                        break;
                    }
                }
                if ('{' == charTyped) {
                    indentOpenedBrace(project, editor);
                }
                else if ('(' == charTyped) {
                    indentOpenedParenth(project, editor);
                }
            }
        });
    }

    private static void type(Editor editor, char charTyped) {
        CommandProcessor.getInstance().setCurrentCommandName(EditorBundle.message("typing.in.editor.command.name"));
        EditorModificationUtil.typeInStringAtCaretHonorBlockSelection(editor, String.valueOf(charTyped), true);
    }

    private static void autoPopupParameterInfo( Editor editor, char charTyped,  Project project,  PsiFile file) {
        if ((charTyped == '(' || charTyped == ',') && !isInsideStringLiteral(editor, file)) {
            AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null);
        }
    }

    public static void autoPopupCompletion( Editor editor, char charTyped,  Project project,  PsiFile file) {
        if (charTyped == '.' || isAutoPopup(editor, file, charTyped)) {
            AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
        }
    }

    public static void commitDocumentIfCurrentCaretIsNotTheFirstOne( Editor editor,  Project project) {
        if (ContainerUtil.getFirstItem(editor.getCaretModel().getAllCarets()) != editor.getCaretModel().getCurrentCaret()) {
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        }
    }

    private static boolean isAutoPopup( Editor editor,  PsiFile file, char charTyped) {
        final int offset = editor.getCaretModel().getOffset() - 1;
        if (offset >= 0) {
            final PsiElement element = file.findElementAt(offset);
            if (element != null) {
                final List<CompletionContributor> list = CompletionContributor.forLanguage(element.getLanguage());
                for (CompletionContributor contributor : list) {
                    if (contributor.invokeAutoPopup(element, charTyped)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isInsideStringLiteral( Editor editor,  PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) return false;
        final ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(element.getLanguage());
        if (definition != null) {
            final TokenSet stringLiteralElements = definition.getStringLiteralElements();
            final ASTNode node = element.getNode();
            if (node == null) return false;
            final IElementType elementType = node.getElementType();
            if (stringLiteralElements.contains(elementType)) {
                return true;
            }
            PsiElement parent = element.getParent();
            if (parent != null) {
                ASTNode parentNode = parent.getNode();
                if (parentNode != null && stringLiteralElements.contains(parentNode.getElementType())) {
                    return true;
                }
            }
        }
        return false;
    }

    
    public static Editor injectedEditorIfCharTypedIsSignificant(final char charTyped,  Editor editor,  PsiFile oldFile) {
        int offset = editor.getCaretModel().getOffset();
        // even for uncommitted document try to retrieve injected fragment that has been there recently
        // we are assuming here that when user is (even furiously) typing, injected language would not change
        // and thus we can use its lexer to insert closing braces etc
        for (DocumentWindow documentWindow : InjectedLanguageUtil.getCachedInjectedDocuments(oldFile)) {
            if (documentWindow.isValid() && documentWindow.containsRange(offset, offset)) {
                PsiFile injectedFile = PsiDocumentManager.getInstance(oldFile.getProject()).getPsiFile(documentWindow);
                final Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injectedFile);
                // IDEA-52375 fix: last quote sign should be handled by outer language quote handler
                final CharSequence charsSequence = editor.getDocument().getCharsSequence();
                if (injectedEditor.getCaretModel().getOffset() == injectedEditor.getDocument().getTextLength() &&
                        offset < charsSequence.length() && charTyped == charsSequence.charAt(offset)) {
                    return editor;
                }
                return injectedEditor;
            }
        }

        return editor;
    }

    private static void handleAfterLParen( Editor editor,  FileType fileType, char lparenChar){
        int offset = editor.getCaretModel().getOffset();
        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
        boolean atEndOfDocument = offset == editor.getDocument().getTextLength();

        if (!atEndOfDocument) iterator.retreat();
        if (iterator.atEnd()) return;
        BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
        if (iterator.atEnd()) return;
        IElementType braceTokenType = iterator.getTokenType();
        final CharSequence fileText = editor.getDocument().getCharsSequence();
        if (!braceMatcher.isLBraceToken(iterator, fileText, fileType)) return;

        if (!iterator.atEnd()) {
            iterator.advance();

            if (!iterator.atEnd() &&
                    !BraceMatchingUtil.isPairedBracesAllowedBeforeTypeInFileType(braceTokenType, iterator.getTokenType(), fileType)) {
                return;
            }

            iterator.retreat();
        }

        int lparenOffset = BraceMatchingUtil.findLeftmostLParen(iterator, braceTokenType, fileText,fileType);
        if (lparenOffset < 0) lparenOffset = 0;

        iterator = ((EditorEx)editor).getHighlighter().createIterator(lparenOffset);
        boolean matched = BraceMatchingUtil.matchBrace(fileText, fileType, iterator, true, true);

        if (!matched) {
            String text;
            if (lparenChar == '(') {
                text = ")";
            }
            else if (lparenChar == '[') {
                text = "]";
            }
            else if (lparenChar == '<') {
                text = ">";
            }
            else if (lparenChar == '{') {
                text = "}";
            }
            else {
                throw new AssertionError("Unknown char "+lparenChar);
            }
            editor.getDocument().insertString(offset, text);
        }
    }

    public static boolean handleRParen( Editor editor,  FileType fileType, char charTyped) {
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return false;

        int offset = editor.getCaretModel().getOffset();

        if (offset == editor.getDocument().getTextLength()) return false;

        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()) return false;

        if (iterator.getEnd() - iterator.getStart() != 1 || editor.getDocument().getCharsSequence().charAt(iterator.getStart()) != charTyped) {
            return false;
        }

        BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
        CharSequence text = editor.getDocument().getCharsSequence();
        if (!braceMatcher.isRBraceToken(iterator, text, fileType)) {
            return false;
        }

        IElementType tokenType = iterator.getTokenType();

        iterator.retreat();

        IElementType lparenTokenType = braceMatcher.getOppositeBraceTokenType(tokenType);
        int lparenthOffset = BraceMatchingUtil.findLeftmostLParen(
                iterator,
                lparenTokenType,
                text,
                fileType
        );

        if (lparenthOffset < 0) {
            if (braceMatcher instanceof NontrivialBraceMatcher) {
                for(IElementType t:((NontrivialBraceMatcher)braceMatcher).getOppositeBraceTokenTypes(tokenType)) {
                    if (t == lparenTokenType) continue;
                    lparenthOffset = BraceMatchingUtil.findLeftmostLParen(
                            iterator,
                            t, text,
                            fileType
                    );
                    if (lparenthOffset >= 0) break;
                }
            }
            if (lparenthOffset < 0) return false;
        }

        iterator = ((EditorEx) editor).getHighlighter().createIterator(lparenthOffset);
        boolean matched = BraceMatchingUtil.matchBrace(text, fileType, iterator, true, true);

        if (!matched) return false;

        EditorModificationUtil.moveCaretRelatively(editor, 1);
        return true;
    }

    private static boolean handleQuote( Editor editor, char quote,  PsiFile file) {
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) return false;
        final QuoteHandler quoteHandler = getQuoteHandler(file, editor);
        if (quoteHandler == null) return false;

        int offset = editor.getCaretModel().getOffset();

        final Document document = editor.getDocument();
        CharSequence chars = document.getCharsSequence();
        int length = document.getTextLength();
        if (isTypingEscapeQuote(editor, quoteHandler, offset)) return false;

        if (offset < length && chars.charAt(offset) == quote){
            if (isClosingQuote(editor, quoteHandler, offset)){
                EditorModificationUtil.moveCaretRelatively(editor, 1);
                return true;
            }
        }

        HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);

        if (!iterator.atEnd()){
            IElementType tokenType = iterator.getTokenType();
            if (quoteHandler instanceof JavaLikeQuoteHandler) {
                try {
                    if (!((JavaLikeQuoteHandler)quoteHandler).isAppropriateElementTypeForLiteral(tokenType)) return false;
                }
                catch (AbstractMethodError incompatiblePluginErrorThatDoesNotInterestUs) {
                    // ignore
                }
            }
        }

        type(editor, quote);
        offset = editor.getCaretModel().getOffset();

        if (quoteHandler instanceof MultiCharQuoteHandler) {
            CharSequence closingQuote = getClosingQuote(editor, (MultiCharQuoteHandler)quoteHandler, offset);
            if (closingQuote != null && hasNonClosedLiterals(editor, quoteHandler, offset - 1)) {
                if (offset == document.getTextLength() ||
                        !Character.isUnicodeIdentifierPart(document.getCharsSequence().charAt(offset))) { //any better heuristic or an API?
                    document.insertString(offset, closingQuote);
                    return true;
                }
            }
        }

        if (isOpeningQuote(editor, quoteHandler, offset - 1) && hasNonClosedLiterals(editor, quoteHandler, offset - 1)) {
            if (offset == document.getTextLength() ||
                    !Character.isUnicodeIdentifierPart(document.getCharsSequence().charAt(offset))) { //any better heuristic or an API?
                document.insertString(offset, String.valueOf(quote));
            }
        }

        return true;
    }

    private static boolean isClosingQuote( Editor editor,  QuoteHandler quoteHandler, int offset) {
        HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()){
            LOG.assertTrue(false);
            return false;
        }

        return quoteHandler.isClosingQuote(iterator,offset);
    }

    
    private static CharSequence getClosingQuote( Editor editor,  MultiCharQuoteHandler quoteHandler, int offset) {
        HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()){
            LOG.assertTrue(false);
            return null;
        }

        return quoteHandler.getClosingQuote(iterator, offset);
    }

    private static boolean isOpeningQuote( Editor editor,  QuoteHandler quoteHandler, int offset) {
        HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()){
            LOG.assertTrue(false);
            return false;
        }

        return quoteHandler.isOpeningQuote(iterator, offset);
    }

    private static boolean hasNonClosedLiterals( Editor editor,  QuoteHandler quoteHandler, int offset) {
        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()) {
            LOG.assertTrue(false);
            return false;
        }

        return quoteHandler.hasNonClosedLiteral(editor, iterator, offset);
    }

    private static boolean isTypingEscapeQuote( Editor editor,  QuoteHandler quoteHandler, int offset){
        if (offset == 0) return false;
        CharSequence chars = editor.getDocument().getCharsSequence();
        int offset1 = CharArrayUtil.shiftBackward(chars, offset - 1, "\\");
        int slashCount = offset - 1 - offset1;
        return slashCount % 2 != 0 && isInsideLiteral(editor, quoteHandler, offset);
    }

    private static boolean isInsideLiteral( Editor editor,  QuoteHandler quoteHandler, int offset){
        if (offset == 0) return false;

        HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset - 1);
        if (iterator.atEnd()){
            LOG.assertTrue(false);
            return false;
        }

        return quoteHandler.isInsideLiteral(iterator);
    }

    private static void indentClosingBrace( Project project,  Editor editor){
        indentBrace(project, editor, '}');
    }

    static void indentOpenedBrace( Project project,  Editor editor){
        indentBrace(project, editor, '{');
    }

    private static void indentOpenedParenth( Project project,  Editor editor){
        indentBrace(project, editor, '(');
    }

    private static void indentClosingParenth( Project project,  Editor editor){
        indentBrace(project, editor, ')');
    }

    private static void indentBrace( final Project project,  final Editor editor, final char braceChar) {
        final int offset = editor.getCaretModel().getOffset() - 1;
        final Document document = editor.getDocument();
        CharSequence chars = document.getCharsSequence();
        if (offset < 0 || chars.charAt(offset) != braceChar) return;

        int spaceStart = CharArrayUtil.shiftBackward(chars, offset - 1, " \t");
        if (spaceStart < 0 || chars.charAt(spaceStart) == '\n' || chars.charAt(spaceStart) == '\r'){
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.commitDocument(document);

            final PsiFile file = documentManager.getPsiFile(document);
            if (file == null || !file.isWritable()) return;
            PsiElement element = file.findElementAt(offset);
            if (element == null) return;

            EditorHighlighter highlighter = ((EditorEx)editor).getHighlighter();
            HighlighterIterator iterator = highlighter.createIterator(offset);

            final FileType fileType = file.getFileType();
            BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
            boolean rBraceToken = braceMatcher.isRBraceToken(iterator, chars, fileType);
            final boolean isBrace = braceMatcher.isLBraceToken(iterator, chars, fileType) || rBraceToken;
            int lBraceOffset = -1;

            if (CodeInsightSettings.getInstance().REFORMAT_BLOCK_ON_RBRACE &&
                    rBraceToken &&
                    braceMatcher.isStructuralBrace(iterator, chars, fileType) && offset > 0) {
                lBraceOffset = BraceMatchingUtil.findLeftLParen(
                        highlighter.createIterator(offset - 1),
                        braceMatcher.getOppositeBraceTokenType(iterator.getTokenType()),
                        editor.getDocument().getCharsSequence(),
                        fileType
                );
            }
            if (element.getNode() != null && isBrace) {
                final int finalLBraceOffset = lBraceOffset;
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run(){
                        try{
                            int newOffset;
                            if (finalLBraceOffset != -1) {
                                RangeMarker marker = document.createRangeMarker(offset, offset + 1);
                                CodeStyleManager.getInstance(project).reformatRange(file, finalLBraceOffset, offset, true);
                                newOffset = marker.getStartOffset();
                                marker.dispose();
                            } else {
                                newOffset = CodeStyleManager.getInstance(project).adjustLineIndent(file, offset);
                            }

                            editor.getCaretModel().moveToOffset(newOffset + 1);
                            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                            editor.getSelectionModel().removeSelection();
                        }
                        catch(IncorrectOperationException e){
                            LOG.error(e);
                        }
                    }
                });
            }
        }
    }

}

