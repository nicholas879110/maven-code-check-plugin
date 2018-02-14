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
package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.project.DumbAware;

import java.util.Arrays;

public class RemoveOccurrenceAction extends EditorHeaderAction implements DumbAware {
    public RemoveOccurrenceAction(EditorSearchComponent editorSearchComponent) {
        super(editorSearchComponent);

        copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_UNSELECT_PREVIOUS_OCCURENCE));
        getTemplatePresentation().setIcon(AllIcons.Actions.RemoveMulticaret);

        registerShortcutsForComponent(Arrays.asList(getShortcutSet().getShortcuts()), editorSearchComponent.getSearchField());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        getEditorSearchComponent().removeOccurrence();
    }

    @Override
    public void update(AnActionEvent e) {
        boolean isFind = !getEditorSearchComponent().getFindModel().isReplaceState();
        boolean hasMatches = getEditorSearchComponent().hasMatches();
        e.getPresentation().setVisible(isFind);
        e.getPresentation().setEnabled(isFind && hasMatches);
    }
}
