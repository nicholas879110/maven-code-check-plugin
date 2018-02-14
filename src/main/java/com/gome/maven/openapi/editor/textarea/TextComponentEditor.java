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
package com.gome.maven.openapi.editor.textarea;

import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.event.EditorMouseEventArea;
import com.gome.maven.openapi.editor.event.EditorMouseListener;
import com.gome.maven.openapi.editor.event.EditorMouseMotionListener;
import com.gome.maven.openapi.editor.impl.EmptyIndentsModel;
import com.gome.maven.openapi.editor.impl.SettingsImpl;
import com.gome.maven.openapi.editor.markup.MarkupModel;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.UserDataHolderBase;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author yole
 */
public class TextComponentEditor extends UserDataHolderBase implements Editor {
    private final Project myProject;
    private final JTextComponent myTextComponent;
    private final TextComponentDocument myDocument;
    private final TextComponentCaretModel myCaretModel;
    private final TextComponentSelectionModel mySelectionModel;
    private final TextComponentScrollingModel myScrollingModel;
    private final TextComponentSoftWrapModel mySoftWrapModel;
    private final TextComponentFoldingModel myFoldingModel;
    private EditorSettings mySettings;

    public TextComponentEditor(final Project project,  JTextComponent textComponent) {
        myProject = project;
        myTextComponent = textComponent;
        if (textComponent instanceof JTextArea) {
            myDocument = new TextAreaDocument((JTextArea) textComponent);
        }
        else {
            myDocument = new TextComponentDocument(textComponent);
        }
        myCaretModel = new TextComponentCaretModel(textComponent, this);
        mySelectionModel = new TextComponentSelectionModel(textComponent, this);
        myScrollingModel = new TextComponentScrollingModel(textComponent);
        mySoftWrapModel = new TextComponentSoftWrapModel();
        myFoldingModel = new TextComponentFoldingModel();
    }

    @Override
    
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public boolean isViewer() {
        return !myTextComponent.isEditable();
    }

    @Override
    
    public JComponent getComponent() {
        return myTextComponent;
    }

    @Override
    
    public JComponent getContentComponent() {
        return myTextComponent;
    }

    @Override
    public void setBorder( Border border) {
    }

    @Override
    public Insets getInsets() {
        return new Insets(0,0,0,0);
    }

    @Override
    
    public TextComponentSelectionModel getSelectionModel() {
        return mySelectionModel;
    }

    @Override
    
    public MarkupModel getMarkupModel() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    
    public FoldingModel getFoldingModel() {
        return myFoldingModel;
    }

    @Override
    
    public ScrollingModel getScrollingModel() {
        return myScrollingModel;
    }

    @Override
    
    public CaretModel getCaretModel() {
        return myCaretModel;
    }

    @Override
    
    public SoftWrapModel getSoftWrapModel() {
        return mySoftWrapModel;
    }

    @Override
    
    public EditorSettings getSettings() {
        if (mySettings == null) {
            mySettings = new SettingsImpl();
        }
        return mySettings;
    }

    @Override
    
    public EditorColorsScheme getColorsScheme() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getLineHeight() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    
    public Point logicalPositionToXY( final LogicalPosition pos) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int logicalPositionToOffset( final LogicalPosition pos) {
        if (pos.line >= myDocument.getLineCount()) {
            return myDocument.getTextLength();
        }
        return myDocument.getLineStartOffset(pos.line) + pos.column;
    }

    @Override
    
    public VisualPosition logicalToVisualPosition( final LogicalPosition logicalPos) {
        return new VisualPosition(logicalPos.line, logicalPos.column);
    }

    @Override
    
    public Point visualPositionToXY( final VisualPosition visible) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    
    public LogicalPosition visualToLogicalPosition( final VisualPosition visiblePos) {
        return new LogicalPosition(visiblePos.line, visiblePos.column);
    }

    @Override
    
    public LogicalPosition offsetToLogicalPosition(final int offset) {
        int line = myDocument.getLineNumber(offset);
        final int lineStartOffset = myDocument.getLineStartOffset(line);
        return new LogicalPosition(line, offset - lineStartOffset);
    }

    @Override
    
    public VisualPosition offsetToVisualPosition(final int offset) {
        int line = myDocument.getLineNumber(offset);
        final int lineStartOffset = myDocument.getLineStartOffset(line);
        return new VisualPosition(line, offset - lineStartOffset);
    }

    @Override
    
    public LogicalPosition xyToLogicalPosition( final Point p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    
    public VisualPosition xyToVisualPosition( final Point p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addEditorMouseListener( final EditorMouseListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeEditorMouseListener( final EditorMouseListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addEditorMouseMotionListener( final EditorMouseMotionListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeEditorMouseMotionListener( final EditorMouseMotionListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean isInsertMode() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isColumnMode() {
        return false;
    }

    @Override
    public boolean isOneLineMode() {
        return !(myTextComponent instanceof JTextArea);
    }

    @Override
    
    public EditorGutter getGutter() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    
    public EditorMouseEventArea getMouseEventArea( final MouseEvent e) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setHeaderComponent( final JComponent header) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean hasHeaderComponent() {
        return false;
    }

    @Override
    
    public JComponent getHeaderComponent() {
        return null;
    }

    
    @Override
    public IndentsModel getIndentsModel() {
        return new EmptyIndentsModel();
    }
}
