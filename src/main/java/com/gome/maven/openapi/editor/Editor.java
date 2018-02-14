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
package com.gome.maven.openapi.editor;

import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.event.EditorMouseEventArea;
import com.gome.maven.openapi.editor.event.EditorMouseListener;
import com.gome.maven.openapi.editor.event.EditorMouseMotionListener;
import com.gome.maven.openapi.editor.markup.MarkupModel;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.UserDataHolder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Represents an instance of the IDEA text editor.
 *
 * @see EditorFactory#createEditor(Document)
 * @see EditorFactory#createViewer(Document)
 */
public interface Editor extends UserDataHolder {
    Editor[] EMPTY_ARRAY = new Editor[0];

    /**
     * Returns the document edited or viewed in the editor.
     *
     * @return the document instance.
     */
    
    Document getDocument();

    /**
     * Returns the value indicating whether the editor operates in viewer mode, with
     * all modification actions disabled.
     *
     * @return true if the editor works as a viewer, false otherwise
     */
    boolean isViewer();

    /**
     * Returns the component for the entire editor including the scrollbars, error stripe, gutter
     * and other decorations. The component can be used, for example, for converting logical to
     * screen coordinates.
     *
     * @return the component instance.
     */
    
    JComponent getComponent();

    /**
     * Returns the component for the content area of the editor (the area displaying the document text).
     * The component can be used, for example, for converting logical to screen coordinates.
     * The instance is implementing {@link DataProvider}
     *
     * @return the component instance.
     */
    
    JComponent getContentComponent();

    void setBorder( Border border);

    Insets getInsets();

    /**
     * Returns the selection model for the editor, which can be used to select ranges of text in
     * the document and retrieve information about the selection.
     * <p>
     * To query or change selections for specific carets, {@link CaretModel} interface should be used.
     *
     * @see #getCaretModel()
     *
     * @return the selection model instance.
     */
    
    SelectionModel getSelectionModel();

    /**
     * Returns the markup model for the editor. This model contains editor-specific highlighters
     * (for example, highlighters added by "Highlight usages in file"), which are painted in addition
     * to the highlighters contained in the markup model for the document.
     *
     * @return the markup model instance.
     * @see com.gome.maven.openapi.editor.impl.DocumentMarkupModel#forDocument(com.gome.maven.openapi.editor.Document, com.gome.maven.openapi.project.Project, boolean)
     */
    
    MarkupModel getMarkupModel();

    /**
     * Returns the folding model for the document, which can be used to add, remove, expand
     * or collapse folded regions in the document.
     *
     * @return the folding model instance.
     */
    
    FoldingModel getFoldingModel();

    /**
     * Returns the scrolling model for the document, which can be used to scroll the document
     * and retrieve information about the current position of the scrollbars.
     *
     * @return the scrolling model instance.
     */
    
    ScrollingModel getScrollingModel();

    /**
     * Returns the caret model for the document, which can be used to add and remove carets to the editor, as well as to query and update 
     * carets' and corresponding selections' positions.
     *
     * @return the caret model instance.
     */
    
    CaretModel getCaretModel();

    /**
     * Returns the soft wrap model for the document, which can be used to get information about soft wraps registered
     * for the editor document at the moment and provides basic management functions for them.
     *
     * @return the soft wrap model instance
     */
    
    SoftWrapModel getSoftWrapModel();

    /**
     * Returns the editor settings for this editor instance. Changes to these settings affect
     * only the current editor instance.
     *
     * @return the settings instance.
     */
    
    EditorSettings getSettings();

    /**
     * Returns the editor color scheme for this editor instance. Changes to the scheme affect
     * only the current editor instance.
     *
     * @return the color scheme instance.
     */
    
    EditorColorsScheme getColorsScheme();

    /**
     * Returns the height of a single line of text in the current editor font.
     *
     * @return the line height in pixels.
     */
    int getLineHeight();

    /**
     * Maps a logical position in the editor to pixel coordinates.
     *
     * @param pos the logical position.
     * @return the coordinates relative to the top left corner of the {@link #getContentComponent() content component}.
     */
    
    Point logicalPositionToXY( LogicalPosition pos);

    /**
     * Maps a logical position in the editor to the offset in the document.
     *
     * @param pos the logical position.
     * @return the corresponding offset in the document.
     */
    int logicalPositionToOffset( LogicalPosition pos);

    /**
     * Maps a logical position in the editor (the line and column ignoring folding) to
     * a visual position (with folded lines and columns not included in the line and column count).
     *
     * @param logicalPos the logical position.
     * @return the corresponding visual position.
     */
    
    VisualPosition logicalToVisualPosition( LogicalPosition logicalPos);

    /**
     * Maps a visual position in the editor to pixel coordinates.
     *
     * @param visible the visual position.
     * @return the coordinates relative to the top left corner of the {@link #getContentComponent() content component}.
     */
    
    Point visualPositionToXY( VisualPosition visible);

    /**
     * Maps a visual position in the editor (with folded lines and columns not included in the line and column count) to
     * a logical position (the line and column ignoring folding).
     *
     * @param visiblePos the visual position.
     * @return the corresponding logical position.
     */
    
    LogicalPosition visualToLogicalPosition( VisualPosition visiblePos);

    /**
     * Maps an offset in the document to a logical position.
     *
     * @param offset the offset in the document.
     * @return the corresponding logical position.
     */
    
    LogicalPosition offsetToLogicalPosition(int offset);

    /**
     * Maps an offset in the document to a visual position.
     *
     * @param offset the offset in the document.
     * @return the corresponding visual position.
     */
    
    VisualPosition offsetToVisualPosition(int offset);

    /**
     * Maps the pixel coordinates in the editor to a logical position.
     *
     * @param p the coordinates relative to the top left corner of the {@link #getContentComponent() content component}.
     * @return the corresponding logical position.
     */
    
    LogicalPosition xyToLogicalPosition( Point p);

    /**
     * Maps the pixel coordinates in the editor to a visual position.
     *
     * @param p the coordinates relative to the top left corner of the {@link #getContentComponent() content component}.
     * @return the corresponding visual position.
     */
    
    VisualPosition xyToVisualPosition( Point p);

    /**
     * Adds a listener for receiving notifications about mouse clicks in the editor and
     * the mouse entering/exiting the editor.
     *
     * @param listener the listener instance.
     */
    void addEditorMouseListener( EditorMouseListener listener);

    /**
     * Removes a listener for receiving notifications about mouse clicks in the editor and
     * the mouse entering/exiting the editor.
     *
     * @param listener the listener instance.
     */
    void removeEditorMouseListener( EditorMouseListener listener);

    /**
     * Adds a listener for receiving notifications about mouse movement in the editor.
     *
     * @param listener the listener instance.
     */
    void addEditorMouseMotionListener( EditorMouseMotionListener listener);

    /**
     * Removes a listener for receiving notifications about mouse movement in the editor.
     *
     * @param listener the listener instance.
     */
    void removeEditorMouseMotionListener( EditorMouseMotionListener listener);

    /**
     * Checks if this editor instance has been disposed.
     *
     * @return true if the editor has been disposed, false otherwise.
     */
    boolean isDisposed();

    /**
     * Returns the project to which the editor is related.
     *
     * @return the project instance, or null if the editor is not related to any project.
     */
    
    Project getProject();

    /**
     * Returns the insert/overwrite mode for the editor.
     *
     * @return true if the editor is in insert mode, false otherwise.
     */
    boolean isInsertMode();

    /**
     * Returns the block selection mode for the editor.
     *
     * @return true if the editor uses column selection, false if it uses regular selection.
     */
    boolean isColumnMode();

    /**
     * Checks if the current editor instance is a one-line editor (used in a dialog control, for example).
     *
     * @return true if the editor is one-line, false otherwise.
     */
    boolean isOneLineMode();

    /**
     * Returns the gutter instance for the editor, which can be used to draw custom text annotations
     * in the gutter.
     *
     * @return the gutter instance.
     */
    
    EditorGutter getGutter();

    /**
     * Returns the editor area (text, gutter, folding outline and so on) in which the specified
     * mouse event occurred.
     *
     * @param e the mouse event for which the area is requested.
     * @return the editor area, or null if the event occurred over an unknown area.
     */
    
    EditorMouseEventArea getMouseEventArea( MouseEvent e);

    /**
     * Set up a header component for this text editor. Please note this is used for textual find feature so your component will most
     * probably will be reset once the user presses Ctrl+F.
     *
     * @param header a component to setup as header for this text editor or <code>null</code> to remove one.
     */
    void setHeaderComponent( JComponent header);

    /**
     * @return <code>true</code> if this editor has active header component set up by {@link #setHeaderComponent(JComponent)}
     */
    boolean hasHeaderComponent();

    /**
     * @return a component set by {@link #setHeaderComponent(JComponent)} or <code>null</code> if no header currently installed.
     */
    
    JComponent getHeaderComponent();

    
    IndentsModel getIndentsModel();
}
