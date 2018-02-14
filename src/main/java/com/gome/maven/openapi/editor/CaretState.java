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
package com.gome.maven.openapi.editor;


public class CaretState {
    private final LogicalPosition caretPosition;
    private final LogicalPosition selectionStart;
    private final LogicalPosition selectionEnd;

    public CaretState( LogicalPosition position,  LogicalPosition start,  LogicalPosition end) {
        caretPosition = position;
        selectionStart = start;
        selectionEnd = end;
    }

    
    public LogicalPosition getCaretPosition(){
        return caretPosition;
    }

    
    public LogicalPosition getSelectionStart() {
        return selectionStart;
    }

    
    public LogicalPosition getSelectionEnd() {
        return selectionEnd;
    }

    @Override
    public String toString() {
        return "CaretState{" +
                "caretPosition=" + caretPosition +
                ", selectionStart=" + selectionStart +
                ", selectionEnd=" + selectionEnd +
                '}';
    }
}
