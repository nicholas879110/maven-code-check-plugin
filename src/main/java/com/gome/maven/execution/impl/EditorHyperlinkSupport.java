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
package com.gome.maven.execution.impl;

import com.gome.maven.execution.filters.Filter;
import com.gome.maven.execution.filters.HyperlinkInfo;
import com.gome.maven.execution.filters.HyperlinkInfoBase;
import com.gome.maven.ide.OccurenceNavigator;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.LogicalPosition;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.event.EditorMouseAdapter;
import com.gome.maven.openapi.editor.event.EditorMouseEvent;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.ex.util.EditorUtil;
import com.gome.maven.openapi.editor.markup.HighlighterLayer;
import com.gome.maven.openapi.editor.markup.HighlighterTargetArea;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.pom.NavigatableAdapter;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.FilteringProcessor;
import com.gome.maven.util.containers.hash.LinkedHashMap;
import com.gome.maven.util.ui.UIUtil;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class EditorHyperlinkSupport {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.execution.impl.EditorHyperlinkSupport");
    public static final Key<TextAttributes> OLD_HYPERLINK_TEXT_ATTRIBUTES = Key.create("OLD_HYPERLINK_TEXT_ATTRIBUTES");
    private static final Key<HyperlinkInfoTextAttributes> HYPERLINK = Key.create("HYPERLINK");
    private static final int HYPERLINK_LAYER = HighlighterLayer.SELECTION - 123;
    private static final int HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 111;

    private final Editor myEditor;
     private final Project myProject;

    public EditorHyperlinkSupport( final Editor editor,  final Project project) {
        myEditor = editor;
        myProject = project;

        editor.addEditorMouseListener(new EditorMouseAdapter() {
            @Override
            public void mouseClicked(EditorMouseEvent e) {
                final MouseEvent mouseEvent = e.getMouseEvent();
                if (mouseEvent.getButton() == MouseEvent.BUTTON1 && !mouseEvent.isPopupTrigger()) {
                    Runnable runnable = getLinkNavigationRunnable(myEditor.xyToLogicalPosition(e.getMouseEvent().getPoint()));
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }
        });

        editor.getContentComponent().addMouseMotionListener(new MouseMotionAdapter() {
                                                                public void mouseMoved(final MouseEvent e) {
                                                                    final HyperlinkInfo info = getHyperlinkInfoByPoint(e.getPoint());
                                                                    if (info != null) {
                                                                        myEditor.getContentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                                                    }
                                                                    else {
                                                                        final Cursor cursor = editor instanceof EditorEx ?
                                                                                UIUtil.getTextCursor(((EditorEx)editor).getBackgroundColor()) :
                                                                                Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
                                                                        myEditor.getContentComponent().setCursor(cursor);
                                                                    }
                                                                }
                                                            }
        );
    }

    public void clearHyperlinks() {
        for (RangeHighlighter highlighter : getHyperlinks(0, myEditor.getDocument().getTextLength(), myEditor)) {
            removeHyperlink(highlighter);
        }
    }

    @Deprecated
    public Map<RangeHighlighter, HyperlinkInfo> getHyperlinks() {
        LinkedHashMap<RangeHighlighter, HyperlinkInfo> result = new LinkedHashMap<RangeHighlighter, HyperlinkInfo>();
        for (RangeHighlighter highlighter : getHyperlinks(0, myEditor.getDocument().getTextLength(), myEditor)) {
            HyperlinkInfo info = getHyperlinkInfo(highlighter);
            if (info != null) {
                result.put(highlighter, info);
            }
        }
        return result;
    }

    
    public Runnable getLinkNavigationRunnable(final LogicalPosition logical) {
        if (EditorUtil.inVirtualSpace(myEditor, logical)) {
            return null;
        }

        final RangeHighlighter range = findLinkRangeAt(this.myEditor.logicalPositionToOffset(logical));
        if (range != null) {
            final HyperlinkInfo hyperlinkInfo = getHyperlinkInfo(range);
            if (hyperlinkInfo != null) {
                return new Runnable() {
                    @Override
                    public void run() {
                        if (hyperlinkInfo instanceof HyperlinkInfoBase) {
                            RelativePoint point = new RelativePoint(myEditor.getContentComponent(), myEditor.logicalPositionToXY(logical));
                            ((HyperlinkInfoBase)hyperlinkInfo).navigate(myProject, point);
                        }
                        else {
                            hyperlinkInfo.navigate(myProject);
                        }
                        linkFollowed(myEditor, getHyperlinks(0, myEditor.getDocument().getTextLength(),myEditor), range);
                    }
                };
            }
        }
        return null;
    }

    
    public static HyperlinkInfo getHyperlinkInfo( RangeHighlighter range) {
        final HyperlinkInfoTextAttributes attributes = range.getUserData(HYPERLINK);
        return attributes != null ? attributes.getHyperlinkInfo() : null;
    }

    
    private RangeHighlighter findLinkRangeAt(final int offset) {
        //noinspection LoopStatementThatDoesntLoop
        for (final RangeHighlighter highlighter : getHyperlinks(offset, offset, myEditor)) {
            return highlighter;
        }
        return null;
    }

    
    private HyperlinkInfo getHyperlinkAt(final int offset) {
        RangeHighlighter range = findLinkRangeAt(offset);
        return range == null ? null : getHyperlinkInfo(range);
    }

    public List<RangeHighlighter> findAllHyperlinksOnLine(int line) {
        final int lineStart = myEditor.getDocument().getLineStartOffset(line);
        final int lineEnd = myEditor.getDocument().getLineEndOffset(line);
        return getHyperlinks(lineStart, lineEnd, myEditor);
    }

    public static List<RangeHighlighter> getHyperlinks(int startOffset, int endOffset, final Editor editor) {
        final MarkupModelEx markupModel = (MarkupModelEx)editor.getMarkupModel();
        final CommonProcessors.CollectProcessor<RangeHighlighterEx> processor = new CommonProcessors.CollectProcessor<RangeHighlighterEx>();
        markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset,
                new FilteringProcessor<RangeHighlighterEx>(new Condition<RangeHighlighterEx>() {
                    @Override
                    public boolean value(RangeHighlighterEx rangeHighlighterEx) {
                        return rangeHighlighterEx.getEditorFilter().avaliableIn(editor) &&
                                HYPERLINK_LAYER == rangeHighlighterEx.getLayer() &&
                                rangeHighlighterEx.isValid() &&
                                getHyperlinkInfo(rangeHighlighterEx) != null;
                    }
                }, processor)
        );
        return new ArrayList<RangeHighlighter>(processor.getResults());
    }

    public void removeHyperlink( RangeHighlighter hyperlink) {
        myEditor.getMarkupModel().removeHighlighter(hyperlink);
    }

    
    public HyperlinkInfo getHyperlinkInfoByLineAndCol(final int line, final int col) {
        return getHyperlinkAt(myEditor.logicalPositionToOffset(new LogicalPosition(line, col)));
    }

    /**
     * @deprecated for binary compatibility with older plugins
     * @see #createHyperlink(int, int, com.gome.maven.openapi.editor.markup.TextAttributes, com.gome.maven.execution.filters.HyperlinkInfo)
     */
    public void addHyperlink(final int highlightStartOffset,
                             final int highlightEndOffset,
                              final TextAttributes highlightAttributes,
                              final HyperlinkInfo hyperlinkInfo) {
        createHyperlink(highlightStartOffset, highlightEndOffset, highlightAttributes, hyperlinkInfo);
    }

    
    public RangeHighlighter createHyperlink(int highlightStartOffset,
                                            int highlightEndOffset,
                                             TextAttributes highlightAttributes,
                                             HyperlinkInfo hyperlinkInfo) {
        return createHyperlink(highlightStartOffset, highlightEndOffset, highlightAttributes, hyperlinkInfo, null);
    }

    
    private RangeHighlighter createHyperlink(final int highlightStartOffset,
                                             final int highlightEndOffset,
                                              final TextAttributes highlightAttributes,
                                              final HyperlinkInfo hyperlinkInfo,
                                              TextAttributes followedHyperlinkAttributes) {
        TextAttributes textAttributes = highlightAttributes != null ? highlightAttributes : getHyperlinkAttributes();
        final RangeHighlighter highlighter = myEditor.getMarkupModel().addRangeHighlighter(highlightStartOffset,
                highlightEndOffset,
                HYPERLINK_LAYER,
                textAttributes,
                HighlighterTargetArea.EXACT_RANGE);
        associateHyperlink(highlighter, hyperlinkInfo, followedHyperlinkAttributes);
        return highlighter;
    }

    public static void associateHyperlink( RangeHighlighter highlighter,  HyperlinkInfo hyperlinkInfo) {
        associateHyperlink(highlighter, hyperlinkInfo, null);
    }

    private static void associateHyperlink( RangeHighlighter highlighter,
                                            HyperlinkInfo hyperlinkInfo,
                                            TextAttributes followedHyperlinkAttributes) {
        highlighter.putUserData(HYPERLINK, new HyperlinkInfoTextAttributes(hyperlinkInfo, followedHyperlinkAttributes));
    }

    
    public HyperlinkInfo getHyperlinkInfoByPoint(final Point p) {
        final LogicalPosition pos = myEditor.xyToLogicalPosition(new Point(p.x, p.y));
        if (EditorUtil.inVirtualSpace(myEditor, pos)) {
            return null;
        }

        return getHyperlinkInfoByLineAndCol(pos.line, pos.column);
    }

    @Deprecated
    public void highlightHyperlinks(final Filter customFilter, final Filter predefinedMessageFilter, final int line1, final int endLine) {
        highlightHyperlinks(new Filter() {
            
            @Override
            public Result applyFilter(String line, int entireLength) {
                Result result = customFilter.applyFilter(line, entireLength);
                return result != null ? result : predefinedMessageFilter.applyFilter(line, entireLength);
            }
        }, line1, endLine);
    }

    public void highlightHyperlinks(final Filter customFilter, final int line1, final int endLine) {
        final Document document = myEditor.getDocument();

        final int startLine = Math.max(0, line1);

        for (int line = startLine; line <= endLine; line++) {
            int endOffset = document.getLineEndOffset(line);
            if (endOffset < document.getTextLength()) {
                endOffset++; // add '\n'
            }
            final String text = getLineText(document, line, true);
            Filter.Result result = customFilter.applyFilter(text, endOffset);
            if (result != null) {
                for (Filter.ResultItem resultItem : result.getResultItems()) {
                    int start = resultItem.getHighlightStartOffset();
                    int end = resultItem.getHighlightEndOffset();
                    if (end < start || end > document.getTextLength()) {
                        LOG.error("Filter returned wrong range: start=" + start + "; end=" + end + "; length=" + document.getTextLength() + "; filter=" + customFilter);
                        continue;
                    }

                    TextAttributes attributes = resultItem.getHighlightAttributes();
                    if (resultItem.getHyperlinkInfo() != null) {
                        createHyperlink(start, end, attributes, resultItem.getHyperlinkInfo(), resultItem.getFollowedHyperlinkAttributes());
                    }
                    else if (attributes != null) {
                        addHighlighter(start, end, attributes);
                    }
                }
            }
        }
    }

    public void addHighlighter(int highlightStartOffset, int highlightEndOffset, TextAttributes highlightAttributes) {
        myEditor.getMarkupModel().addRangeHighlighter(highlightStartOffset, highlightEndOffset, HIGHLIGHT_LAYER, highlightAttributes,
                HighlighterTargetArea.EXACT_RANGE);
    }

    private static TextAttributes getHyperlinkAttributes() {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
    }

    
    private static TextAttributes getFollowedHyperlinkAttributes( RangeHighlighter range) {
        HyperlinkInfoTextAttributes attrs = HYPERLINK.get(range);
        TextAttributes result = attrs != null ? attrs.getFollowedHyperlinkAttributes() : null;
        if (result == null) {
            result = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES);
        }
        return result;
    }

    
    public static OccurenceNavigator.OccurenceInfo getNextOccurrence(final Editor editor,
                                                                     final int delta,
                                                                     final Consumer<RangeHighlighter> action) {
        final List<RangeHighlighter> ranges = getHyperlinks(0, editor.getDocument().getTextLength(),editor);
        if (ranges.isEmpty()) {
            return null;
        }
        int i;
        for (i = 0; i < ranges.size(); i++) {
            RangeHighlighter range = ranges.get(i);
            if (range.getUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES) != null) {
                break;
            }
        }
        i = i % ranges.size();
        int newIndex = i;
        while (newIndex < ranges.size() && newIndex >= 0) {
            newIndex = (newIndex + delta + ranges.size()) % ranges.size();
            final RangeHighlighter next = ranges.get(newIndex);
            if (editor.getFoldingModel().getCollapsedRegionAtOffset(next.getStartOffset()) == null) {
                return new OccurenceNavigator.OccurenceInfo(new NavigatableAdapter() {
                    public void navigate(final boolean requestFocus) {
                        action.consume(next);
                        linkFollowed(editor, ranges, next);
                    }
                }, newIndex == -1 ? -1 : newIndex + 1, ranges.size());
            }
            if (newIndex == i) {
                break; // cycled through everything, found no next/prev hyperlink
            }
        }
        return null;
    }

    // todo fix link followed here!
    private static void linkFollowed(Editor editor, Collection<RangeHighlighter> ranges, final RangeHighlighter link) {
        MarkupModelEx markupModel = (MarkupModelEx)editor.getMarkupModel();
        for (RangeHighlighter range : ranges) {
            TextAttributes oldAttr = range.getUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES);
            if (oldAttr != null) {
                markupModel.setRangeHighlighterAttributes(range, oldAttr);
                range.putUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES, null);
            }
            if (range == link) {
                range.putUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES, range.getTextAttributes());
                markupModel.setRangeHighlighterAttributes(range, getFollowedHyperlinkAttributes(range));
            }
        }
        //refresh highlighter text attributes
        markupModel.addRangeHighlighter(0, 0, HYPERLINK_LAYER, getHyperlinkAttributes(), HighlighterTargetArea.EXACT_RANGE).dispose();
    }


    public static String getLineText(Document document, int lineNumber, boolean includeEol) {
        int endOffset = document.getLineEndOffset(lineNumber);
        if (includeEol && endOffset < document.getTextLength()) {
            endOffset++;
        }
        return document.getCharsSequence().subSequence(document.getLineStartOffset(lineNumber), endOffset).toString();
    }

    private static class HyperlinkInfoTextAttributes extends TextAttributes {
        private final HyperlinkInfo myHyperlinkInfo;
        private final TextAttributes myFollowedHyperlinkAttributes;

        public HyperlinkInfoTextAttributes( HyperlinkInfo hyperlinkInfo,  TextAttributes followedHyperlinkAttributes) {
            myHyperlinkInfo = hyperlinkInfo;
            myFollowedHyperlinkAttributes = followedHyperlinkAttributes;
        }

        
        public HyperlinkInfo getHyperlinkInfo() {
            return myHyperlinkInfo;
        }

        
        public TextAttributes getFollowedHyperlinkAttributes() {
            return myFollowedHyperlinkAttributes;
        }
    }
}