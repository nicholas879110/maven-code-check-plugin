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

import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.Separator;
import com.gome.maven.openapi.editor.ex.EditorGutterComponentEx;

import java.util.ArrayList;
import java.util.List;

public class AnnotateActionGroup extends ActionGroup {
    private final AnAction[] myActions;

    public AnnotateActionGroup(List<AnnotationFieldGutter> gutters, EditorGutterComponentEx gutterComponent) {
        super("View", true);
        final List<AnAction> actions = new ArrayList<AnAction>();
        for (AnnotationFieldGutter g : gutters) {
            if (g.getID() != null) {
                actions.add(new ShowHideAspectAction(g, gutterComponent));
            }
        }
        actions.add(Separator.getInstance());
        actions.add(new ShowAnnotationColorsAction(gutterComponent));
        actions.add(new ShowShortenNames(gutterComponent));
        myActions = actions.toArray(new AnAction[actions.size()]);
    }

    
    @Override
    public AnAction[] getChildren( AnActionEvent e) {
        return myActions;
    }
}
