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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeInsight.CodeInsightActionHandler;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;

public class ShowErrorDescriptionHandler implements CodeInsightActionHandler {
    private final int myWidth;

    public ShowErrorDescriptionHandler(final int width) {
        myWidth = width;
    }

    @Override
    public void invoke( Project project,  Editor editor,  PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
        HighlightInfo info = ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(editor.getDocument(), offset, false);
        if (info != null) {
            DaemonTooltipUtil.showInfoTooltip(info, editor, editor.getCaretModel().getOffset(), myWidth);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
