/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.Separator;
import com.gome.maven.openapi.editor.colors.ColorKey;
import com.gome.maven.openapi.editor.colors.EditorFontType;
import com.gome.maven.openapi.editor.ex.EditorGutterComponentEx;
import com.gome.maven.openapi.vcs.annotate.*;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AnnotationPresentation implements TextAnnotationPresentation {
    private final FileAnnotation myFileAnnotation;
    
    private final AnnotationSourceSwitcher mySwitcher;
    private final ArrayList<AnAction> myActions;
    private SwitchAnnotationSourceAction mySwitchAction;
    private final List<LineNumberListener> myPopupLineNumberListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    AnnotationPresentation( FileAnnotation fileAnnotation,
                            final AnnotationSourceSwitcher switcher,
                           final EditorGutterComponentEx gutter,
                           final AnAction... actions) {
        myFileAnnotation = fileAnnotation;
        mySwitcher = switcher;

        myActions = new ArrayList<AnAction>();
        myActions.add(Separator.getInstance());
        if (actions != null) {
            final List<AnAction> actionsList = Arrays.asList(actions);
            if (!actionsList.isEmpty()) {
                myActions.addAll(actionsList);
                myActions.add(new Separator());
            }
        }
        if (mySwitcher != null) {
            mySwitchAction = new SwitchAnnotationSourceAction(mySwitcher, gutter);
            myActions.add(mySwitchAction);
        }
    }

    public void addLineNumberListener(final LineNumberListener listener) {
        myPopupLineNumberListeners.add(listener);
    }

    public EditorFontType getFontType(final int line) {
        VcsRevisionNumber revision = myFileAnnotation.originalRevision(line);
        VcsRevisionNumber currentRevision = myFileAnnotation.getCurrentRevision();
        return currentRevision != null && currentRevision.equals(revision) ? EditorFontType.BOLD : EditorFontType.PLAIN;
    }

    public ColorKey getColor(final int line) {
        if (mySwitcher == null) return AnnotationSource.LOCAL.getColor();
        return mySwitcher.getAnnotationSource(line).getColor();
    }

    public List<AnAction> getActions(int line) {
        for (LineNumberListener listener : myPopupLineNumberListeners) {
            listener.consume(line);
        }
        return myActions;
    }

    
    public List<AnAction> getActions() {
        return myActions;
    }

    public void addSourceSwitchListener(final Consumer<AnnotationSource> listener) {
        mySwitchAction.addSourceSwitchListener(listener);
    }

    public void addAction(AnAction action) {
        myActions.add(action);
    }

    public void addAction(AnAction action, int index) {
        myActions.add(index, action);
    }
}
