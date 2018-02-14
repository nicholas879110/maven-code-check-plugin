/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.openapi.editor.actions;

import com.gome.maven.execution.impl.ConsoleViewUtil;
import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.find.FindManager;
import com.gome.maven.find.FindModel;
import com.gome.maven.find.FindUtil;
import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.actionSystem.EditorAction;
import com.gome.maven.openapi.editor.actionSystem.EditorActionHandler;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;

public class IncrementalFindAction extends EditorAction {
    public static class Handler extends EditorActionHandler {

        private final boolean myReplace;

        public Handler(boolean isReplace) {

            myReplace = isReplace;
        }

        @Override
        public void execute(final Editor editor, DataContext dataContext) {
            final Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getComponent()));
            if (!editor.isOneLineMode()) {
                final JComponent headerComponent = editor.getHeaderComponent();
                if (headerComponent instanceof EditorSearchComponent) {
                    EditorSearchComponent editorSearchComponent = (EditorSearchComponent)headerComponent;
                    headerComponent.requestFocus();
                    FindUtil.configureFindModel(myReplace, editor, editorSearchComponent.getFindModel(), false);
                } else {
                    FindManager findManager = FindManager.getInstance(project);
                    FindModel model;
                    if (myReplace) {
                        model = findManager.createReplaceInFileModel();
                    } else {
                        model = new FindModel();
                        model.copyFrom(findManager.getFindInFileModel());
                    }
                    FindUtil.configureFindModel(myReplace, editor, model, true);
                    final EditorSearchComponent header = new EditorSearchComponent(editor, project, model);
                    editor.setHeaderComponent(header);
                    header.requestFocus();
                }
            }
        }

        @Override
        public boolean isEnabled(Editor editor, DataContext dataContext) {
            if (myReplace && ConsoleViewUtil.isConsoleViewEditor(editor)) {
                return false;
            }
            Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getComponent()));
            return project != null && !editor.isOneLineMode();
        }
    }

    public IncrementalFindAction() {
        super(new Handler(false));
    }
}