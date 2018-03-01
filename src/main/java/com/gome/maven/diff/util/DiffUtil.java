/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff.util;

import com.gome.maven.codeStyle.CodeStyleFacade;
import com.gome.maven.diff.DiffContext;
import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.DiffTool;
import com.gome.maven.diff.SuppressiveDiffTool;
import com.gome.maven.diff.comparison.ComparisonManager;
import com.gome.maven.diff.comparison.ComparisonPolicy;
import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.contents.DocumentContent;
import com.gome.maven.diff.contents.EmptyContent;
import com.gome.maven.diff.fragments.DiffFragment;
import com.gome.maven.diff.fragments.LineFragment;
import com.gome.maven.diff.requests.ContentDiffRequest;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.tools.util.LineFragmentCache;
import com.gome.maven.diff.tools.util.LineFragmentCache.PolicyData;
import com.gome.maven.diff.tools.util.base.HighlightPolicy;
import com.gome.maven.diff.tools.util.base.IgnorePolicy;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.impl.LaterInvocator;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.DiffBundle;
import com.gome.maven.openapi.diff.impl.external.DiffManagerImpl;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.ex.EditorMarkupModel;
import com.gome.maven.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighterFactory;
import com.gome.maven.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapperDialog;
import com.gome.maven.openapi.ui.WindowWrapper;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.ReadonlyStatusHandler;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.IdeFocusManager;
import com.gome.maven.openapi.wm.impl.IdeFrameImpl;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.ui.IdeBorderFactory;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.ScreenUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.LineSeparator;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class DiffUtil {
    private static final Logger LOG = Logger.getInstance(DiffUtil.class);

     public static final String DIFF_CONFIG = StoragePathMacros.APP_CONFIG + "/diff.xml";

    //
    // Editor
    //

    
    public static EditorHighlighter initEditorHighlighter( Project project,
                                                           DocumentContent content,
                                                           CharSequence text) {
        EditorHighlighter highlighter = createEditorHighlighter(project, content);
        if (highlighter == null) return null;
        highlighter.setText(text);
        return highlighter;
    }

    
    public static EditorHighlighter initEmptyEditorHighlighter( Project project,  CharSequence text) {
        EditorHighlighter highlighter = createEmptyEditorHighlighter();
        highlighter.setText(text);
        return highlighter;
    }

    
    public static EditorHighlighter createEditorHighlighter( Project project,  DocumentContent content) {
        FileType type = content.getContentType();
        VirtualFile file = content.getHighlightFile();

        if ((file != null && file.getFileType() == type) || file instanceof LightVirtualFile) {
            return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file);
        }
        if (type != null) {
            return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, type);
        }

        return null;
    }

    
    public static EditorHighlighter createEmptyEditorHighlighter() {
        return new EmptyEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT));
    }

    public static void setEditorHighlighter( Project project,  EditorEx editor,  DocumentContent content) {
        EditorHighlighter highlighter = createEditorHighlighter(project, content);
        if (highlighter != null) editor.setHighlighter(highlighter);
    }

    public static void setEditorCodeStyle( Project project,  EditorEx editor,  FileType fileType) {
        if (project != null && fileType != null) {
            CodeStyleFacade codeStyleFacade = CodeStyleFacade.getInstance(project);
            editor.getSettings().setTabSize(codeStyleFacade.getTabSize(fileType));
            editor.getSettings().setUseTabCharacter(codeStyleFacade.useTabCharacter(fileType));
        }
        editor.getColorsScheme().setAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES, null);
        editor.getSettings().setCaretRowShown(false);
        editor.reinitSettings();
    }

    
    public static EditorEx createEditor( Document document,  Project project, boolean isViewer) {
        return createEditor(document, project, isViewer, false);
    }

    
    public static EditorEx createEditor( Document document,  Project project, boolean isViewer, boolean enableFolding) {
        EditorFactory factory = EditorFactory.getInstance();
        EditorEx editor = (EditorEx)(isViewer ? factory.createViewer(document, project) : factory.createEditor(document, project));

        editor.putUserData(DiffManagerImpl.EDITOR_IS_DIFF_KEY, Boolean.TRUE);
        editor.setSoftWrapAppliancePlace(SoftWrapAppliancePlaces.VCS_DIFF);

        editor.getSettings().setLineNumbersShown(true);
        ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeVisible(true);
        editor.getGutterComponentEx().setShowDefaultGutterPopup(false);

        if (enableFolding) {
            editor.getSettings().setFoldingOutlineShown(true);
            editor.getSettings().setAutoCodeFoldingEnabled(false);
        }
        else {
            editor.getSettings().setFoldingOutlineShown(false);
            editor.getFoldingModel().setFoldingEnabled(false);
        }

        UIUtil.removeScrollBorder(editor.getComponent());

        return editor;
    }

    public static void configureEditor( EditorEx editor,  DocumentContent content,  Project project) {
        setEditorHighlighter(project, editor, content);
        setEditorCodeStyle(project, editor, content.getContentType());
        editor.reinitSettings();
    }

    //
    // Scrolling
    //

    public static void scrollEditor( final Editor editor, int line) {
        scrollEditor(editor, line, 0);
    }

    public static void scrollEditor( final Editor editor, int line, int column) {
        scrollEditor(editor, new LogicalPosition(line, column));
    }

    public static void scrollEditor( final Editor editor,  LogicalPosition position) {
        if (editor == null) return;
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToLogicalPosition(position);
        ScrollingModel scrollingModel = editor.getScrollingModel();
        scrollingModel.disableAnimation();
        scrollingModel.scrollToCaret(ScrollType.CENTER);
        scrollingModel.enableAnimation();
    }

    public static void scrollToLineAnimated( final Editor editor, int line) {
        if (editor == null) return;
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, 0));
        ScrollingModel scrollingModel = editor.getScrollingModel();
        scrollingModel.scrollToCaret(ScrollType.CENTER);
    }

    public static void scrollToPoint( Editor editor,  Point point) {
        if (editor == null) return;
        editor.getScrollingModel().disableAnimation();
        editor.getScrollingModel().scrollHorizontally(point.x);
        editor.getScrollingModel().scrollVertically(point.y);
        editor.getScrollingModel().enableAnimation();
    }

    
    public static Point getScrollingPosition( Editor editor) {
        if (editor == null) return new Point(0, 0);
        ScrollingModel model = editor.getScrollingModel();
        return new Point(model.getHorizontalScrollOffset(), model.getVerticalScrollOffset());
    }

    
    public static LogicalPosition getCaretPosition( Editor editor) {
        return editor != null ? editor.getCaretModel().getLogicalPosition() : new LogicalPosition(0, 0);
    }

    
    public static Point[] getScrollingPositions( List<? extends Editor> editors) {
        Point[] carets = new Point[editors.size()];
        for (int i = 0; i < editors.size(); i++) {
            carets[i] = getScrollingPosition(editors.get(i));
        }
        return carets;
    }

    
    public static LogicalPosition[] getCaretPositions( List<? extends Editor> editors) {
        LogicalPosition[] carets = new LogicalPosition[editors.size()];
        for (int i = 0; i < editors.size(); i++) {
            carets[i] = getCaretPosition(editors.get(i));
        }
        return carets;
    }

    public static class EditorsVisiblePositions {
        public static final Key<EditorsVisiblePositions> KEY = Key.create("Diff.EditorsVisiblePositions");

         public final LogicalPosition[] myCaretPosition;
         public final Point[] myPoints;

        public EditorsVisiblePositions( LogicalPosition caretPosition,  Point points) {
            myCaretPosition = new LogicalPosition[]{caretPosition};
            myPoints = new Point[]{points};
        }

        public EditorsVisiblePositions( LogicalPosition[] caretPosition,  Point[] points) {
            myCaretPosition = caretPosition;
            myPoints = points;
        }

        public boolean isSame( LogicalPosition... caretPosition) {
            // TODO: allow small fluctuations ?
            if (caretPosition == null) return true;
            if (myCaretPosition.length != caretPosition.length) return false;
            for (int i = 0; i < caretPosition.length; i++) {
                if (!caretPosition[i].equals(myCaretPosition[i])) return false;
            }
            return true;
        }
    }

    //
    // UI
    //

    
    public static JPanel createMessagePanel( String message) {
        Pair<JPanel, JLabel> pair = createMessagePanel();
        pair.getSecond().setText(message);
        return pair.getFirst();
    }

    
    public static Pair<JPanel, JLabel> createMessagePanel() {
        final JLabel label = new JLabel();
        label.setForeground(UIUtil.getInactiveTextColor());
        final JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.add(label,
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        return Pair.create(wrapper, label);
    }

    public static void addActionBlock( DefaultActionGroup group, AnAction... actions) {
        if (actions.length == 0) return;
        if (group.getChildrenCount() != 0) group.addSeparator();

        for (AnAction action : actions) {
            if (action != null) group.add(action);
        }
    }

    public static void addActionBlock( DefaultActionGroup group,  List<? extends AnAction> actions) {
        if (actions == null || actions.isEmpty()) return;
        if (group.getChildrenCount() != 0) group.addSeparator();
        group.addAll(actions);
    }

    // Titles

    
    public static List<JComponent> createSimpleTitles( ContentDiffRequest request) {
        List<String> titles = request.getContentTitles();

        List<JComponent> components = new ArrayList<JComponent>(titles.size());
        for (String title : titles) {
            components.add(createTitle(title));
        }

        return components;
    }

    
    public static List<JComponent> createTextTitles( ContentDiffRequest request,  List<? extends Editor> editors) {
        List<DiffContent> contents = request.getContents();
        List<String> titles = request.getContentTitles();

        List<Charset> charsets = ContainerUtil.map(contents, new Function<DiffContent, Charset>() {
            @Override
            public Charset fun(DiffContent content) {
                if (content instanceof EmptyContent) return null;
                return ((DocumentContent)content).getCharset();
            }
        });
        List<LineSeparator> separators = ContainerUtil.map(contents, new Function<DiffContent, LineSeparator>() {
            @Override
            public LineSeparator fun(DiffContent content) {
                if (content instanceof EmptyContent) return null;
                return ((DocumentContent)content).getLineSeparator();
            }
        });

        boolean equalCharsets = isEqualElements(charsets);
        boolean equalSeparators = isEqualElements(separators);

        List<JComponent> result = new ArrayList<JComponent>(contents.size());

        if (equalCharsets && equalSeparators && ContainerUtil.find(titles, Condition.NOT_NULL) == null) {
            return Collections.nCopies(titles.size(), null);
        }

        for (int i = 0; i < contents.size(); i++) {
            result.add(createTitle(StringUtil.notNullize(titles.get(i)), contents.get(i), equalCharsets, equalSeparators, editors.get(i)));
        }

        return result;
    }

    private static boolean isEqualElements( List elements) {
        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                if (!isEqualElements(elements.get(i), elements.get(j))) return false;
            }
        }
        return true;
    }

    private static boolean isEqualElements( Object element1,  Object element2) {
        if (element1 == null || element2 == null) return true;
        return element1.equals(element2);
    }

    
    private static JComponent createTitle( String title,
                                           DiffContent content,
                                          boolean equalCharsets,
                                          boolean equalSeparators,
                                           Editor editor) {
        if (content instanceof EmptyContent) return null;

        Charset charset = equalCharsets ? null : ((DocumentContent)content).getCharset();
        LineSeparator separator = equalSeparators ? null : ((DocumentContent)content).getLineSeparator();
        boolean isReadOnly = editor == null || editor.isViewer() || !canMakeWritable(editor.getDocument());

        return createTitle(title, charset, separator, isReadOnly);
    }

    
    public static JComponent createTitle( String title) {
        return createTitle(title, null, null, true);
    }

    
    public static JComponent createTitle( String title,
                                          Charset charset,
                                          LineSeparator separator,
                                         boolean readOnly) {
        if (readOnly) title += " " + DiffBundle.message("diff.content.read.only.content.title.suffix");

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(IdeBorderFactory.createEmptyBorder(0, 4, 0, 4));
        panel.add(createTitlePanel(title), BorderLayout.CENTER);
        if (charset != null && separator != null) {
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
            panel2.add(createCharsetPanel(charset));
            panel2.add(Box.createRigidArea(new Dimension(4, 0)));
            panel2.add(createSeparatorPanel(separator));
            panel.add(panel2, BorderLayout.EAST);
        }
        else if (charset != null) {
            panel.add(createCharsetPanel(charset), BorderLayout.EAST);
        }
        else if (separator != null) {
            panel.add(createSeparatorPanel(separator), BorderLayout.EAST);
        }
        return panel;
    }

    
    private static JComponent createTitlePanel( String title) {
        if (title.isEmpty()) title = " "; // do not collapse
        return new JLabel(title); // TODO: allow to copy text
    }

    
    private static JComponent createCharsetPanel( Charset charset) {
        JLabel label = new JLabel(charset.displayName());
        // TODO: specific colors for other charsets
        if (charset.equals(Charset.forName("UTF-8"))) {
            label.setForeground(JBColor.BLUE);
        }
        else if (charset.equals(Charset.forName("ISO-8859-1"))) {
            label.setForeground(JBColor.RED);
        }
        else {
            label.setForeground(JBColor.BLACK);
        }
        return label;
    }

    
    private static JComponent createSeparatorPanel( LineSeparator separator) {
        JLabel label = new JLabel(separator.name());
        Color color;
        if (separator == LineSeparator.CRLF) {
            color = JBColor.RED;
        }
        else if (separator == LineSeparator.LF) {
            color = JBColor.BLUE;
        }
        else if (separator == LineSeparator.CR) {
            color = JBColor.MAGENTA;
        }
        else {
            color = JBColor.BLACK;
        }
        label.setForeground(color);
        return label;
    }

    //
    // Focus
    //

    public static boolean isFocusedComponent( Project project,  Component component) {
        if (component == null) return false;
        return IdeFocusManager.getInstance(project).getFocusedDescendantFor(component) != null;
    }

    public static void requestFocus( Project project,  Component component) {
        if (component == null) return;
        IdeFocusManager.getInstance(project).requestFocus(component, true);
    }

    //
    // Compare
    //

    
    public static List<LineFragment> compareWithCache( DiffRequest request,
                                                       DocumentData data,
                                                       DiffConfig config,
                                                       ProgressIndicator indicator) {
        return compareWithCache(request, data.getText1(), data.getText2(), data.getStamp1(), data.getStamp2(), config, indicator);
    }

    
    public static List<LineFragment> compareWithCache( DiffRequest request,
                                                       CharSequence text1,
                                                       CharSequence text2,
                                                      long stamp1,
                                                      long stamp2,
                                                       DiffConfig config,
                                                       ProgressIndicator indicator) {
        List<LineFragment> fragments = doCompareWithCache(request, text1, text2, stamp1, stamp2, config, indicator);

        indicator.checkCanceled();
        return ComparisonManager.getInstance().processBlocks(fragments, text1, text2,
                config.policy, config.squashFragments, config.trimFragments);
    }

    
    private static List<LineFragment> doCompareWithCache( DiffRequest request,
                                                          CharSequence text1,
                                                          CharSequence text2,
                                                         long stamp1,
                                                         long stamp2,
                                                          DiffConfig config,
                                                          ProgressIndicator indicator) {
        indicator.checkCanceled();
        PolicyData cachedData = getFromCache(request, config, stamp1, stamp2);

        List<LineFragment> newFragments;
        if (cachedData != null) {
            if (cachedData.getFragments().isEmpty()) return cachedData.getFragments();
            if (!config.innerFragments) return cachedData.getFragments();
            if (cachedData.isInnerFragments()) return cachedData.getFragments();
            newFragments = ComparisonManager.getInstance().compareLinesInner(text1, text2, cachedData.getFragments(), config.policy, indicator);
        }
        else {
            if (config.innerFragments) {
                newFragments = ComparisonManager.getInstance().compareLinesInner(text1, text2, config.policy, indicator);
            }
            else {
                newFragments = ComparisonManager.getInstance().compareLines(text1, text2, config.policy, indicator);
            }
        }

        indicator.checkCanceled();
        putToCache(request, config, stamp1, stamp2, newFragments, config.innerFragments);
        return newFragments;
    }

    
    public static PolicyData getFromCache( DiffRequest request,  DiffConfig config, long stamp1, long stamp2) {
        LineFragmentCache cache = request.getUserData(DiffUserDataKeysEx.LINE_FRAGMENT_CACHE);
        if (cache != null && cache.checkStamps(stamp1, stamp2)) {
            return cache.getData(config.policy);
        }
        return null;
    }

    public static void putToCache( DiffRequest request,  DiffConfig config, long stamp1, long stamp2,
                                   List<LineFragment> fragments, boolean isInnerFragments) {
        // We can't rely on monotonicity on modificationStamps, so we can't check if we actually compared freshest versions of documents
        // Possible data races also could make cache outdated.
        // But these cases shouldn't be often and won't break anything.

        LineFragmentCache oldCache = request.getUserData(DiffUserDataKeysEx.LINE_FRAGMENT_CACHE);
        LineFragmentCache cache;
        if (oldCache == null || !oldCache.checkStamps(stamp1, stamp2)) {
            cache = new LineFragmentCache(stamp1, stamp2);
        }
        else {
            cache = new LineFragmentCache(oldCache);
        }

        cache.putData(config.policy, fragments, isInnerFragments);
        request.putUserData(DiffUserDataKeysEx.LINE_FRAGMENT_CACHE, cache);
    }

    //
    // Document modification
    //

    
    public static BitSet getSelectedLines( Editor editor) {
        Document document = editor.getDocument();
        int totalLines = getLineCount(document);
        BitSet lines = new BitSet(totalLines + 1);

        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            if (caret.hasSelection()) {
                int line1 = editor.offsetToLogicalPosition(caret.getSelectionStart()).line;
                int line2 = editor.offsetToLogicalPosition(caret.getSelectionEnd()).line;
                lines.set(line1, line2 + 1);
                if (caret.getSelectionEnd() == document.getTextLength()) lines.set(totalLines);
            }
            else {
                lines.set(caret.getLogicalPosition().line);
                if (caret.getOffset() == document.getTextLength()) lines.set(totalLines);
            }
        }

        return lines;
    }

    public static boolean isSelectedByLine(int line, int line1, int line2) {
        if (line1 == line2 && line == line1) {
            return true;
        }
        if (line >= line1 && line < line2) {
            return true;
        }
        return false;
    }

    public static boolean isSelectedByLine( BitSet selected, int line1, int line2) {
        if (line1 == line2) {
            return selected.get(line1);
        }
        else {
            int next = selected.nextSetBit(line1);
            return next != -1 && next < line2;
        }
    }

    public static void deleteLines( Document document, int line1, int line2) {
        TextRange range = getLinesRange(document, line1, line2);
        int offset1 = range.getStartOffset();
        int offset2 = range.getEndOffset();

        if (offset1 > 0) {
            offset1--;
        }
        else if (offset2 < document.getTextLength()) {
            offset2++;
        }
        document.deleteString(offset1, offset2);
    }

    public static void insertLines( Document document, int line,  CharSequence text) {
        if (line == getLineCount(document)) {
            document.insertString(document.getTextLength(), "\n" + text);
        }
        else {
            document.insertString(document.getLineStartOffset(line), text + "\n");
        }
    }

    public static void replaceLines( Document document, int line1, int line2,  CharSequence text) {
        TextRange currentTextRange = getLinesRange(document, line1, line2);
        int offset1 = currentTextRange.getStartOffset();
        int offset2 = currentTextRange.getEndOffset();

        document.replaceString(offset1, offset2, text);
    }

    public static void insertLines( Document document1, int line,  Document document2, int otherLine1, int otherLine2) {
        insertLines(document1, line, getLinesContent(document2, otherLine1, otherLine2));
    }

    public static void replaceLines( Document document1, int line1, int line2,  Document document2, int oLine1, int oLine2) {
        replaceLines(document1, line1, line2, getLinesContent(document2, oLine1, oLine2));
    }

    public static void applyModification( Document document1,
                                         int line1,
                                         int line2,
                                          Document document2,
                                         int oLine1,
                                         int oLine2) {
        if (line1 == line2 && oLine1 == oLine2) return;
        if (line1 == line2) {
            insertLines(document1, line1, document2, oLine1, oLine2);
        }
        else if (oLine1 == oLine2) {
            deleteLines(document1, line1, line2);
        }
        else {
            replaceLines(document1, line1, line2, document2, oLine1, oLine2);
        }
    }

    
    public static CharSequence getLinesContent( Document document, int line1, int line2) {
        TextRange otherRange = getLinesRange(document, line1, line2);
        return document.getCharsSequence().subSequence(otherRange.getStartOffset(), otherRange.getEndOffset());
    }

    
    public static TextRange getLinesRange( Document document, int line1, int line2) {
        if (line1 == line2) {
            int lineStartOffset = line1 < getLineCount(document) ? document.getLineStartOffset(line1) : document.getTextLength();
            return new TextRange(lineStartOffset, lineStartOffset);
        }
        else {
            int startOffset = document.getLineStartOffset(line1);
            int endOffset = document.getLineEndOffset(line2 - 1);
            return new TextRange(startOffset, endOffset);
        }
    }

    public static int getLineCount( Document document) {
        return Math.max(document.getLineCount(), 1);
    }

    //
    // Types
    //

    
    public static TextDiffType getLineDiffType( LineFragment fragment) {
        boolean left = fragment.getEndOffset1() != fragment.getStartOffset1() || fragment.getStartLine1() != fragment.getEndLine1();
        boolean right = fragment.getEndOffset2() != fragment.getStartOffset2() || fragment.getStartLine2() != fragment.getEndLine2();
        return getType(left, right);
    }

    
    public static TextDiffType getDiffType( DiffFragment fragment) {
        boolean left = fragment.getEndOffset1() != fragment.getStartOffset1();
        boolean right = fragment.getEndOffset2() != fragment.getStartOffset2();
        return getType(left, right);
    }

    private static TextDiffType getType(boolean left, boolean right) {
        if (left && right) {
            return TextDiffType.MODIFIED;
        }
        else if (left) {
            return TextDiffType.DELETED;
        }
        else if (right) {
            return TextDiffType.INSERTED;
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    //
    // Writable
    //

//    @CalledInAwt
    public static void executeWriteCommand( final Document document,
                                            final Project project,
                                            final String name,
                                            final Runnable task) {
        if (!makeWritable(project, document)) return;

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                CommandProcessor.getInstance().executeCommand(project, task, name, null);
            }
        });
    }

    public static boolean isEditable( Editor editor) {
        return !editor.isViewer() && canMakeWritable(editor.getDocument());
    }

    public static boolean canMakeWritable( Document document) {
        if (document.isWritable()) {
            return true;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null && file.isInLocalFileSystem()) {
            return true;
        }
        return false;
    }

//    @CalledInAwt
    public static boolean makeWritable( Project project,  Document document) {
        if (document.isWritable()) return true;
        if (project == null) return false;
        return ReadonlyStatusHandler.ensureDocumentWritable(project, document);
    }

    //
    // Windows
    //

    
    public static Dimension getDefaultDiffPanelSize() {
        return new Dimension(400, 200);
    }

    
    public static Dimension getDefaultDiffWindowSize() {
        Rectangle screenBounds = ScreenUtil.getMainScreenBounds();
        int width = (int)(screenBounds.width * 0.8);
        int height = (int)(screenBounds.height * 0.8);
        return new Dimension(width, height);
    }

    
    public static WindowWrapper.Mode getWindowMode( DiffDialogHints hints) {
        WindowWrapper.Mode mode = hints.getMode();
        if (mode == null) {
            boolean isUnderDialog = LaterInvocator.isInModalContext();
            mode = isUnderDialog ? WindowWrapper.Mode.MODAL : WindowWrapper.Mode.FRAME;
        }
        return mode;
    }

    public static void closeWindow( Window window, boolean modalOnly, boolean recursive) {
        if (window == null) return;

        Component component = window;
        while (component != null) {
            if (component instanceof Window) closeWindow((Window)component, modalOnly);

            component = recursive ? component.getParent() : null;
        }
    }

    public static void closeWindow( Window window, boolean modalOnly) {
        if (window instanceof IdeFrameImpl) return;
        if (modalOnly && window instanceof Frame) return;

        if (window instanceof DialogWrapperDialog) {
            ((DialogWrapperDialog)window).getDialogWrapper().doCancelAction();
            return;
        }

        window.setVisible(false);
        window.dispose();
    }

    //
    // UserData
    //

    public static <T> UserDataHolderBase createUserDataHolder( Key<T> key,  T value) {
        UserDataHolderBase holder = new UserDataHolderBase();
        holder.putUserData(key, value);
        return holder;
    }

    public static <T> UserDataHolderBase createUserDataHolder( Key<T> key1,  T value1,
                                                               Key<T> key2,  T value2) {
        UserDataHolderBase holder = new UserDataHolderBase();
        holder.putUserData(key1, value1);
        holder.putUserData(key2, value2);
        return holder;
    }

    public static boolean isUserDataFlagSet( Key<Boolean> key, UserDataHolder... holders) {
        for (UserDataHolder holder : holders) {
            if (holder == null) continue;
            Boolean data = holder.getUserData(key);
            if (data != null) return data;
        }
        return false;
    }

    public static <T> T getUserData( DiffRequest request,  DiffContext context,  Key<T> key) {
        if (request != null) {
            T data = request.getUserData(key);
            if (data != null) return data;
        }
        if (context != null) {
            T data = context.getUserData(key);
            if (data != null) return data;
        }
        return null;
    }

    public static <T> T getUserData( DiffContext context,  DiffRequest request,  Key<T> key) {
        if (context != null) {
            T data = context.getUserData(key);
            if (data != null) return data;
        }
        if (request != null) {
            T data = request.getUserData(key);
            if (data != null) return data;
        }
        return null;
    }

    //
    // DataProvider
    //

    
    public static Object getData( DataProvider provider,  DataProvider fallbackProvider,  String dataId) {
        if (provider != null) {
            Object data = provider.getData(dataId);
            if (data != null) return data;
        }
        if (fallbackProvider != null) {
            Object data = fallbackProvider.getData(dataId);
            if (data != null) return data;
        }
        return null;
    }

    //
    // Tools
    //

    
    public static <T extends DiffTool> List<T> filterSuppressedTools( List<T> tools) {
        if (tools.size() < 2) return tools;

        final List<Class<? extends DiffTool>> suppressedTools = new ArrayList<Class<? extends DiffTool>>();
        for (T tool : tools) {
            try {
                if (tool instanceof SuppressiveDiffTool) suppressedTools.addAll(((SuppressiveDiffTool)tool).getSuppressedTools());
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        if (suppressedTools.isEmpty()) return tools;

        List<T> filteredTools = ContainerUtil.filter(tools, new Condition<T>() {
            @Override
            public boolean value(T tool) {
                return !suppressedTools.contains(tool.getClass());
            }
        });

        return filteredTools.isEmpty() ? tools : filteredTools;
    }

    //
    // Helpers
    //

    public static class DocumentData {
         private final CharSequence myText1;
         private final CharSequence myText2;
        private final long myStamp1;
        private final long myStamp2;

        public DocumentData( CharSequence text1,  CharSequence text2, long stamp1, long stamp2) {
            myText1 = text1;
            myText2 = text2;
            myStamp1 = stamp1;
            myStamp2 = stamp2;
        }

        
        public CharSequence getText1() {
            return myText1;
        }

        
        public CharSequence getText2() {
            return myText2;
        }

        public long getStamp1() {
            return myStamp1;
        }

        public long getStamp2() {
            return myStamp2;
        }
    }

    public static class DiffConfig {
         public final ComparisonPolicy policy;
        public final boolean innerFragments;
        public final boolean squashFragments;
        public final boolean trimFragments;

        public DiffConfig( ComparisonPolicy policy, boolean innerFragments, boolean squashFragments, boolean trimFragments) {
            this.policy = policy;
            this.innerFragments = innerFragments;
            this.squashFragments = squashFragments;
            this.trimFragments = trimFragments;
        }

        public DiffConfig( IgnorePolicy ignorePolicy,  HighlightPolicy highlightPolicy) {
            this(ignorePolicy.getComparisonPolicy(), highlightPolicy.isFineFragments(), highlightPolicy.isShouldSquash(),
                    ignorePolicy.isShouldTrimChunks());
        }

        public DiffConfig() {
            this(IgnorePolicy.DEFAULT, HighlightPolicy.BY_LINE);
        }
    }
}
