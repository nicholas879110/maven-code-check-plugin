/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.openapi.diff.impl.incrementalMerge.ui;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.DiffUtil;
import com.gome.maven.openapi.diff.impl.highlighting.FragmentSide;
import com.gome.maven.openapi.diff.impl.splitter.DividerPolygon;
import com.gome.maven.openapi.diff.impl.util.DiffDivider;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.ButtonlessScrollBarUI;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The container for an {@link Editor}, which is added then to {@link com.gome.maven.openapi.diff.impl.util.ThreePanels}.
 */
public class EditorPlace extends JComponent implements Disposable, ButtonlessScrollBarUI.ScrollbarRepaintCallback {
    private static final Logger LOG = Logger.getInstance(EditorPlace.class);

     private final MergePanel2.DiffEditorState myState;
     private final MergePanelColumn myColumn;
     private final MergePanel2 myMergePanel;
     private final List<EditorListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
     private EditorEx myEditor;

    public EditorPlace( MergePanel2.DiffEditorState state,  MergePanelColumn column,  MergePanel2 mergePanel) {
        myState = state;
        myColumn = column;
        myMergePanel = mergePanel;

        setLayout(new BorderLayout());
    }

    @Override
    public void call(Graphics g) {
        paintThis(g);
    }

    private void paintThis(Graphics g) {
        if (myEditor == null) return;
        // painting only above the central scrollbar, because painting on edge scrollbars is not needed, and there are error stripes
        if (myColumn != MergePanelColumn.BASE) return;

        int offset = DiffDivider.MERGE_SCROLL_DIVIDER_POLYGONS_OFFSET;
        ArrayList<DividerPolygon> polygons =
                DividerPolygon.createVisiblePolygons(myMergePanel.getSecondEditingSide(), FragmentSide.SIDE1, offset);
        for (DividerPolygon polygon : polygons) {
            int startY = polygon.getTopLeftY();
            int endY = polygon.getBottomLeftY();
            int height = endY - startY;

            if (height == 0) { // draw at least a one-pixel line (e.g. for insertion or deletion), as it is done in highlighters
                height = 1;
            }

            drawPolygon((Graphics2D)g, startY, height, polygon.getColor(), polygon.isApplied());
        }
    }

    private void drawPolygon( Graphics2D g, int startY, int height,  Color color, boolean applied) {
        int scrollbarWidth = myEditor.getScrollPane().getVerticalScrollBar().getWidth();
        int startX = 0;
        int endX = startX + scrollbarWidth - 1;

        int endY = startY + height;

        g.setColor(color);
        if (!applied) {
            if (height > 2) {
                g.fillRect(startX, startY, scrollbarWidth, height);

                Color framingColor = DiffUtil.getFramingColor(color);
                UIUtil.drawLine(g, startX, startY, endX, startY, null, framingColor);
                UIUtil.drawLine(g, startX, endY, endX, endY, null, framingColor);
            }
            else {
                DiffUtil.drawDoubleShadowedLine(g, startX, endX, startY, color);
            }
        }
        else {
            UIUtil.drawBoldDottedLine(g, startX, endX, startY, null, color, false);
            UIUtil.drawBoldDottedLine(g, startX, endX, endY, null, color, false);
        }
    }

    public void addNotify() {
        if (myEditor != null) {
            super.addNotify();
            return;
        }
        createEditor();
        super.addNotify();
        revalidate();
    }

    private void createEditor() {
        LOG.assertTrue(myEditor == null);
        myEditor = myState.createEditor();
        if (myEditor == null) return;
        add(myEditor.getComponent(), BorderLayout.CENTER);
        myEditor.registerScrollBarRepaintCallback(this);

        repaint();
        fireEditorCreated();
    }

    public void addListener(EditorListener listener) {
        myListeners.add(listener);
    }

    private void fireEditorCreated() {
        for (EditorListener listener : myListeners) {
            listener.onEditorCreated(this);
        }
    }

    private void fireEditorReleased(Editor releasedEditor) {
        for (EditorListener listener : myListeners) {
            listener.onEditorReleased(releasedEditor);
        }
    }

    public void removeNotify() {
        removeEditor();
        super.removeNotify();
    }

    private void removeEditor() {
        if (myEditor != null) {
            myEditor.registerScrollBarRepaintCallback(null);
            Editor releasedEditor = myEditor;
            remove(myEditor.getComponent());
            getEditorFactory().releaseEditor(myEditor);
            myEditor = null;
            fireEditorReleased(releasedEditor);
        }
    }

    
    public Editor getEditor() {
        return myEditor;
    }

    public void setDocument( Document document) {
        if (document == getDocument()) return;
        removeEditor();
        myState.setDocument(document);
        createEditor();
    }

    private Document getDocument() {
        return myState.getDocument();
    }

    
    public MergePanel2.DiffEditorState getState() {
        return myState;
    }

    public interface EditorListener {
        void onEditorCreated(EditorPlace place);

        void onEditorReleased(Editor releasedEditor);
    }

    private static EditorFactory getEditorFactory() {
        return EditorFactory.getInstance();
    }

    
    public JComponent getContentComponent() {
        return myEditor == null ? null : myEditor.getContentComponent();
    }

    public void dispose() {
        removeEditor();
    }
}
