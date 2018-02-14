/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff;

import com.gome.maven.openapi.ui.WindowWrapper;

import java.awt.*;

public class DiffDialogHints {
     public static final DiffDialogHints DEFAULT = new DiffDialogHints(null);
     public static final DiffDialogHints FRAME = new DiffDialogHints(WindowWrapper.Mode.FRAME);
     public static final DiffDialogHints MODAL = new DiffDialogHints(WindowWrapper.Mode.MODAL);
     public static final DiffDialogHints NON_MODAL = new DiffDialogHints(WindowWrapper.Mode.NON_MODAL);

    //
    // Impl
    //

     private final WindowWrapper.Mode myMode;
     private final Component myParent;

    public DiffDialogHints( WindowWrapper.Mode mode) {
        this(mode, null);
    }

    public DiffDialogHints( WindowWrapper.Mode mode,  Component parent) {
        myMode = mode;
        myParent = parent;
    }

    //
    // Getters
    //

    
    public WindowWrapper.Mode getMode() {
        return myMode;
    }

    
    public Component getParent() {
        return myParent;
    }
}
