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

package com.gome.maven.find.actions;

import com.gome.maven.CommonBundle;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.find.FindBundle;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.findUsages.EmptyFindUsagesProvider;
import com.gome.maven.lang.findUsages.LanguageFindUsages;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilBase;
import com.gome.maven.usages.UsageTarget;
import com.gome.maven.usages.UsageView;

public class FindUsagesInFileAction extends AnAction {

    public FindUsagesInFileAction() {
        setInjectedContext(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) return;
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

        UsageTarget[] usageTargets = UsageView.USAGE_TARGETS_KEY.getData(dataContext);
        if (usageTargets != null) {
            FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(dataContext);
            if (fileEditor != null) {
                usageTargets[0].findUsagesInEditor(fileEditor);
            }
        }
        else if (editor == null) {
            Messages.showMessageDialog(
                    project,
                    FindBundle.message("find.no.usages.at.cursor.error"),
                    CommonBundle.getErrorTitle(),
                    Messages.getErrorIcon()
            );
        }
        else {
            HintManager.getInstance().showErrorHint(editor, FindBundle.message("find.no.usages.at.cursor.error"));
        }
    }

    @Override
    public void update(AnActionEvent event){
        updateFindUsagesAction(event);
    }

    private static boolean isEnabled(DataContext dataContext) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return false;
        }

        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor == null) {
            UsageTarget[] target = UsageView.USAGE_TARGETS_KEY.getData(dataContext);
            return target != null && target.length > 0;
        }
        else {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) {
                return false;
            }

            Language language = PsiUtilBase.getLanguageInEditor(editor, project);
            if (language == null) {
                language = file.getLanguage();
            }
            return !(LanguageFindUsages.INSTANCE.forLanguage(language) instanceof EmptyFindUsagesProvider);
        }
    }

    public static void updateFindUsagesAction(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        boolean enabled = isEnabled(dataContext);
        presentation.setVisible(enabled || !ActionPlaces.isPopupPlace(event.getPlace()));
        presentation.setEnabled(enabled);
    }
}
