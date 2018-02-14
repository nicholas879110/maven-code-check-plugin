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
package com.gome.maven.openapi.editor.event;

import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.LogicalPosition;

import java.util.EventObject;

public class CaretEvent extends EventObject {
    private final Caret myCaret;
    private final LogicalPosition myOldPosition;
    private final LogicalPosition myNewPosition;

    public CaretEvent( Editor editor,  LogicalPosition oldPosition,  LogicalPosition newPosition) {
        this(editor, null, oldPosition, newPosition);
    }

    public CaretEvent( Editor editor,  Caret caret,  LogicalPosition oldPosition,  LogicalPosition newPosition) {
        super(editor);
        myCaret = caret;
        myOldPosition = oldPosition;
        myNewPosition = newPosition;
    }

    
    public Editor getEditor() {
        return (Editor) getSource();
    }

    
    public Caret getCaret() {
        return myCaret;
    }

    
    public LogicalPosition getOldPosition() {
        return myOldPosition;
    }

    
    public LogicalPosition getNewPosition() {
        return myNewPosition;
    }
}
