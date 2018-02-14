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

/*
 * @author max
 */
package com.gome.maven.openapi.fileEditor.impl.text;

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.codeInsight.daemon.impl.TextEditorBackgroundHighlighter;
import com.gome.maven.codeInsight.folding.CodeFoldingManager;
import com.gome.maven.codeInsight.lookup.LookupManager;
import com.gome.maven.codeInsight.lookup.impl.LookupImpl;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

public class PsiAwareTextEditorImpl extends TextEditorImpl {
    private TextEditorBackgroundHighlighter myBackgroundHighlighter;

    public PsiAwareTextEditorImpl( final Project project,  final VirtualFile file, final TextEditorProvider provider) {
        super(project, file, provider);
    }

    
    @Override
    protected TextEditorComponent createEditorComponent(final Project project, final VirtualFile file) {
        return new PsiAwareTextEditorComponent(project, file, this);
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        if (myBackgroundHighlighter == null) {
            myBackgroundHighlighter = new TextEditorBackgroundHighlighter(myProject, getEditor());
        }
        return myBackgroundHighlighter;
    }

    private static class PsiAwareTextEditorComponent extends TextEditorComponent {
        private final Project myProject;
        private final VirtualFile myFile;

        private PsiAwareTextEditorComponent( final Project project,
                                             final VirtualFile file,
                                             final TextEditorImpl textEditor) {
            super(project, file, textEditor);
            myProject = project;
            myFile = file;
        }

        @Override
        void dispose() {
            CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(myProject);
            if (foldingManager != null) {
                foldingManager.releaseFoldings(getEditor());
            }
            super.dispose();
        }

        @Override
        public Object getData(final String dataId) {
            if (PlatformDataKeys.DOMINANT_HINT_AREA_RECTANGLE.is(dataId)) {
                final LookupImpl lookup = (LookupImpl)LookupManager.getInstance(myProject).getActiveLookup();
                if (lookup != null && lookup.isVisible()) {
                    return lookup.getBounds();
                }
            }
            if (LangDataKeys.MODULE.is(dataId)) {
                return ModuleUtilCore.findModuleForFile(myFile, myProject);
            }
            return super.getData(dataId);
        }
    }
}
