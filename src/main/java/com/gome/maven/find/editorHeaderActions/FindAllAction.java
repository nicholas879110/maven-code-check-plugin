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
import com.gome.maven.find.FindManager;
import com.gome.maven.find.FindModel;
import com.gome.maven.find.FindUtil;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiDocumentManager;

/**
 * Created by IntelliJ IDEA.
 * User: zajac
 * Date: 05.03.11
 * Time: 10:53
 * To change this template use File | Settings | File Templates.
 */
public class FindAllAction extends EditorHeaderAction implements DumbAware {
    public FindAllAction(EditorSearchComponent editorSearchComponent) {
        super(editorSearchComponent);
        getTemplatePresentation().setIcon(AllIcons.Actions.Export);
        getTemplatePresentation().setDescription("Export matches to Find tool window");
        getTemplatePresentation().setText("Find All");
        final AnAction findUsages = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES);
        if (findUsages != null) {
            registerCustomShortcutSet(findUsages.getShortcutSet(),
                    editorSearchComponent.getSearchField());
        }
    }

    @Override
    public void update(final AnActionEvent e) {
        super.update(e);
        Editor editor = getEditorSearchComponent().getEditor();
        Project project = editor.getProject();
        if (project != null && !project.isDisposed()) {
            e.getPresentation().setEnabled(getEditorSearchComponent().hasMatches() &&
                    PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) != null);
        }
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        Editor editor = getEditorSearchComponent().getEditor();
        Project project = editor.getProject();
        if (project != null && !project.isDisposed()) {
            final FindModel model = FindManager.getInstance(project).getFindInFileModel();
            final FindModel realModel = (FindModel)model.clone();
            String text = getEditorSearchComponent().getTextInField();
            if (StringUtil.isEmpty(text)) return;
            realModel.setStringToFind(text);
            FindUtil.findAllAndShow(project, editor, realModel);
        }
    }
}
