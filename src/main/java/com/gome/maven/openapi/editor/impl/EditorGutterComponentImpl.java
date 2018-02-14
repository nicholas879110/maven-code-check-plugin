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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jun 6, 2002
 * Time: 8:37:03 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.openapi.editor.impl;

import com.gome.maven.codeInsight.daemon.GutterMark;
import com.gome.maven.codeInsight.hint.TooltipController;
import com.gome.maven.codeInsight.hint.TooltipGroup;
import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.ide.dnd.*;
import com.gome.maven.ide.ui.UISettings;
import com.gome.maven.ide.ui.customization.CustomActionsSchema;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.impl.ApplicationImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.colors.ColorKey;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorFontType;
import com.gome.maven.openapi.editor.event.EditorMouseEventArea;
import com.gome.maven.openapi.editor.ex.*;
import com.gome.maven.openapi.editor.ex.util.EditorUtil;
import com.gome.maven.openapi.editor.markup.*;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.ui.popup.Balloon;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.ui.HintHint;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.Function;
import com.gome.maven.util.IconUtil;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntFunction;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectProcedure;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

class EditorGutterComponentImpl extends EditorGutterComponentEx implements MouseListener, MouseMotionListener, DataProvider {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.editor.impl.EditorGutterComponentImpl");
    private static final int START_ICON_AREA_WIDTH = 15;
    private static final int FREE_PAINTERS_AREA_WIDTH = 5;
    private static final int GAP_BETWEEN_ICONS = 3;
    private static final TooltipGroup GUTTER_TOOLTIP_GROUP = new TooltipGroup("GUTTER_TOOLTIP_GROUP", 0);
    public static final TIntFunction ID = new TIntFunction() {
        @Override
        public int execute(int value) {
            return value;
        }
    };

    private final EditorImpl myEditor;
    private final FoldingAnchorsOverlayStrategy myAnchorsDisplayStrategy;
    private int myLineMarkerAreaWidth = START_ICON_AREA_WIDTH + FREE_PAINTERS_AREA_WIDTH;
    private int myIconsAreaWidth = START_ICON_AREA_WIDTH;
    private int myLineNumberAreaWidth = 0;
    private int myAdditionalLineNumberAreaWidth = 0;
    private FoldRegion myActiveFoldRegion;
    private int myTextAnnotationGuttersSize = 0;
    private int myTextAnnotationExtraSize = 0;
    private TIntArrayList myTextAnnotationGutterSizes = new TIntArrayList();
    private ArrayList<TextAnnotationGutterProvider> myTextAnnotationGutters = new ArrayList<TextAnnotationGutterProvider>();
    private final Map<TextAnnotationGutterProvider, EditorGutterAction> myProviderToListener = new HashMap<TextAnnotationGutterProvider, EditorGutterAction>();
    private static final int GAP_BETWEEN_ANNOTATIONS = 5;
    private String myLastGutterToolTip = null;
     private TIntFunction myLineNumberConvertor;
     private TIntFunction myAdditionalLineNumberConvertor;
    private TIntFunction myLineNumberAreaWidthFunction;
    private boolean myShowDefaultGutterPopup = true;
    private TIntObjectHashMap<Color> myTextFgColors = new TIntObjectHashMap<Color>();

    @SuppressWarnings("unchecked")
    public EditorGutterComponentImpl(EditorImpl editor) {
        myEditor = editor;
        myLineNumberConvertor = ID;
        if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
            installDnD();
        }
        setOpaque(true);
        myAnchorsDisplayStrategy = new FoldingAnchorsOverlayStrategy(editor);
    }

    @SuppressWarnings({"ConstantConditions"})
    private void installDnD() {
        DnDSupport.createBuilder(this)
                .setBeanProvider(new Function<DnDActionInfo, DnDDragStartBean>() {
                    @Override
                    public DnDDragStartBean fun(DnDActionInfo info) {
                        final GutterMark renderer = getGutterRenderer(info.getPoint());
                        return renderer != null && (info.isCopy() || info.isMove()) ? new DnDDragStartBean(renderer) : null;
                    }
                })
                .setDropHandler(new DnDDropHandler() {
                    @Override
                    public void drop(DnDEvent e) {
                        final Object attachedObject = e.getAttachedObject();
                        if (attachedObject instanceof GutterIconRenderer) {
                            final GutterDraggableObject draggableObject = ((GutterIconRenderer)attachedObject).getDraggableObject();
                            if (draggableObject != null) {
                                final int line = convertPointToLineNumber(e.getPoint());
                                if (line != -1) {
                                    draggableObject.copy(line, myEditor.getVirtualFile());
                                }
                            }
                        }
                    }
                })
                .setImageProvider(new NullableFunction<DnDActionInfo, DnDImage>() {
                    @Override
                    public DnDImage fun(DnDActionInfo info) {
                        return new DnDImage(IconUtil.toImage(getGutterRenderer(info.getPoint()).getIcon()));
                    }
                })
                .install();
    }

    private void fireResized() {
        processComponentEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
    }

    @Override
    public Dimension getPreferredSize() {
        int w = getLineNumberAreaWidth() +
                getAnnotationsAreaWidthEx() +
                getLineMarkerAreaWidth() +
                getFoldingAreaWidth();

        return new Dimension(w, myEditor.getPreferredHeight());
    }

    @Override
    protected void setUI(ComponentUI newUI) {
        super.setUI(newUI);
        reinitSettings();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        reinitSettings();
    }

    public void reinitSettings() {
        revalidateMarkup();
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        ((ApplicationImpl)ApplicationManager.getApplication()).editorPaintStart();
        try {
            Rectangle clip = g.getClipBounds();
            if (clip.height < 0) return;

            final Graphics2D g2 = (Graphics2D)g;
            final AffineTransform old = g2.getTransform();

            if (isMirrored()) {
                final AffineTransform transform = new AffineTransform(old);
                transform.scale(-1, 1);
                transform.translate(-getWidth(), 0);
                g2.setTransform(transform);
            }

            UISettings.setupAntialiasing(g);
            Color backgroundColor = getBackground();
            paintLineNumbersBackground(g, clip, backgroundColor);
            paintAnnotationsBackground(g, clip, backgroundColor);

            Object hint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            if (!UIUtil.isRetina()) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            try {
                int firstVisibleOffset = myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(new Point(0, clip.y - myEditor.getLineHeight())));
                int lastVisibleOffset = myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(new Point(0, clip.y + clip.height + myEditor.getLineHeight())));
                paintFoldingBackground(g, clip, backgroundColor);
                paintLineMarkersBackground(g, clip, backgroundColor);
                paintBackground(g, clip, getLineMarkerAreaOffset(), getLineMarkerAreaWidth(), backgroundColor);
                paintEditorBackgrounds(g, firstVisibleOffset, lastVisibleOffset);
                paintAnnotations(g, clip);
                paintLineMarkers(g, firstVisibleOffset, lastVisibleOffset);
                paintFoldingLines((Graphics2D)g, clip);
                paintFoldingTree(g, clip, firstVisibleOffset, lastVisibleOffset);
                paintLineNumbers(g, clip);
            }
            finally {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
            }

            g2.setTransform(old);
        }
        finally {
            ((ApplicationImpl)ApplicationManager.getApplication()).editorPaintFinish();
        }
    }

    private void paintEditorBackgrounds(Graphics g, int firstVisibleOffset, int lastVisibleOffset) {
        myTextFgColors.clear();
        Color defaultBackgroundColor = myEditor.getBackgroundColor();
        Color defaultForegroundColor = myEditor.getColorsScheme().getDefaultForeground();
        int startX = myEditor.isInDistractionFreeMode() ? 0 : getWhitespaceSeparatorOffset() + (isFoldingOutlineShown() ? 1 : 0);
        IterationState state = new IterationState(myEditor, firstVisibleOffset, lastVisibleOffset, false, true);
        while (!state.atEnd()) {
            VisualPosition visualStart = myEditor.offsetToVisualPosition(state.getStartOffset());
            VisualPosition visualEnd   = myEditor.offsetToVisualPosition(state.getEndOffset());
            for (int line = visualStart.getLine(); line <= visualEnd.getLine(); line++) {
                if (line == visualStart.getLine()) {
                    if (visualStart.getColumn() == 0) {
                        drawEditorLineBackgroundRect(g, state, line, defaultBackgroundColor, defaultForegroundColor, startX, myEditor.visibleLineToY(line));
                    }
                }
                else if (line != visualEnd.getLine() || visualEnd.getColumn() != 0) {
                    drawEditorLineBackgroundRect(g, state, line, defaultBackgroundColor, defaultForegroundColor, startX, myEditor.visibleLineToY(line));
                }
            }
            state.advance();
        }
    }

    private void drawEditorLineBackgroundRect(Graphics g,
                                              IterationState state,
                                              int visualLine,
                                              Color defaultBackgroundColor,
                                              Color defaultForegroundColor,
                                              int startX,
                                              int startY) {
        TextAttributes attributes = state.getMergedAttributes();
        Color color = myEditor.getBackgroundColor(attributes);
        if (!Comparing.equal(color, defaultBackgroundColor)) {
            Color fgColor = attributes.getForegroundColor();
            if (!Comparing.equal(fgColor, defaultForegroundColor)) {
                myTextFgColors.put(visualLine, fgColor);
            }
            g.setColor(color);
            g.fillRect(startX, startY, getWidth() - startX, myEditor.getLineHeight());
        }
    }

    private void processClose(final MouseEvent e) {
        final IdeEventQueue queue = IdeEventQueue.getInstance();

        // See IDEA-59553 for rationale on why this feature is disabled
        //if (isLineNumbersShown()) {
        //  if (e.getX() >= getLineNumberAreaOffset() && getLineNumberAreaOffset() + getLineNumberAreaWidth() >= e.getX()) {
        //    queue.blockNextEvents(e);
        //    myEditor.getSettings().setLineNumbersShown(false);
        //    e.consume();
        //    return;
        //  }
        //}

        if (getGutterRenderer(e) != null) return;

        int x = getAnnotationsAreaOffset();
        for (int i = 0; i < myTextAnnotationGutters.size(); i++) {
            final int size = myTextAnnotationGutterSizes.get(i);
            if (x <= e.getX() && e.getX() <= x + size + GAP_BETWEEN_ANNOTATIONS) {
                queue.blockNextEvents(e);
                closeAllAnnotations();
                e.consume();
                break;
            }

            x += size + GAP_BETWEEN_ANNOTATIONS;
        }
    }

    private void paintAnnotationsBackground(Graphics g, Rectangle clip, Color backgroundColor) {
        int w = getAnnotationsAreaWidthEx();
        if (w == 0) return;
        paintBackground(g, clip, getAnnotationsAreaOffset(), w, backgroundColor);
    }

    private void paintAnnotations(Graphics g, Rectangle clip) {
        int x = getAnnotationsAreaOffset();
        int w = getAnnotationsAreaWidthEx();

        if (w == 0) return;

        Color color = myEditor.getColorsScheme().getColor(EditorColors.ANNOTATIONS_COLOR);
        g.setColor(color != null ? color : JBColor.blue);
        g.setFont(myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));

        for (int i = 0; i < myTextAnnotationGutters.size(); i++) {
            TextAnnotationGutterProvider gutterProvider = myTextAnnotationGutters.get(i);

            int lineHeight = myEditor.getLineHeight();
            int startLineNumber = clip.y / lineHeight;
            int endLineNumber = (clip.y + clip.height) / lineHeight + 1;
            int lastLine = myEditor.logicalToVisualPosition(
                    new LogicalPosition(endLineNumber(), 0))
                    .line;
            endLineNumber = Math.min(endLineNumber, lastLine + 1);
            if (startLineNumber >= endLineNumber) {
                break;
            }

            int annotationSize = myTextAnnotationGutterSizes.get(i);
            for (int j = startLineNumber; j < endLineNumber; j++) {
                int logLine = myEditor.visualToLogicalPosition(new VisualPosition(j, 0)).line;
                String s = gutterProvider.getLineText(logLine, myEditor);
                final EditorFontType style = gutterProvider.getStyle(logLine, myEditor);
                final Color bg = gutterProvider.getBgColor(logLine, myEditor);
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(x, j * lineHeight, annotationSize, lineHeight);
                }
                g.setColor(myEditor.getColorsScheme().getColor(gutterProvider.getColor(logLine, myEditor)));
                g.setFont(myEditor.getColorsScheme().getFont(style));
                if (s != null) {
                    g.drawString(s, x, (j+1) * lineHeight - myEditor.getDescent());
                }
            }

            x += annotationSize;
        }
    }

    private void paintFoldingTree(Graphics g, Rectangle clip, int firstVisibleOffset, int lastVisibleOffset) {
        if (isFoldingOutlineShown()) {
            doPaintFoldingTree((Graphics2D)g, clip, firstVisibleOffset, lastVisibleOffset);
        }
    }

    private void paintLineMarkersBackground(Graphics g, Rectangle clip, Color bgColor) {
        if (isLineMarkersShown()) {
            paintBackground(g, clip, getLineMarkerAreaOffset(), getLineMarkerAreaWidth(), bgColor);
        }
    }

    private void paintLineMarkers(Graphics g, int firstVisibleOffset, int lastVisibleOffset) {
        if (isLineMarkersShown()) {
            paintGutterRenderers(g, firstVisibleOffset, lastVisibleOffset);
        }
    }

    private void paintBackground(final Graphics g,
                                 final Rectangle clip,
                                 final int x,
                                 final int width,
                                 Color background) {
        g.setColor(background);
        g.fillRect(x, clip.y, width, clip.height);

        paintCaretRowBackground(g, x, width);
    }

    private void paintCaretRowBackground(final Graphics g, final int x, final int width) {
        if (!myEditor.getSettings().isCaretRowShown()) return;
        final VisualPosition visCaret = myEditor.getCaretModel().getVisualPosition();
        Color caretRowColor = myEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        if (caretRowColor != null) {
            g.setColor(caretRowColor);
            final Point caretPoint = myEditor.visualPositionToXY(visCaret);
            g.fillRect(x, caretPoint.y, width, myEditor.getLineHeight());
        }
    }

    private void paintLineNumbers(Graphics g, Rectangle clip) {
        if (isLineNumbersShown()) {
            int offset = getLineNumberAreaOffset() + myLineNumberAreaWidth;
            doPaintLineNumbers(g, clip, offset, myLineNumberConvertor);
            if (myAdditionalLineNumberConvertor != null) {
                doPaintLineNumbers(g, clip, offset + myAdditionalLineNumberAreaWidth, myAdditionalLineNumberConvertor);
            }
        }
    }

    private void paintLineNumbersBackground(Graphics g, Rectangle clip, Color bgColor) {
        if (isLineNumbersShown()) {
            paintBackground(g, clip, getLineNumberAreaOffset(), getLineNumberAreaWidth(), bgColor);
        }
    }

    @Override
    public Color getBackground() {
        if (myEditor.isInDistractionFreeMode()) {
            return myEditor.getBackgroundColor();
        }
        Color color = myEditor.getColorsScheme().getColor(EditorColors.GUTTER_BACKGROUND);
        return color != null ? color : EditorColors.GUTTER_BACKGROUND.getDefaultColor();
    }

    private void doPaintLineNumbers(Graphics g, Rectangle clip, int offset,  TIntFunction convertor) {
        if (!isLineNumbersShown()) {
            return;
        }
        int lineHeight = myEditor.getLineHeight();
        int startLineNumber = clip.y / lineHeight;
        int endLineNumber = (clip.y + clip.height) / lineHeight + 1;
        int lastLine = myEditor.logicalToVisualPosition(
                new LogicalPosition(endLineNumber(), 0))
                .line;
        endLineNumber = Math.min(endLineNumber, lastLine + 1);
        if (startLineNumber >= endLineNumber) {
            return;
        }

        Color color = myEditor.getColorsScheme().getColor(EditorColors.LINE_NUMBERS_COLOR);
        g.setColor(color != null ? color : JBColor.blue);
        g.setFont(myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));

        Graphics2D g2 = (Graphics2D)g;
        AffineTransform old = g2.getTransform();

        if (isMirrored()) {
            AffineTransform originalTransform = new AffineTransform(old);
            originalTransform.scale(-1, 1);
            originalTransform.translate(-getLineNumberAreaWidth() + 2, 0);
            g2.setTransform(originalTransform);
        }

        for (int i = startLineNumber; i < endLineNumber; i++) {
            LogicalPosition logicalPosition = myEditor.visualToLogicalPosition(new VisualPosition(i, 0));
            if (logicalPosition.softWrapLinesOnCurrentLogicalLine > 0) {
                continue;
            }
            int logLine = convertor.execute(logicalPosition.line);
            if (logLine >= 0) {
                String s = String.valueOf(logLine + 1);
                int startY = (i + 1) * lineHeight;
                if (myEditor.isInDistractionFreeMode()) {
                    Color fgColor = myTextFgColors.get(i);
                    g.setColor(fgColor != null ? fgColor : color != null ? color : JBColor.blue);
                }
                g.drawString(s,
                        offset -
                                myEditor.getFontMetrics(Font.PLAIN).stringWidth(s) -
                                4,
                        startY - myEditor.getDescent());
            }
        }

        g2.setTransform(old);
    }

    private int endLineNumber() {
        return Math.max(0, myEditor.getDocument().getLineCount() - 1);
    }

    
    @Override
    public Object getData( String dataId) {
        return EditorGutter.KEY.is(dataId) ? this : null;
    }

    private interface RangeHighlighterProcessor {
        void process( RangeHighlighter highlighter);
    }

    private void processRangeHighlighters(int startOffset, int endOffset,  RangeHighlighterProcessor processor) {
        Document document = myEditor.getDocument();
        final MarkupModelEx docMarkup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myEditor.getProject(), true);
        // we limit highlighters to process to between line starting at startOffset and line ending at endOffset
        DisposableIterator<RangeHighlighterEx> docHighlighters = docMarkup.overlappingIterator(startOffset, endOffset);
        DisposableIterator<RangeHighlighterEx> editorHighlighters = myEditor.getMarkupModel().overlappingIterator(startOffset, endOffset);

        try {
            RangeHighlighterEx lastDocHighlighter = null;
            RangeHighlighterEx lastEditorHighlighter = null;
            while (true) {
                if (lastDocHighlighter == null && docHighlighters.hasNext()) {
                    lastDocHighlighter = docHighlighters.next();
                    if (!lastDocHighlighter.isValid() || lastDocHighlighter.getAffectedAreaStartOffset() > endOffset) {
                        lastDocHighlighter = null;
                        continue;
                    }
                    if (lastDocHighlighter.getAffectedAreaEndOffset() < startOffset) {
                        lastDocHighlighter = null;
                        continue;
                    }
                }

                if (lastEditorHighlighter == null && editorHighlighters.hasNext()) {
                    lastEditorHighlighter = editorHighlighters.next();
                    if (!lastEditorHighlighter.isValid() || lastEditorHighlighter.getAffectedAreaStartOffset() > endOffset) {
                        lastEditorHighlighter = null;
                        continue;
                    }
                    if (lastEditorHighlighter.getAffectedAreaEndOffset() < startOffset) {
                        lastEditorHighlighter = null;
                        continue;
                    }
                }

                if (lastDocHighlighter == null && lastEditorHighlighter == null) return;

                final RangeHighlighterEx lowerHighlighter;
                if (less(lastDocHighlighter, lastEditorHighlighter)) {
                    lowerHighlighter = lastDocHighlighter;
                    lastDocHighlighter = null;
                }
                else {
                    lowerHighlighter = lastEditorHighlighter;
                    lastEditorHighlighter = null;
                }

                if (!lowerHighlighter.isValid()) continue;

                int startLineIndex = lowerHighlighter.getDocument().getLineNumber(startOffset);
                if (startLineIndex < 0 || startLineIndex >= document.getLineCount()) continue;

                int endLineIndex = lowerHighlighter.getDocument().getLineNumber(endOffset);
                if (endLineIndex < 0 || endLineIndex >= document.getLineCount()) continue;

                if (lowerHighlighter.getEditorFilter().avaliableIn(myEditor)) {
                    processor.process(lowerHighlighter);
                }
            }
        }
        finally {
            docHighlighters.dispose();
            editorHighlighters.dispose();
        }
    }

    private static boolean less(RangeHighlighter h1, RangeHighlighter h2) {
        return h1 != null && (h2 == null || h1.getStartOffset() < h2.getStartOffset());
    }

    @Override
    public void revalidateMarkup() {
        updateSize();
    }

    public void updateSize() {
        updateSize(false);
    }

    void updateSize(boolean onLayout) {
        int prevHash = sizeHash();
        updateSizeInner(onLayout);

        if (prevHash != sizeHash()) {
            fireResized();
        }
        repaint();
    }

    private void updateSizeInner(boolean onLayout) {
        if (!onLayout) {
            calcLineNumberAreaWidth();
            calcIconAreaWidth();
            calcAnnotationsSize();
        }
        calcAnnotationExtraSize();
    }

    private int sizeHash() {
        int result = myLineMarkerAreaWidth;
        result = 31 * result + myTextAnnotationGuttersSize;
        result = 31 * result + myTextAnnotationExtraSize;
        result = 31 * result + getLineNumberAreaWidth();
        return result;
    }

    private void calcAnnotationsSize() {
        myTextAnnotationGuttersSize = 0;
        final FontMetrics fontMetrics = myEditor.getFontMetrics(Font.PLAIN);
        final int lineCount = myEditor.getDocument().getLineCount();
        for (int j = 0; j < myTextAnnotationGutters.size(); j++) {
            TextAnnotationGutterProvider gutterProvider = myTextAnnotationGutters.get(j);
            int gutterSize = 0;
            for (int i = 0; i < lineCount; i++) {
                final String lineText = gutterProvider.getLineText(i, myEditor);
                if (lineText != null) {
                    gutterSize = Math.max(gutterSize, fontMetrics.stringWidth(lineText));
                }
            }
            if (gutterSize > 0) gutterSize += GAP_BETWEEN_ANNOTATIONS;
            myTextAnnotationGutterSizes.set(j, gutterSize);
            myTextAnnotationGuttersSize += gutterSize;
        }
    }

    private void calcAnnotationExtraSize() {
        myTextAnnotationExtraSize = 0;
        if (!myEditor.isInDistractionFreeMode() || isMirrored()) return;

        Window frame = SwingUtilities.getWindowAncestor(myEditor.getComponent());
        if (frame == null) return;

        EditorSettings settings = myEditor.getSettings();
        int rightMargin = settings.getRightMargin(myEditor.getProject());
        if (rightMargin <= 0) return;

        JComponent editorComponent = myEditor.getComponent();
        RelativePoint point = new RelativePoint(editorComponent, new Point(0, 0));
        Point editorLocationInWindow = point.getPoint(frame);

        int editorLocationX = (int)editorLocationInWindow.getX();
        int rightMarginX = rightMargin * EditorUtil.getSpaceWidth(Font.PLAIN, myEditor) + editorLocationX;

        int width = editorLocationX + editorComponent.getWidth();
        if (rightMarginX < width && editorLocationX < width - rightMarginX) {
            int centeredSize = (width - rightMarginX - editorLocationX) / 2 - (myLineMarkerAreaWidth + getLineNumberAreaWidth());
            myTextAnnotationExtraSize = Math.max(0, centeredSize - myTextAnnotationGuttersSize);
        }
    }

    private TIntObjectHashMap<List<GutterMark>> myLineToGutterRenderers;

    private void calcIconAreaWidth() {
        myLineToGutterRenderers = new TIntObjectHashMap<List<GutterMark>>();

        processRangeHighlighters(0, myEditor.getDocument().getTextLength(), new RangeHighlighterProcessor() {
            @Override
            public void process( RangeHighlighter highlighter) {
                GutterMark renderer = highlighter.getGutterIconRenderer();
                if (renderer == null) {
                    return;
                }
                if (!isHighlighterVisible(highlighter)) {
                    return;
                }
                int lineStartOffset = EditorUtil.getNotFoldedLineStartOffset(myEditor, highlighter.getStartOffset());
                int line = myEditor.getDocument().getLineNumber(lineStartOffset);
                List<GutterMark> renderers = myLineToGutterRenderers.get(line);
                if (renderers == null) {
                    renderers = new SmartList<GutterMark>();
                    myLineToGutterRenderers.put(line, renderers);
                }

                if (renderers.size() < 5) { // Don't allow more than 5 icons per line
                    renderers.add(renderer);
                }
            }
        });

        myIconsAreaWidth = START_ICON_AREA_WIDTH;

        myLineToGutterRenderers.forEachValue(new TObjectProcedure<List<GutterMark>>() {
            @Override
            public boolean execute(List<GutterMark> renderers) {
                int width = 1;
                for (int i = 0; i < renderers.size(); i++) {
                    GutterMark renderer = renderers.get(i);
                    width += renderer.getIcon().getIconWidth();
                    if (i > 0) width += GAP_BETWEEN_ICONS;
                }
                if (myIconsAreaWidth < width) {
                    myIconsAreaWidth = width + 1;
                }
                return true;
            }
        });

        myLineMarkerAreaWidth = myIconsAreaWidth + FREE_PAINTERS_AREA_WIDTH;
    }

    private boolean isHighlighterVisible(RangeHighlighter highlighter) {
        int startOffset = highlighter instanceof RangeHighlighterEx ?
                ((RangeHighlighterEx)highlighter).getAffectedAreaStartOffset() :
                highlighter.getStartOffset();
        int endOffset = highlighter instanceof RangeHighlighterEx ?
                ((RangeHighlighterEx)highlighter).getAffectedAreaEndOffset() :
                highlighter.getEndOffset();
        FoldRegion foldRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        return foldRegion == null || foldRegion.getEndOffset() < endOffset;
    }

    private void paintGutterRenderers(final Graphics g, int firstVisibleOffset, int lastVisibleOffset) {
        Graphics2D g2 = (Graphics2D)g;

        Object hint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            processRangeHighlighters(firstVisibleOffset, lastVisibleOffset, new RangeHighlighterProcessor() {
                @Override
                public void process( RangeHighlighter highlighter) {
                    paintLineMarkerRenderer(highlighter, g);
                }
            });
        }
        finally {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
        }

        int firstVisibleLine = myEditor.getDocument().getLineNumber(firstVisibleOffset);
        int lastVisibleLine = myEditor.getDocument().getLineNumber(lastVisibleOffset);
        paintIcons(firstVisibleLine, lastVisibleLine, g);
    }

    private void paintIcons(final int firstVisibleLine, final int lastVisibleLine, final Graphics g) {
        for (int line = firstVisibleLine; line <= lastVisibleLine; line++) {
            List<GutterMark> renderers = myLineToGutterRenderers.get(line);
            if (renderers != null) {
                paintIconRow(line, renderers, g);
            }
        }
    }

    private void paintIconRow(int line, List<GutterMark> row, final Graphics g) {
        processIconsRow(line, row, new LineGutterIconRendererProcessor() {
            @Override
            public void process(int x, int y, GutterMark renderer) {
                Icon icon = renderer.getIcon();
                icon.paintIcon(EditorGutterComponentImpl.this, g, x, y);
            }
        });
    }

    private void paintLineMarkerRenderer(RangeHighlighter highlighter, Graphics g) {
        Rectangle rectangle = getLineRendererRectangle(highlighter);

        if (rectangle != null) {
            final LineMarkerRenderer lineMarkerRenderer = highlighter.getLineMarkerRenderer();
            assert lineMarkerRenderer != null;
            lineMarkerRenderer.paint(myEditor, g, rectangle);
        }
    }

    
    private Rectangle getLineRendererRectangle(RangeHighlighter highlighter) {
        LineMarkerRenderer renderer = highlighter.getLineMarkerRenderer();
        if (renderer == null) return null;

        int startOffset = highlighter.getStartOffset();
        int endOffset = highlighter.getEndOffset();

        FoldRegion startFoldRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        FoldRegion endFoldRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(endOffset);
        if (startFoldRegion != null && endFoldRegion != null && startFoldRegion.equals(endFoldRegion)) {
            return null;
        }

        int startY = myEditor.visualPositionToXY(myEditor.offsetToVisualPosition(startOffset)).y;

        // top edge of the last line of the highlighted area
        int endY = myEditor.visualPositionToXY(myEditor.offsetToVisualPosition(endOffset)).y;
        // => add one line height to make height correct (bottom edge of the highlighted area)
        DocumentEx document = myEditor.getDocument();
        if (document.getLineStartOffset(document.getLineNumber(endOffset)) != endOffset) {
            // but if the highlighter ends with the end of line, its line number is the next line, but that line should not be highlighted
            endY += myEditor.getLineHeight();
        }

        int height = endY - startY;
        int w = FREE_PAINTERS_AREA_WIDTH;
        int x = getLineMarkerAreaOffset() + myIconsAreaWidth - 1;
        return new Rectangle(x, startY, w, height);
    }

    private interface LineGutterIconRendererProcessor {
        void process(int x, int y, GutterMark renderer);
    }

    private void processIconsRow(int line, List<GutterMark> row, LineGutterIconRendererProcessor processor) {
        int middleCount = 0;
        int middleSize = 0;
        int x = getLineMarkerAreaOffset() + 2;
        final int y = myEditor.logicalPositionToXY(new LogicalPosition(line, 0)).y;

        for (GutterMark r : row) {
            final GutterIconRenderer.Alignment alignment = ((GutterIconRenderer)r).getAlignment();
            final Icon icon = r.getIcon();
            if (alignment == GutterIconRenderer.Alignment.LEFT) {
                processor.process(x, y + getTextAlignmentShift(icon), r);
                x += icon.getIconWidth() + GAP_BETWEEN_ICONS;
            }
            else if (alignment == GutterIconRenderer.Alignment.CENTER) {
                middleCount++;
                middleSize += icon.getIconWidth() + GAP_BETWEEN_ICONS;
            }
        }

        final int leftSize = x - getLineMarkerAreaOffset();

        x = getLineMarkerAreaOffset() + myIconsAreaWidth - 2; // because of 2px LineMarkerRenderers
        for (GutterMark r : row) {
            if (((GutterIconRenderer)r).getAlignment() == GutterIconRenderer.Alignment.RIGHT) {
                Icon icon = r.getIcon();
                x -= icon.getIconWidth();
                processor.process(x, y + getTextAlignmentShift(icon), r);
                x -= GAP_BETWEEN_ICONS;
            }
        }

        int rightSize = myIconsAreaWidth + getLineMarkerAreaOffset() - x + 1;

        if (middleCount > 0) {
            middleSize -= GAP_BETWEEN_ICONS;
            x = getLineMarkerAreaOffset() + leftSize + (myIconsAreaWidth - leftSize - rightSize - middleSize) / 2;
            for (GutterMark r : row) {
                if (((GutterIconRenderer)r).getAlignment() == GutterIconRenderer.Alignment.CENTER) {
                    Icon icon = r.getIcon();
                    processor.process(x, y + getTextAlignmentShift(icon), r);
                    x += icon.getIconWidth() + GAP_BETWEEN_ICONS;
                }
            }
        }
    }

    private int getTextAlignmentShift(Icon icon) {
        return (myEditor.getLineHeight() - icon.getIconHeight()) /2;
    }

    @Override
    public Color getOutlineColor(boolean isActive) {
        ColorKey key = isActive ? EditorColors.SELECTED_TEARLINE_COLOR : EditorColors.TEARLINE_COLOR;
        Color color = myEditor.getColorsScheme().getColor(key);
        return color != null ? color : JBColor.black;
    }

    @Override
    public void registerTextAnnotation( TextAnnotationGutterProvider provider) {
        myTextAnnotationGutters.add(provider);
        myTextAnnotationGutterSizes.add(0);
        updateSize();
    }

    @Override
    public void registerTextAnnotation( TextAnnotationGutterProvider provider,  EditorGutterAction action) {
        myTextAnnotationGutters.add(provider);
        myProviderToListener.put(provider, action);
        myTextAnnotationGutterSizes.add(0);
        updateSize();
    }

    private void doPaintFoldingTree(final Graphics2D g, final Rectangle clip, int firstVisibleOffset, int lastVisibleOffset) {
        final int anchorX = getFoldingAreaOffset();
        final int width = getFoldingAnchorWidth();

        Collection<DisplayedFoldingAnchor> anchorsToDisplay =
                myAnchorsDisplayStrategy.getAnchorsToDisplay(firstVisibleOffset, lastVisibleOffset, myActiveFoldRegion);
        for (DisplayedFoldingAnchor anchor : anchorsToDisplay) {
            drawAnchor(width, clip, g, anchorX, anchor.visualLine, anchor.type, anchor.foldRegion == myActiveFoldRegion);
        }
    }

    private void paintFoldingBackground(Graphics g, Rectangle clip, Color bgColor) {
        int lineX = getWhitespaceSeparatorOffset();
        paintBackground(g, clip, getFoldingAreaOffset(), getFoldingAreaWidth(), bgColor);

        g.setColor(myEditor.getBackgroundColor());
        g.fillRect(lineX, clip.y, getFoldingAreaWidth(), clip.height);

        paintCaretRowBackground(g, lineX, getFoldingAnchorWidth());
    }

    private void paintFoldingLines(final Graphics2D g, final Rectangle clip) {
        if (!isFoldingOutlineShown()) return;

        g.setColor(getOutlineColor(false));
        int x = getWhitespaceSeparatorOffset();
        UIUtil.drawLine(g, x, clip.y, x, clip.y + clip.height);

        final int anchorX = getFoldingAreaOffset();
        final int width = getFoldingAnchorWidth();

        if (myActiveFoldRegion != null && myActiveFoldRegion.isExpanded() && myActiveFoldRegion.isValid()) {
            int foldStart = myEditor.offsetToVisualLine(myActiveFoldRegion.getStartOffset());
            int foldEnd = myEditor.offsetToVisualLine(getEndOffset(myActiveFoldRegion));
            int startY = myEditor.visibleLineToY(foldStart + 1) - myEditor.getDescent();
            int endY = myEditor.visibleLineToY(foldEnd) + myEditor.getLineHeight() -
                    myEditor.getDescent();

            if (startY <= clip.y + clip.height && endY + 1 + myEditor.getDescent() >= clip.y) {
                int lineX = anchorX + width / 2;

                g.setColor(getOutlineColor(true));
                UIUtil.drawLine(g, lineX, startY, lineX, endY);
            }
        }
    }

    @Override
    public int getWhitespaceSeparatorOffset() {
        return getFoldingAreaOffset() + getFoldingAnchorWidth() / 2;
    }

    public void setActiveFoldRegion(FoldRegion activeFoldRegion) {
        if (myActiveFoldRegion != activeFoldRegion) {
            myActiveFoldRegion = activeFoldRegion;
            repaint();
        }
    }

    public int getHeadCenterY(FoldRegion foldRange) {
        int width = getFoldingAnchorWidth();
        int foldStart = myEditor.offsetToVisualLine(foldRange.getStartOffset());

        return myEditor.visibleLineToY(foldStart) + myEditor.getLineHeight() - myEditor.getDescent() - width / 2;
    }

    private void drawAnchor(int width, Rectangle clip, Graphics2D g, int anchorX, int visualLine,
                            DisplayedFoldingAnchor.Type type, boolean active) {

        final int off = JBUI.scale(2);
        int height = width + off;
        int y;
        switch (type) {
            case COLLAPSED:
                y = myEditor.visibleLineToY(visualLine) + myEditor.getLineHeight() - myEditor.getDescent() - width;
                if (y <= clip.y + clip.height && y + height >= clip.y) {
                    drawSquareWithPlus(g, anchorX, y, width, active);
                }
                break;
            case EXPANDED_TOP:
                y = myEditor.visibleLineToY(visualLine) + myEditor.getLineHeight() - myEditor.getDescent() - width;
                if (y <= clip.y + clip.height && y + height >= clip.y) {
                    drawDirectedBox(g, anchorX, y, width, height, width - off, active);
                }
                break;
            case EXPANDED_BOTTOM:
                y = myEditor.visibleLineToY(visualLine) + myEditor.getLineHeight() - myEditor.getDescent();
                if (y - height <= clip.y + clip.height && y >= clip.y) {
                    drawDirectedBox(g, anchorX, y, width, -height, -width + off, active);
                }
                break;
        }
    }

    private int getEndOffset(FoldRegion foldRange) {
        LOG.assertTrue(foldRange.isValid(), foldRange);
        FoldingGroup group = foldRange.getGroup();
        return group == null ? foldRange.getEndOffset() : myEditor.getFoldingModel().getEndOffset(group);
    }

    private void drawDirectedBox(Graphics2D g,
                                 int anchorX,
                                 int y,
                                 int width,
                                 int height,
                                 int baseHeight,
                                 boolean active) {
        Object antialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (SystemInfo.isMac && SystemInfo.JAVA_VERSION.startsWith("1.4.1") || UIUtil.isRetina()) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        try {
            final int off = JBUI.scale(2);
            int[] xPoints = {anchorX, anchorX + width, anchorX + width, anchorX + width / 2, anchorX};
            int[] yPoints = {y, y, y + baseHeight, y + height, y + baseHeight};

            g.setColor(myEditor.getBackgroundColor());
            g.fillPolygon(xPoints, yPoints, 5);

            g.setColor(getOutlineColor(active));
            g.drawPolygon(xPoints, yPoints, 5);

            //Minus
            int minusHeight = y + baseHeight / 2 + (height - baseHeight) / 4;
            UIUtil.drawLine(g, anchorX + off, minusHeight, anchorX + width - off, minusHeight);
        }
        finally {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
        }
    }

    private void drawSquareWithPlus(Graphics2D g,
                                    int anchorX,
                                    int y,
                                    int width,
                                    boolean active) {
        drawSquareWithMinus(g, anchorX, y, width, active);
        final int off = JBUI.scale(2);
        UIUtil.drawLine(g, anchorX + width / 2, y + off, anchorX + width / 2, y + width - off);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void drawSquareWithMinus(Graphics2D g,
                                     int anchorX,
                                     int y,
                                     int width,
                                     boolean active) {
        g.setColor(myEditor.getBackgroundColor());
        g.fillRect(anchorX, y, width, width);

        g.setColor(getOutlineColor(active));
        g.drawRect(anchorX, y, width, width);
        final int off = JBUI.scale(2);
        // Draw plus
        if (!active) g.setColor(getOutlineColor(true));
        UIUtil.drawLine(g, anchorX + off, y + width / 2, anchorX + width - off, y + width / 2);
    }

    private int getFoldingAnchorWidth() {
        return Math.min(JBUI.scale(4), myEditor.getLineHeight() / 2 - JBUI.scale(2)) * 2;
    }

    public int getFoldingAreaOffset() {
        return getLineMarkerAreaOffset() + getLineMarkerAreaWidth();
    }

    public int getFoldingAreaWidth() {
        int width = isFoldingOutlineShown() ? getFoldingAnchorWidth() + 2 : (isRealEditor() ? getFoldingAnchorWidth() : 0);
        return JBUI.scale(width);
    }

    public boolean isRealEditor() {
        return EditorUtil.isRealFileEditor(myEditor);
    }

    @Override
    public boolean isLineMarkersShown() {
        return myEditor.getSettings().isLineMarkerAreaShown();
    }

    public boolean isLineNumbersShown() {
        return myEditor.getSettings().isLineNumbersShown();
    }

    @Override
    public boolean isAnnotationsShown() {
        return !myTextAnnotationGutters.isEmpty();
    }

    @Override
    public boolean isFoldingOutlineShown() {
        return myEditor.getSettings().isFoldingOutlineShown() &&
                myEditor.getFoldingModel().isFoldingEnabled() &&
                !myEditor.isInPresentationMode();
    }

    public int getLineNumberAreaWidth() {
        return isLineNumbersShown() ? myLineNumberAreaWidth + myAdditionalLineNumberAreaWidth : 0;
    }

    public int getLineMarkerAreaWidth() {
        return isLineMarkersShown() ? myLineMarkerAreaWidth : 0;
    }

    public void setLineNumberAreaWidthFunction( TIntFunction calculator) {
        myLineNumberAreaWidthFunction = calculator;
    }

    private void calcLineNumberAreaWidth() {
        if (myLineNumberAreaWidthFunction == null || !isLineNumbersShown()) return;

        int maxLineNumber = getMaxLineNumber(myLineNumberConvertor);
        myLineNumberAreaWidth = myLineNumberAreaWidthFunction.execute(maxLineNumber) + 4;

        myAdditionalLineNumberAreaWidth = 0;
        if (myAdditionalLineNumberConvertor != null) {
            int maxAdditionalLineNumber = getMaxLineNumber(myAdditionalLineNumberConvertor);
            myAdditionalLineNumberAreaWidth = myLineNumberAreaWidthFunction.execute(maxAdditionalLineNumber) + 4;
        }
    }

    private int getMaxLineNumber( TIntFunction convertor) {
        for (int i = endLineNumber(); i >= 0; i--) {
            int number = convertor.execute(i);
            if (number >= 0) {
                return number;
            }
        }
        return 0;
    }

    
    public EditorMouseEventArea getEditorMouseAreaByOffset(int offset) {
        int x = offset - getLineNumberAreaOffset();

        if (x >= 0 && (x -= getLineNumberAreaWidth()) < 0) {
            return EditorMouseEventArea.LINE_NUMBERS_AREA;
        }

        if (x >= 0 && (x -= getAnnotationsAreaWidth()) < 0) {
            return EditorMouseEventArea.ANNOTATIONS_AREA;
        }

        if (x >= 0 && (x -= myTextAnnotationExtraSize) < 0) {
            return EditorMouseEventArea.LINE_MARKERS_AREA;
        }

        if (x >= 0 && (x -= getLineMarkerAreaWidth()) < 0) {
            return EditorMouseEventArea.LINE_MARKERS_AREA;
        }

        if (x >= 0 && (x - getFoldingAreaWidth()) < 0) {
            return EditorMouseEventArea.FOLDING_OUTLINE_AREA;
        }

        return null;
    }

    public static int getLineNumberAreaOffset() {
        return 0;
    }

    public int getAnnotationsAreaOffset() {
        return getLineNumberAreaOffset() + getLineNumberAreaWidth();
    }

    public int getAnnotationsAreaWidth() {
        return myTextAnnotationGuttersSize;
    }

    public int getAnnotationsAreaWidthEx() {
        return myTextAnnotationGuttersSize + myTextAnnotationExtraSize;
    }

    @Override
    public int getLineMarkerAreaOffset() {
        return getAnnotationsAreaOffset() + getAnnotationsAreaWidthEx();
    }

    @Override
    public int getIconsAreaWidth() {
        return myIconsAreaWidth;
    }

    private boolean isMirrored() {
        return myEditor.getVerticalScrollbarOrientation() != EditorEx.VERTICAL_SCROLLBAR_RIGHT;
    }

    
    @Override
    public FoldRegion findFoldingAnchorAt(int x, int y) {
        if (!myEditor.getSettings().isFoldingOutlineShown()) return null;

        int anchorX = getFoldingAreaOffset();
        int anchorWidth = getFoldingAnchorWidth();

        int neighbourhoodStartOffset = myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(new Point(0, y - myEditor.getLineHeight())));
        int neighbourhoodEndOffset = myEditor.logicalPositionToOffset(myEditor.xyToLogicalPosition(new Point(0, y + myEditor.getLineHeight())));

        Collection<DisplayedFoldingAnchor> displayedAnchors = myAnchorsDisplayStrategy.getAnchorsToDisplay(neighbourhoodStartOffset, neighbourhoodEndOffset, null);
        for (DisplayedFoldingAnchor anchor : displayedAnchors) {
            if (rectangleByFoldOffset(anchor.visualLine, anchorWidth, anchorX).contains(convertX(x), y)) return anchor.foldRegion;
        }

        return null;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Rectangle rectangleByFoldOffset(int foldStart, int anchorWidth, int anchorX) {
        int anchorY = myEditor.visibleLineToY(foldStart) + myEditor.getLineHeight() -
                myEditor.getDescent() - anchorWidth;
        return new Rectangle(anchorX, anchorY, anchorWidth, anchorWidth);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        TooltipController.getInstance().cancelTooltips();
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        String toolTip = null;
        final GutterIconRenderer renderer = getGutterRenderer(e);
        TooltipController controller = TooltipController.getInstance();
        if (renderer != null) {
            toolTip = renderer.getTooltipText();
        }
        else {
            ActiveGutterRenderer lineRenderer = getActiveRendererByMouseEvent(e);
            if (lineRenderer == null) {
                TextAnnotationGutterProvider provider = getProviderAtPoint(e.getPoint());
                if (provider != null) {
                    final int line = getLineNumAtPoint(e.getPoint());
                    toolTip = provider.getToolTip(line, myEditor);
                    if (!Comparing.equal(toolTip, myLastGutterToolTip)) {
                        controller.cancelTooltip(GUTTER_TOOLTIP_GROUP, e, true);
                        myLastGutterToolTip = toolTip;
                    }
                }
            }
        }

        if (toolTip != null && !toolTip.isEmpty()) {
            final Ref<Point> t = new Ref<Point>(e.getPoint());
            int line = EditorUtil.yPositionToLogicalLine(myEditor, e);
            List<GutterMark> row = myLineToGutterRenderers.get(line);
            Balloon.Position ballPosition = Balloon.Position.atRight;
            if (row != null) {
                final TreeMap<Integer, GutterMark> xPos = new TreeMap<Integer, GutterMark>();
                final int[] currentPos = {0};
                processIconsRow(line, row, new LineGutterIconRendererProcessor() {
                    @Override
                    public void process(int x, int y, GutterMark r) {
                        xPos.put(x, r);
                        if (renderer == r && r != null) {
                            currentPos[0] = x;
                            Icon icon = r.getIcon();
                            t.set(new Point(x + icon.getIconWidth() / 2, y + icon.getIconHeight() / 2));
                        }
                    }
                });

                List<Integer> xx = new ArrayList<Integer>(xPos.keySet());
                int posIndex = xx.indexOf(currentPos[0]);
                if (xPos.size() > 1 && posIndex == 0) {
                    ballPosition = Balloon.Position.below;
                }
            }

            RelativePoint showPoint = new RelativePoint(this, t.get());

            controller.showTooltipByMouseMove(myEditor, showPoint, ((EditorMarkupModel)myEditor.getMarkupModel()).getErrorStripTooltipRendererProvider().calcTooltipRenderer(toolTip), false, GUTTER_TOOLTIP_GROUP,
                    new HintHint(this, t.get()).setAwtTooltip(true).setPreferredPosition(ballPosition));
        }
        else {
            controller.cancelTooltip(GUTTER_TOOLTIP_GROUP, e, false);
        }
    }

    void validateMousePointer( MouseEvent e) {
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        FoldRegion foldingAtCursor = findFoldingAnchorAt(e.getX(), e.getY());
        setActiveFoldRegion(foldingAtCursor);
        if (foldingAtCursor != null) {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        }
        GutterIconRenderer renderer = getGutterRenderer(e);
        if (renderer != null) {
            if (renderer.isNavigateAction()) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
        }
        else {
            ActiveGutterRenderer lineRenderer = getActiveRendererByMouseEvent(e);
            if (lineRenderer != null) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
            else {
                TextAnnotationGutterProvider provider = getProviderAtPoint(e.getPoint());
                if (provider != null) {
                    if (myProviderToListener.containsKey(provider)) {
                        EditorGutterAction action = myProviderToListener.get(provider);
                        if (action != null) {
                            int line = getLineNumAtPoint(e.getPoint());
                            cursor = action.getCursor(line);
                        }
                    }
                }
            }
        }
        setCursor(cursor);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            invokePopup(e);
        }
    }

    private void fireEventToTextAnnotationListeners(final MouseEvent e) {
        if (myEditor.getMouseEventArea(e) == EditorMouseEventArea.ANNOTATIONS_AREA) {
            final Point clickPoint = e.getPoint();

            final TextAnnotationGutterProvider provider = getProviderAtPoint(clickPoint);

            if (provider == null) {
                return;
            }

            if (myProviderToListener.containsKey(provider)) {
                int line = getLineNumAtPoint(clickPoint);

                if (line >= 0 && line < myEditor.getDocument().getLineCount() && UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED)) {
                    myProviderToListener.get(provider).doAction(line);
                }

            }
        }
    }

    private int getLineNumAtPoint(final Point clickPoint) {
        return EditorUtil.yPositionToLogicalLine(myEditor, clickPoint);
    }

    
    private TextAnnotationGutterProvider getProviderAtPoint(final Point clickPoint) {
        int current = getAnnotationsAreaOffset();
        if (clickPoint.x < current) return null;
        for (int i = 0; i < myTextAnnotationGutterSizes.size(); i++) {
            current += myTextAnnotationGutterSizes.get(i);
            if (clickPoint.x <= current) return myTextAnnotationGutters.get(i);
        }

        return null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            invokePopup(e);
        } else if (UIUtil.isCloseClick(e)) {
            processClose(e);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.isPopupTrigger()) {
            invokePopup(e);
            return;
        }

        GutterIconRenderer renderer = getGutterRenderer(e);
        AnAction clickAction = null;
        if (renderer != null && e.getButton() < 4) {
            clickAction = (InputEvent.BUTTON2_MASK & e.getModifiers()) > 0
                    ? renderer.getMiddleButtonClickAction()
                    : renderer.getClickAction();
        }
        if (clickAction != null) {
            clickAction.actionPerformed(new AnActionEvent(e, myEditor.getDataContext(), "ICON_NAVIGATION", clickAction.getTemplatePresentation(),
                    ActionManager.getInstance(),
                    e.getModifiers()));
            e.consume();
            repaint();
        }
        else {
            ActiveGutterRenderer lineRenderer = getActiveRendererByMouseEvent(e);
            if (lineRenderer != null) {
                lineRenderer.doAction(myEditor, e);
            } else {
                fireEventToTextAnnotationListeners(e);
            }
        }
    }

    
    private ActiveGutterRenderer getActiveRendererByMouseEvent(final MouseEvent e) {
        if (findFoldingAnchorAt(e.getX(), e.getY()) != null) {
            return null;
        }
        if (e.isConsumed() || e.getX() > getWhitespaceSeparatorOffset()) {
            return null;
        }
        final ActiveGutterRenderer[] gutterRenderer = {null};
        Rectangle clip = myEditor.getScrollingModel().getVisibleArea();
        int firstVisibleOffset = myEditor.logicalPositionToOffset(
                myEditor.xyToLogicalPosition(new Point(0, clip.y - myEditor.getLineHeight())));
        int lastVisibleOffset = myEditor.logicalPositionToOffset(
                myEditor.xyToLogicalPosition(new Point(0, clip.y + clip.height + myEditor.getLineHeight())));

        processRangeHighlighters(firstVisibleOffset, lastVisibleOffset, new RangeHighlighterProcessor() {
            @Override
            public void process( RangeHighlighter highlighter) {
                if (gutterRenderer[0] != null) return;
                Rectangle rectangle = getLineRendererRectangle(highlighter);
                if (rectangle == null) return;

                int startY = rectangle.y;
                int endY = startY + rectangle.height;
                if (startY == endY) {
                    endY += myEditor.getLineHeight();
                }

                if (startY < e.getY() && e.getY() <= endY) {
                    final LineMarkerRenderer renderer = highlighter.getLineMarkerRenderer();
                    if (renderer instanceof ActiveGutterRenderer && ((ActiveGutterRenderer)renderer).canDoAction(e)) {
                        gutterRenderer[0] = (ActiveGutterRenderer)renderer;
                    }
                }
            }
        });
        return gutterRenderer[0];
    }

    @Override
    public void closeAllAnnotations() {
        for (TextAnnotationGutterProvider provider : myTextAnnotationGutters) {
            provider.gutterClosed();
        }

        revalidateSizes();
    }

    private void revalidateSizes() {
        myTextAnnotationGutters = new ArrayList<TextAnnotationGutterProvider>();
        myTextAnnotationGutterSizes = new TIntArrayList();
        updateSize();
    }

    private class CloseAnnotationsAction extends DumbAwareAction {
        public CloseAnnotationsAction() {
            super(EditorBundle.message("close.editor.annotations.action.name"));
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            closeAllAnnotations();
        }
    }

    @Override
    
    public Point getPoint(final GutterIconRenderer renderer) {
        final Ref<Point> result = Ref.create();
        for (int line : myLineToGutterRenderers.keys()) {
            processIconsRow(line, myLineToGutterRenderers.get(line), new LineGutterIconRendererProcessor() {
                @Override
                public void process(int x, int y, GutterMark r) {
                    if (result.isNull() && r.equals(renderer)) {
                        result.set(new Point(x, y));
                    }
                }
            });

            if (!result.isNull()) {
                return result.get();
            }
        }
        return null;
    }

    @Override
    public void setLineNumberConvertor( TIntFunction lineNumberConvertor) {
        setLineNumberConvertor(lineNumberConvertor, null);
    }

    @Override
    public void setLineNumberConvertor( TIntFunction lineNumberConvertor1,  TIntFunction lineNumberConvertor2) {
        myLineNumberConvertor = lineNumberConvertor1;
        myAdditionalLineNumberConvertor = lineNumberConvertor2;
    }

    @Override
    public void setShowDefaultGutterPopup(boolean show) {
        myShowDefaultGutterPopup = show;
    }

    private void invokePopup(MouseEvent e) {
        final ActionManager actionManager = ActionManager.getInstance();
        if (myEditor.getMouseEventArea(e) == EditorMouseEventArea.ANNOTATIONS_AREA) {
            DefaultActionGroup actionGroup = new DefaultActionGroup(EditorBundle.message("editor.annotations.action.group.name"), true);
            actionGroup.add(new CloseAnnotationsAction());
            final List<AnAction> addActions = new ArrayList<AnAction>();
            final Point p = e.getPoint();
            int line = EditorUtil.yPositionToLogicalLine(myEditor, p);
            //if (line >= myEditor.getDocument().getLineCount()) return;

            for (TextAnnotationGutterProvider gutterProvider : myTextAnnotationGutters) {
                final List<AnAction> list = gutterProvider.getPopupActions(line, myEditor);
                if (list != null) {
                    for (AnAction action : list) {
                        if (! addActions.contains(action)) {
                            addActions.add(action);
                        }
                    }
                }
            }
            for (AnAction addAction : addActions) {
                actionGroup.add(addAction);
            }
            JPopupMenu menu = actionManager.createActionPopupMenu("", actionGroup).getComponent();
            menu.show(this, e.getX(), e.getY());
        }
        else {
            GutterIconRenderer renderer = getGutterRenderer(e);
            if (renderer != null) {
                ActionGroup actionGroup = renderer.getPopupMenuActions();
                if (actionGroup != null) {
                    ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN,
                            actionGroup);
                    popupMenu.getComponent().show(this, e.getX(), e.getY());
                    e.consume();
                } else {
                    AnAction rightButtonAction = renderer.getRightButtonClickAction();
                    if (rightButtonAction != null) {
                        rightButtonAction.actionPerformed(new AnActionEvent(e, myEditor.getDataContext(), "ICON_NAVIGATION_SECONDARY_BUTTON", rightButtonAction.getTemplatePresentation(),
                                ActionManager.getInstance(),
                                e.getModifiers()));
                        e.consume();
                    }
                }
            }
            else {
                if (myShowDefaultGutterPopup) {
                    ActionGroup group = (ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_EDITOR_GUTTER);
                    ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN, group);
                    popupMenu.getComponent().show(this, e.getX(), e.getY());
                }
                e.consume();
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        TooltipController.getInstance().cancelTooltip(GUTTER_TOOLTIP_GROUP, e, false);
    }

    private int convertPointToLineNumber(final Point p) {
        int line = EditorUtil.yPositionToLogicalLine(myEditor, p);

        if (line >= myEditor.getDocument().getLineCount()) return -1;
        int startOffset = myEditor.getDocument().getLineStartOffset(line);
        final FoldRegion region = myEditor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        if (region != null) {
            line = myEditor.getDocument().getLineNumber(region.getEndOffset());
            if (line >= myEditor.getDocument().getLineCount()) return -1;
        }
        return line;
    }

    
    private GutterMark getGutterRenderer(final Point p) {
        int line = convertPointToLineNumber(p);
        if (line == -1) return null;
        List<GutterMark> renderers = myLineToGutterRenderers.get(line);
        if (renderers == null) {
            return null;
        }

        final GutterMark[] result = {null};
        processIconsRow(line, renderers, new LineGutterIconRendererProcessor() {
            @Override
            public void process(int x, int y, GutterMark renderer) {
                final int ex = convertX((int)p.getX());
                Icon icon = renderer.getIcon();
                // Do not check y to extend the area where users could click
                if (x <= ex && ex <= x + icon.getIconWidth()) {
                    result[0] = renderer;
                }
            }
        });

        return result[0];
    }

    
    private GutterIconRenderer getGutterRenderer(final MouseEvent e) {
        return (GutterIconRenderer)getGutterRenderer(e.getPoint());
    }

    public int convertX(int x) {
        if (!isMirrored()) return x;
        return getWidth() - x;
    }

    public void dispose() {
        for (TextAnnotationGutterProvider gutterProvider : myTextAnnotationGutters) {
            gutterProvider.gutterClosed();
        }
        myProviderToListener.clear();
    }
}