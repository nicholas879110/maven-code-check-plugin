/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.openapi.editor;

import com.gome.maven.lang.LanguageExtension;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;

/**
 * Exposes {@link LineWrapPositionStrategy} implementations to their clients.
 *
 * @author Denis Zhdanov
 * @since Aug 25, 2010 11:30:21 AM
 */
public class LanguageLineWrapPositionStrategy extends LanguageExtension<LineWrapPositionStrategy> {

    public static final String EP_NAME = "com.gome.maven.lang.lineWrapStrategy";
    public static final LanguageLineWrapPositionStrategy INSTANCE = new LanguageLineWrapPositionStrategy();

    private LanguageLineWrapPositionStrategy() {
        super(EP_NAME, new DefaultLineWrapPositionStrategy());
    }

    /**
     * Asks to get wrap position strategy to use for the document managed by the given editor.
     *
     * @param editor    editor that manages document which text should be processed by wrap position strategy
     * @return          line wrap position strategy to use for the lines from the document managed by the given editor
     */
    
    public LineWrapPositionStrategy forEditor( Editor editor) {
        LineWrapPositionStrategy result = getDefaultImplementation();
        Project project = editor.getProject();
        if (project != null && !project.isDisposed()) {
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (psiFile != null) {
                result = INSTANCE.forLanguage(psiFile.getLanguage());
            }
        }
        return result;
    }

    
    @Override
    public LineWrapPositionStrategy getDefaultImplementation() {
        return super.getDefaultImplementation();
    }
}
