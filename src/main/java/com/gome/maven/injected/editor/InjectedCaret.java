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
package com.gome.maven.injected.editor;

import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.TextRange;

public class InjectedCaret implements Caret {
    private final EditorWindow myEditorWindow;
    final Caret myDelegate;

    InjectedCaret(EditorWindow window, Caret delegate) {
        myEditorWindow = window;
        myDelegate = delegate;
    }

    
    @Override
    public Editor getEditor() {
        return myEditorWindow;
    }

    
    @Override
    public CaretModel getCaretModel() {
        return myEditorWindow.getCaretModel();
    }

    public Caret getDelegate() {
        return myDelegate;
    }

    @Override
    public boolean isValid() {
        return myDelegate.isValid();
    }

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {
        myDelegate.moveCaretRelatively(columnShift, lineShift, withSelection, scrollToCaret);
    }

    @Override
    public void moveToLogicalPosition( LogicalPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(pos);
        myDelegate.moveToLogicalPosition(hostPos);
    }

    @Override
    public void moveToVisualPosition( VisualPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        myDelegate.moveToLogicalPosition(hostPos);
    }

    @Override
    public void moveToOffset(int offset) {
        moveToOffset(offset, false);
    }

    @Override
    public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
        int hostOffset = myEditorWindow.getDocument().injectedToHost(offset);
        myDelegate.moveToOffset(hostOffset, locateBeforeSoftWrap);
    }

    @Override
    public boolean isUpToDate() {
        return myDelegate.isUpToDate();
    }

    
    @Override
    public LogicalPosition getLogicalPosition() {
        LogicalPosition hostPos = myDelegate.getLogicalPosition();
        return myEditorWindow.hostToInjected(hostPos);
    }

    
    @Override
    public VisualPosition getVisualPosition() {
        LogicalPosition logicalPosition = getLogicalPosition();
        return myEditorWindow.logicalToVisualPosition(logicalPosition);
    }

    @Override
    public int getOffset() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getOffset());
    }

    @Override
    public int getVisualLineStart() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineStart());
    }

    @Override
    public int getVisualLineEnd() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineEnd());
    }

    @Override
    public int getSelectionStart() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getSelectionStart());
    }

    
    @Override
    public VisualPosition getSelectionStartPosition() {
        return myDelegate.getSelectionStartPosition();
    }

    @Override
    public int getSelectionEnd() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getSelectionEnd());
    }

    
    @Override
    public VisualPosition getSelectionEndPosition() {
        return myDelegate.getSelectionEndPosition();
    }

    
    @Override
    public String getSelectedText() {
        return myDelegate.getSelectedText();
    }

    @Override
    public int getLeadSelectionOffset() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getLeadSelectionOffset());
    }

    
    @Override
    public VisualPosition getLeadSelectionPosition() {
        return myDelegate.getLeadSelectionPosition();
    }

    @Override
    public boolean hasSelection() {
        return myDelegate.hasSelection();
    }

    @Override
    public void setSelection(int startOffset, int endOffset) {
        TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
        myDelegate.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset());
    }

    @Override
    public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {
        TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
        myDelegate.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset(), updateSystemSelection);
    }

    @Override
    public void setSelection(int startOffset,  VisualPosition endPosition, int endOffset) {
        TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
        myDelegate.setSelection(hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
    }

    @Override
    public void setSelection( VisualPosition startPosition, int startOffset,  VisualPosition endPosition, int endOffset) {
        TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
        myDelegate.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
    }

    @Override
    public void setSelection( VisualPosition startPosition, int startOffset,  VisualPosition endPosition, int endOffset, boolean updateSystemSelection) {
        TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
        myDelegate.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset(), updateSystemSelection);
    }

    @Override
    public void removeSelection() {
        myDelegate.removeSelection();
    }

    @Override
    public void selectLineAtCaret() {
        myDelegate.selectLineAtCaret();
    }

    @Override
    public void selectWordAtCaret(boolean honorCamelWordsSettings) {
        myDelegate.selectWordAtCaret(honorCamelWordsSettings);
    }

    
    @Override
    public Caret clone(boolean above) {
        Caret clone = myDelegate.clone(above);
        return clone == null ? null : new InjectedCaret(myEditorWindow, clone);
    }

    @Override
    public void dispose() {
        //noinspection SSBasedInspection
        myDelegate.dispose();
    }

    
    @Override
    public <T> T putUserDataIfAbsent( Key<T> key,  T value) {
        return myDelegate.putUserDataIfAbsent(key, value);
    }

    @Override
    public <T> boolean replace( Key<T> key,  T oldValue,  T newValue) {
        return myDelegate.replace(key, oldValue, newValue);
    }

    
    @Override
    public <T> T getUserData( Key<T> key) {
        return myDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData( Key<T> key,  T value) {
        myDelegate.putUserData(key, value);
    }
}
