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
package com.gome.maven.openapi.editor.markup;

import com.gome.maven.openapi.editor.RangeMarker;

import java.awt.*;

/**
 * Represents a range of text in the document which has specific markup (special text attributes,
 * line marker, gutter icon, error stripe marker or line separator).
 *
 * @see MarkupModel#addRangeHighlighter(int, int, int, TextAttributes, HighlighterTargetArea)
 * @see com.gome.maven.lang.annotation.Annotation
 */
public interface RangeHighlighter extends RangeMarker {
    RangeHighlighter[] EMPTY_ARRAY = new RangeHighlighter[0];
    /**
     * Returns the relative priority of the highlighter (higher priority highlighters can override
     * lower priority ones; layer number values for standard IDEA highlighters are given in
     * {@link HighlighterLayer} class).
     *
     * @return the highlighter priority.
     */
    int getLayer();

    /**
     * Returns the value indicating whether the highlighter affects a range of text or a sequence of
     * of entire lines in the specified range.
     *
     * @return the highlighter target area.
     */
    
    HighlighterTargetArea getTargetArea();

    /**
     * Returns the text attributes used for highlighting.
     *
     * @return the attributes to use for highlighting, or null if the highlighter
     * does not modify the text attributes.
     */
    
    TextAttributes getTextAttributes();

    /**
     * Returns the renderer used for drawing line markers in the area covered by the
     * highlighter, and optionally for processing mouse events over the markers.
     * Line markers are drawn over the folding area and are used, for example,
     * to highlight modified lines in files under source control.
     *
     * @return the renderer instance, or null if the highlighter does not add any line markers.
     * @see ActiveGutterRenderer
     */
    
    LineMarkerRenderer getLineMarkerRenderer();

    /**
     * Sets the renderer used for drawing line markers in the area covered by the
     * highlighter, and optionally for processing mouse events over the markers.
     * Line markers are drawn over the folding area and are used, for example,
     * to highlight modified lines in files under source control.
     *
     * @param renderer the renderer instance, or null if the highlighter does not add any line markers.
     * @see ActiveGutterRenderer
     */
    void setLineMarkerRenderer( LineMarkerRenderer renderer);


    
    CustomHighlighterRenderer getCustomRenderer();

    void setCustomRenderer(CustomHighlighterRenderer renderer);
    /**
     * Returns the renderer used for drawing gutter icons in the area covered by the
     * highlighter. Gutter icons are drawn to the left of the folding area and can be used,
     * for example, to mark implemented or overridden methods.
     *
     * @return the renderer instance, or null if the highlighter does not add any gutter icons.
     */
    
    GutterIconRenderer getGutterIconRenderer();

    /**
     * Sets the renderer used for drawing gutter icons in the area covered by the
     * highlighter. Gutter icons are drawn to the left of the folding area and can be used,
     * for example, to mark implemented or overridden methods.
     *
     * @param renderer the renderer instance, or null if the highlighter does not add any gutter icons.
     */
    void setGutterIconRenderer( GutterIconRenderer renderer);

    /**
     * Returns the color of the marker drawn in the error stripe in the area covered by the highlighter.
     *
     * @return the error stripe marker color, or null if the highlighter does not add any
     * error stripe markers.
     */
    
    Color getErrorStripeMarkColor();

    /**
     * Sets the color of the marker drawn in the error stripe in the area covered by the highlighter.
     *
     * @param color the error stripe marker color, or null if the highlighter does not add any
     * error stripe markers.
     */
    void setErrorStripeMarkColor( Color color);

    /**
     * Returns the object whose <code>toString()</code> method is called to get the text of the tooltip
     * for the error stripe marker added by the highlighter.
     *
     * @return the error stripe tooltip objects, or null if the highlighter does not add any error
     * stripe markers or the marker has no tooltip.
     */
    
    Object getErrorStripeTooltip();

    /**
     * Sets the object whose <code>toString()</code> method is called to get the text of the tooltip
     * for the error stripe marker added by the highlighter.
     *
     * @param tooltipObject the error stripe tooltip objects, or null if the highlighter does not
     * add any error stripe markers or the marker has no tooltip.
     */
    void setErrorStripeTooltip( Object tooltipObject);

    /**
     * Returns the value indicating whether the error stripe marker has reduced width (like
     * the markers used to highlight changed lines).
     *
     * @return true if the marker has reduced width, false otherwise.
     */
    boolean isThinErrorStripeMark();

    /**
     * Sets the value indicating whether the error stripe marker has reduced width (like
     * the markers used to highlight changed lines).
     *
     * @param value true if the marker has reduced width, false otherwise.
     */
    void setThinErrorStripeMark(boolean value);

    /**
     * Returns the color of the separator drawn above or below the range covered by
     * the highlighter.
     *
     * @return the separator color, or null if the highlighter does not add a line separator.
     */
    
    Color getLineSeparatorColor();

    /**
     * Sets the color of the separator drawn above or below the range covered by
     * the highlighter.
     *
     * @param color the separator color, or null if the highlighter does not add a line separator.
     */
    void setLineSeparatorColor( Color color);

    void setLineSeparatorRenderer(LineSeparatorRenderer renderer);

    LineSeparatorRenderer getLineSeparatorRenderer();

    /**
     * Returns the placement of the separator drawn by the range highlighter
     * (above or below the range).
     *
     * @return the separator placement, or null if the highlighter does not add a line separator.
     */
    
    SeparatorPlacement getLineSeparatorPlacement();

    /**
     * Sets the placement of the separator drawn by the range highlighter
     * (above or below the range).
     *
     * @param placement the separator placement, or null if the highlighter does not add a line separator.
     */
    void setLineSeparatorPlacement( SeparatorPlacement placement);

    /**
     * Sets the filter which can disable the highlighter in specific editor instances.
     *
     * @param filter the filter controlling the highlighter availability, or MarkupEditorFilter.EMPTY if
     * highlighter is available in all editors.
     */
    void setEditorFilter( MarkupEditorFilter filter);

    /**
     * Gets the filter which can disable the highlighter in specific editor instances.
     *
     * @return the filter controlling the highlighter availability. Default availability is controlled by MarkupEditorFilter.EMPTY
     */
    
    MarkupEditorFilter getEditorFilter();
}
