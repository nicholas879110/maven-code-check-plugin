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
import com.gome.maven.openapi.editor.event.CaretAdapter;
import com.gome.maven.openapi.editor.event.CaretEvent;
import com.gome.maven.openapi.editor.event.CaretListener;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.markup.TextAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Alexey
 */
public class CaretModelWindow implements CaretModel {
    private final CaretModel myDelegate;
    private final EditorEx myHostEditor;
    private final EditorWindow myEditorWindow;
    private final Map<Caret, InjectedCaret> myInjectedCaretMap = new WeakHashMap<Caret, InjectedCaret>();

    public CaretModelWindow(CaretModel delegate, EditorWindow editorWindow) {
        myDelegate = delegate;
        myHostEditor = (EditorEx)editorWindow.getDelegate();
        myEditorWindow = editorWindow;
    }

    @Override
    public void moveCaretRelatively(final int columnShift,
                                    final int lineShift,
                                    final boolean withSelection,
                                    final boolean blockSelection,
                                    final boolean scrollToCaret) {
        myDelegate.moveCaretRelatively(columnShift, lineShift, withSelection, blockSelection, scrollToCaret);
    }

    @Override
    public void moveToLogicalPosition( final LogicalPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(pos);
        myDelegate.moveToLogicalPosition(hostPos);
    }

    @Override
    public void moveToVisualPosition( final VisualPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        myDelegate.moveToLogicalPosition(hostPos);
    }

    @Override
    public void moveToOffset(int offset) {
        moveToOffset(offset, false);
    }

    @Override
    public void moveToOffset(final int offset, boolean locateBeforeSoftWrap) {
        int hostOffset = myEditorWindow.getDocument().injectedToHost(offset);
        myDelegate.moveToOffset(hostOffset, locateBeforeSoftWrap);
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
    public boolean isUpToDate() {
        return myDelegate.isUpToDate();
    }

    private final ListenerWrapperMap<CaretListener> myCaretListeners = new ListenerWrapperMap<CaretListener>();
    @Override
    public void addCaretListener( final CaretListener listener) {
        CaretListener wrapper = new CaretAdapter() {
            @Override
            public void caretPositionChanged(CaretEvent e) {
                if (!myEditorWindow.getDocument().isValid()) return; // injected document can be destroyed by now
                CaretEvent event = new CaretEvent(myEditorWindow, createInjectedCaret(e.getCaret()),
                        myEditorWindow.hostToInjected(e.getOldPosition()),
                        myEditorWindow.hostToInjected(e.getNewPosition()));
                listener.caretPositionChanged(event);
            }
        };
        myCaretListeners.registerWrapper(listener, wrapper);
        myDelegate.addCaretListener(wrapper);
    }

    @Override
    public void removeCaretListener( final CaretListener listener) {
        CaretListener wrapper = myCaretListeners.removeWrapper(listener);
        if (wrapper != null) {
            myDelegate.removeCaretListener(wrapper);
        }
    }

    public void disposeModel() {
        for (CaretListener wrapper : myCaretListeners.wrappers()) {
            myDelegate.removeCaretListener(wrapper);
        }
        myCaretListeners.clear();
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
    public TextAttributes getTextAttributes() {
        return myDelegate.getTextAttributes();
    }

    @Override
    public boolean supportsMultipleCarets() {
        return myDelegate.supportsMultipleCarets();
    }

    
    @Override
    public Caret getCurrentCaret() {
        return createInjectedCaret(myDelegate.getCurrentCaret());
    }

    
    @Override
    public Caret getPrimaryCaret() {
        return createInjectedCaret(myDelegate.getPrimaryCaret());
    }

    @Override
    public int getCaretCount() {
        return myDelegate.getCaretCount();
    }

    
    @Override
    public List<Caret> getAllCarets() {
        List<Caret> hostCarets = myDelegate.getAllCarets();
        List<Caret> carets = new ArrayList<Caret>(hostCarets.size());
        for (Caret hostCaret : hostCarets) {
            carets.add(createInjectedCaret(hostCaret));
        }
        return carets;
    }

    
    @Override
    public Caret getCaretAt( VisualPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        Caret caret = myDelegate.getCaretAt(myHostEditor.logicalToVisualPosition(hostPos));
        return createInjectedCaret(caret);
    }

    
    @Override
    public Caret addCaret( VisualPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        Caret caret = myDelegate.addCaret(myHostEditor.logicalToVisualPosition(hostPos));
        return createInjectedCaret(caret);
    }

    @Override
    public boolean removeCaret( Caret caret) {
        if (caret instanceof InjectedCaret) {
            caret = ((InjectedCaret)caret).myDelegate;
        }
        return myDelegate.removeCaret(caret);
    }

    @Override
    public void removeSecondaryCarets() {
        myDelegate.removeSecondaryCarets();
    }

    @Override
    public void setCaretsAndSelections( List<CaretState> caretStates) {
        List<CaretState> convertedStates = convertCaretStates(caretStates);
        myDelegate.setCaretsAndSelections(convertedStates);
    }

    @Override
    public void setCaretsAndSelections( List<CaretState> caretStates, boolean updateSystemSelection) {
        List<CaretState> convertedStates = convertCaretStates(caretStates);
        myDelegate.setCaretsAndSelections(convertedStates, updateSystemSelection);
    }

    private List<CaretState> convertCaretStates(List<CaretState> caretStates) {
        List<CaretState> convertedStates = new ArrayList<CaretState>(caretStates.size());
        for (CaretState state : caretStates) {
            convertedStates.add(new CaretState(injectedToHost(state.getCaretPosition()),
                    injectedToHost(state.getSelectionStart()),
                    injectedToHost(state.getSelectionEnd())));
        }
        return convertedStates;
    }

    private LogicalPosition injectedToHost( LogicalPosition position) {
        return position == null ? null : myEditorWindow.injectedToHost(position);
    }

    
    @Override
    public List<CaretState> getCaretsAndSelections() {
        List<CaretState> caretsAndSelections = myDelegate.getCaretsAndSelections();
        List<CaretState> convertedStates = new ArrayList<CaretState>(caretsAndSelections.size());
        for (CaretState state : caretsAndSelections) {
            convertedStates.add(new CaretState(hostToInjected(state.getCaretPosition()),
                    hostToInjected(state.getSelectionStart()),
                    hostToInjected(state.getSelectionEnd())));
        }
        return convertedStates;
    }

    private LogicalPosition hostToInjected( LogicalPosition position) {
        return position == null ? null : myEditorWindow.hostToInjected(position);
    }

    private InjectedCaret createInjectedCaret(Caret caret) {
        if (caret == null) {
            return null;
        }
        synchronized (myInjectedCaretMap) {
            InjectedCaret injectedCaret = myInjectedCaretMap.get(caret);
            if (injectedCaret == null) {
                injectedCaret = new InjectedCaret(myEditorWindow, caret);
                myInjectedCaretMap.put(caret, injectedCaret);
            }
            return injectedCaret;
        }
    }

    @Override
    public void runForEachCaret(final  CaretAction action) {
        myDelegate.runForEachCaret(new CaretAction() {
            @Override
            public void perform(Caret caret) {
                action.perform(createInjectedCaret(caret));
            }
        });
    }

    @Override
    public void runForEachCaret( final CaretAction action, boolean reverseOrder) {
        myDelegate.runForEachCaret(new CaretAction() {
            @Override
            public void perform(Caret caret) {
                action.perform(createInjectedCaret(caret));
            }
        }, reverseOrder);
    }

    @Override
    public void runBatchCaretOperation( Runnable runnable) {
        myDelegate.runBatchCaretOperation(runnable);
    }
}
