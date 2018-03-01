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
package com.gome.maven.openapi.diff.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.CaretModel;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.event.CaretEvent;
import com.gome.maven.openapi.editor.event.CaretListener;
import com.gome.maven.openapi.editor.markup.HighlighterLayer;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;

public class CurrentLineMarker implements CaretListener {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.diff.impl.CurrentLineMarker");
    private Editor myEditor;
    private RangeHighlighter myHighlighter = null;
    public static final int LAYER = HighlighterLayer.CARET_ROW + 1;

    public void attach(EditorSource editorSource) {
        if (myEditor != null) hide();
        myEditor = editorSource.getEditor();
        if (myEditor == null) return;
        final CaretModel caretModel = myEditor.getCaretModel();
        caretModel.addCaretListener(this);
        editorSource.addDisposable(new Disposable() {
            public void dispose() {
                caretModel.removeCaretListener(CurrentLineMarker.this);
            }
        });
    }

    public void set() {
        if (myEditor == null) return;
        hide();
        final int line = myEditor.getCaretModel().getLogicalPosition().line;
        myHighlighter = line < myEditor.getDocument().getLineCount() ? myEditor.getMarkupModel().addLineHighlighter(line, LAYER, null) : null;
    }

    private boolean isHiden() { return myHighlighter == null; }

    void hide() {
        if (myHighlighter != null) {
            LOG.assertTrue(myEditor != null);
            myHighlighter.dispose();
            myHighlighter = null;
        }
    }

    public void caretPositionChanged(CaretEvent e) {
        if (isHiden()) return;
        set();
    }

    @Override
    public void caretAdded(CaretEvent e) {

    }

    @Override
    public void caretRemoved(CaretEvent e) {

    }
}
