package com.gome.maven.codeInsight.intention.impl.config;

import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.event.EditorMouseEventArea;
import com.gome.maven.openapi.editor.event.EditorMouseListener;
import com.gome.maven.openapi.editor.event.EditorMouseMotionListener;
import com.gome.maven.openapi.editor.markup.MarkupModel;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.psi.PsiFile;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author Dmitry Avdeev
 */
class LazyEditor extends UserDataHolderBase implements Editor {

    private final PsiFile myFile;
    private Editor myEditor;

    public LazyEditor(PsiFile file) {
        myFile = file;
    }

    private Editor getEditor() {
        if (myEditor == null) {
            final Project project = myFile.getProject();
            myEditor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, myFile.getVirtualFile(), 0), false);
            assert myEditor != null;
        }
        return myEditor;
    }

    @Override
    
    public Document getDocument() {
        return getEditor().getDocument();
    }

    @Override
    public boolean isViewer() {
        return getEditor().isViewer();
    }

    @Override
    
    public JComponent getComponent() {
        return getEditor().getComponent();
    }

    @Override
    
    public JComponent getContentComponent() {
        return getEditor().getContentComponent();
    }

    @Override
    public void setBorder( Border border) {
        getEditor().setBorder(border);
    }

    @Override
    public Insets getInsets() {
        return getEditor().getInsets();
    }

    @Override
    
    public SelectionModel getSelectionModel() {
        return getEditor().getSelectionModel();
    }

    @Override
    
    public MarkupModel getMarkupModel() {
        return getEditor().getMarkupModel();
    }

    @Override
    
    public FoldingModel getFoldingModel() {
        return getEditor().getFoldingModel();
    }

    @Override
    
    public ScrollingModel getScrollingModel() {
        return getEditor().getScrollingModel();
    }

    @Override
    
    public CaretModel getCaretModel() {
        return getEditor().getCaretModel();
    }

    @Override
    
    public SoftWrapModel getSoftWrapModel() {
        return getEditor().getSoftWrapModel();
    }

    @Override
    
    public EditorSettings getSettings() {
        return getEditor().getSettings();
    }

    @Override
    
    public EditorColorsScheme getColorsScheme() {
        return getEditor().getColorsScheme();
    }

    @Override
    public int getLineHeight() {
        return getEditor().getLineHeight();
    }

    @Override
    
    public Point logicalPositionToXY( final LogicalPosition pos) {
        return getEditor().logicalPositionToXY(pos);
    }

    @Override
    public int logicalPositionToOffset( final LogicalPosition pos) {
        return getEditor().logicalPositionToOffset(pos);
    }

    @Override
    
    public VisualPosition logicalToVisualPosition( final LogicalPosition logicalPos) {
        return getEditor().logicalToVisualPosition(logicalPos);
    }

    @Override
    
    public Point visualPositionToXY( final VisualPosition visible) {
        return getEditor().visualPositionToXY(visible);
    }

    @Override
    
    public LogicalPosition visualToLogicalPosition( final VisualPosition visiblePos) {
        return getEditor().visualToLogicalPosition(visiblePos);
    }

    @Override
    
    public LogicalPosition offsetToLogicalPosition(final int offset) {
        return getEditor().offsetToLogicalPosition(offset);
    }

    @Override
    
    public VisualPosition offsetToVisualPosition(final int offset) {
        return getEditor().offsetToVisualPosition(offset);
    }

    @Override
    
    public LogicalPosition xyToLogicalPosition( final Point p) {
        return getEditor().xyToLogicalPosition(p);
    }

    @Override
    
    public VisualPosition xyToVisualPosition( final Point p) {
        return getEditor().xyToVisualPosition(p);
    }

    @Override
    public void addEditorMouseListener( final EditorMouseListener listener) {
        getEditor().addEditorMouseListener(listener);
    }

    @Override
    public void removeEditorMouseListener( final EditorMouseListener listener) {
        getEditor().removeEditorMouseListener(listener);
    }

    @Override
    public void addEditorMouseMotionListener( final EditorMouseMotionListener listener) {
        getEditor().addEditorMouseMotionListener(listener);
    }

    @Override
    public void removeEditorMouseMotionListener( final EditorMouseMotionListener listener) {
        getEditor().removeEditorMouseMotionListener(listener);
    }

    @Override
    public boolean isDisposed() {
        return getEditor().isDisposed();
    }

    @Override
    
    public Project getProject() {
        return getEditor().getProject();
    }

    @Override
    public boolean isInsertMode() {
        return getEditor().isInsertMode();
    }

    @Override
    public boolean isColumnMode() {
        return getEditor().isColumnMode();
    }

    @Override
    public boolean isOneLineMode() {
        return getEditor().isOneLineMode();
    }

    @Override
    
    public EditorGutter getGutter() {
        return getEditor().getGutter();
    }

    @Override
    
    public EditorMouseEventArea getMouseEventArea( final MouseEvent e) {
        return getEditor().getMouseEventArea(e);
    }

    @Override
    public void setHeaderComponent( final JComponent header) {
        getEditor().setHeaderComponent(header);
    }

    @Override
    public boolean hasHeaderComponent() {
        return getEditor().hasHeaderComponent();
    }

    @Override
    
    public JComponent getHeaderComponent() {
        return getEditor().getHeaderComponent();
    }

    @Override
    
    public IndentsModel getIndentsModel() {
        return getEditor().getIndentsModel();
    }
}
