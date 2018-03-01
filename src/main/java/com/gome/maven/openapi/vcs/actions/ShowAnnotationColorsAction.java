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
package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.editor.ex.EditorGutterComponentEx;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class ShowAnnotationColorsAction extends ActionGroup {
    private final AnAction[] myChildren;

    public ShowAnnotationColorsAction(EditorGutterComponentEx gutter) {
        super("Colors", true);

        final ArrayList<AnAction> kids = new ArrayList<AnAction>(ShortNameType.values().length);
        for (ColorMode type : ColorMode.values()) {
            kids.add(new SetColorModeAction(type, gutter));
        }
        myChildren = kids.toArray(new AnAction[kids.size()]);
    }

    
    @Override
    public AnAction[] getChildren( AnActionEvent e) {
        return myChildren;
    }

    public static ColorMode getType() {
        for (ColorMode type : ColorMode.values()) {
            if (type.isSet()) {
                return type;
            }
        }
        return ColorMode.ORDER;
    }

    public static class SetColorModeAction extends ToggleAction {
        private final ColorMode myType;
        private final EditorGutterComponentEx myGutter;

        public SetColorModeAction(ColorMode type, EditorGutterComponentEx gutter) {
            super(type.getDescription());
            myType = type;
            myGutter = gutter;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myType == getType();
        }

        @Override
        public void setSelected(AnActionEvent e, boolean enabled) {
            if (enabled) {
                myType.set();
            }
            myGutter.revalidateMarkup();
        }
    }
}