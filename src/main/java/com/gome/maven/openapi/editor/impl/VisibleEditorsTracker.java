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
package com.gome.maven.openapi.editor.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandAdapter;
import com.gome.maven.openapi.command.CommandEvent;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;

import java.util.HashSet;
import java.util.Set;

public class VisibleEditorsTracker extends CommandAdapter {
    private final Set<Editor> myEditorsVisibleOnCommandStart = new HashSet<Editor>();
    private long myCurrentCommandStart;
    private long myLastCommandFinish;

    public static VisibleEditorsTracker getInstance() {
        return ApplicationManager.getApplication().getComponent(VisibleEditorsTracker.class);
    }


    public VisibleEditorsTracker(CommandProcessor commandProcessor) {
        commandProcessor.addCommandListener(this);
    }

    public boolean wasEditorVisibleOnCommandStart(Editor editor){
        return myEditorsVisibleOnCommandStart.contains(editor);
    }

    public long getCurrentCommandStart() { return myCurrentCommandStart; }

    public long getLastCommandFinish() { return myLastCommandFinish; }

    @Override
    public void commandStarted(CommandEvent event) {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor.getComponent().isShowing()) {
                myEditorsVisibleOnCommandStart.add(editor);
            }

            ((ScrollingModelImpl)editor.getScrollingModel()).finishAnimation();
            myCurrentCommandStart = System.currentTimeMillis();
        }
    }

    @Override
    public void commandFinished(CommandEvent event) {
        myEditorsVisibleOnCommandStart.clear();
        myLastCommandFinish = System.currentTimeMillis();
    }
}
