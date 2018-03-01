/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.generation;

import com.gome.maven.codeInsight.CodeInsightActionHandler;
import com.gome.maven.codeInsight.CodeInsightUtilBase;
import com.gome.maven.lang.CodeInsightActions;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageCodeInsightActionHandler;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;

public class ImplementMethodsHandler implements CodeInsightActionHandler{
    @Override
    public final void invoke( final Project project,  final Editor editor,  PsiFile file) {
        if (!CodeInsightUtilBase.prepareEditorForWrite(editor)) return;
        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)){
            return;
        }

        Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
        final LanguageCodeInsightActionHandler codeInsightActionHandler = CodeInsightActions.IMPLEMENT_METHOD.forLanguage(language);
        if (codeInsightActionHandler != null) {
            codeInsightActionHandler.invoke(project, editor, file);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
