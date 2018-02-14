/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.openapi.vcs.ex;

import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.editor.Editor;

public class RollbackLineStatusRangeAction extends RollbackLineStatusAction {
     private final LineStatusTracker myTracker;
     private final Editor myEditor;
     private final Range myRange;

    public RollbackLineStatusRangeAction( LineStatusTracker tracker,  Range range,  Editor editor) {
        myTracker = tracker;
        myEditor = editor;
        myRange = range;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
    }

    public void actionPerformed(final AnActionEvent e) {
        rollback(myTracker, myEditor, myRange);
    }
}
