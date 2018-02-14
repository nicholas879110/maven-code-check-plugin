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
import com.gome.maven.openapi.editor.event.CaretListener;
import com.gome.maven.openapi.editor.markup.TextAttributes;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class TextComponentCaretModel implements CaretModel {
    private final JTextComponent myTextComponent;
    private final TextComponentEditor myEditor;
    private final Caret myCaret;

    public TextComponentCaretModel( JTextComponent textComponent,  TextComponentEditor editor) {
        myTextComponent = textComponent;
        myEditor = editor;
        myCaret = new TextComponentCaret(editor);
    }

    @Override
    public void moveCaretRelatively(final int columnShift,
                                    final int lineShift,
                                    final boolean withSelection, final boolean blockSelection, final boolean scrollToCaret) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void moveToLogicalPosition( final LogicalPosition pos) {
        moveToOffset(myEditor.logicalPositionToOffset(pos), false);
    }

    @Override
    public void moveToVisualPosition( final VisualPosition pos) {
        moveToLogicalPosition(myEditor.visualToLogicalPosition(pos));
    }

    @Override
    public void moveToOffset(int offset) {
        moveToOffset(offset, false);
    }

    @Override
    public void moveToOffset(final int offset, boolean locateBeforeSoftWrap) {
        myTextComponent.setCaretPosition(Math.min(offset, myTextComponent.getText().length()));
    }

    @Override
    public boolean isUpToDate() {
        return true;
    }

    @Override
    
    public LogicalPosition getLogicalPosition() {
        int caretPos = myTextComponent.getCaretPosition();
        int line;
        int lineStart;
        if (myTextComponent instanceof JTextArea) {
            final JTextArea textArea = (JTextArea)myTextComponent;
            try {
                line = textArea.getLineOfOffset(caretPos);
                lineStart = textArea.getLineStartOffset(line);
            }
            catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            line = 0;
            lineStart = 0;
        }
        return new LogicalPosition(line, caretPos - lineStart);
    }

    @Override
    
    public VisualPosition getVisualPosition() {
        LogicalPosition pos = getLogicalPosition();
        return new VisualPosition(pos.line, pos.column);
    }

    @Override
    public int getOffset() {
        return myTextComponent.getCaretPosition();
    }

    @Override
    public void addCaretListener( final CaretListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeCaretListener( final CaretListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getVisualLineStart() {
        return 0;
    }

    @Override
    public int getVisualLineEnd() {
        return 0;
    }

    @Override
    public TextAttributes getTextAttributes() {
        return null;
    }

    @Override
    public boolean supportsMultipleCarets() {
        return false;
    }

    
    @Override
    public Caret getCurrentCaret() {
        return myCaret;
    }

    
    @Override
    public Caret getPrimaryCaret() {
        return myCaret;
    }

    @Override
    public int getCaretCount() {
        return 1;
    }

    
    @Override
    public List<Caret> getAllCarets() {
        return Collections.singletonList(myCaret);
    }

    
    @Override
    public Caret getCaretAt( VisualPosition pos) {
        return myCaret.getVisualPosition().equals(pos) ? myCaret : null;
    }

    
    @Override
    public Caret addCaret( VisualPosition pos) {
        return null;
    }

    @Override
    public boolean removeCaret( Caret caret) {
        return false;
    }

    @Override
    public void removeSecondaryCarets() {
    }

    @Override
    public void setCaretsAndSelections( List<CaretState> caretStates) {
        throw new UnsupportedOperationException("Multiple carets are not supported");
    }

    @Override
    public void setCaretsAndSelections( List<CaretState> caretStates, boolean updateSystemSelection) {
        throw new UnsupportedOperationException("Multiple carets are not supported");
    }

    
    @Override
    public List<CaretState> getCaretsAndSelections() {
        throw new UnsupportedOperationException("Multiple carets are not supported");
    }

    @Override
    public void runForEachCaret( CaretAction action) {
        action.perform(myCaret);
    }

    @Override
    public void runForEachCaret( CaretAction action, boolean reverseOrder) {
        action.perform(myCaret);
    }

    @Override
    public void runBatchCaretOperation( Runnable runnable) {
        runnable.run();
    }
}
