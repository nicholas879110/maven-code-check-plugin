/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.ui;

import com.gome.maven.ide.highlighter.HighlighterFactory;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileFactory;
import com.gome.maven.util.LocalTimeCounter;

public class LanguageTextField extends EditorTextField {
    private final Language myLanguage;
    // Could be null to allow usage in UI designer, as EditorTextField permits
    private final Project myProject;

    public LanguageTextField() {
        this(null, null, "");
    }

    public LanguageTextField(Language language,  Project project,  String value) {
        this(language, project, value, true);
    }

    public LanguageTextField(Language language,  Project project,  String value, boolean oneLineMode) {
        this(language, project, value, new SimpleDocumentCreator(), oneLineMode);
    }

    public LanguageTextField( Language language,
                              Project project,
                              String value,
                              DocumentCreator documentCreator)
    {
        this(language, project, value, documentCreator, true);
    }

    public LanguageTextField( Language language,
                              Project project,
                              String value,
                              DocumentCreator documentCreator,
                             boolean oneLineMode) {
        super(documentCreator.createDocument(value, language, project), project,
                language != null ? language.getAssociatedFileType() : StdFileTypes.PLAIN_TEXT, language == null, oneLineMode);

        myLanguage = language;
        myProject = project;

        setEnabled(language != null);
    }

    public interface DocumentCreator {
        Document createDocument(String value,  Language language, Project project);
    }

    public static class SimpleDocumentCreator implements DocumentCreator {
        @Override
        public Document createDocument(String value,  Language language, Project project) {
            return LanguageTextField.createDocument(value, language, project, this);
        }

        public void customizePsiFile(PsiFile file) {
        }
    }

    private static Document createDocument(String value,  Language language, Project project,
                                            SimpleDocumentCreator documentCreator) {
        if (language != null) {
            final PsiFileFactory factory = PsiFileFactory.getInstance(project);
            final FileType fileType = language.getAssociatedFileType();
            assert fileType != null;

            final long stamp = LocalTimeCounter.currentTime();
            final PsiFile psiFile = factory.createFileFromText("Dummy." + fileType.getDefaultExtension(), fileType, value, stamp, true, false);
            documentCreator.customizePsiFile(psiFile);
            final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            assert document != null;
            return document;
        }
        else {
            return EditorFactory.getInstance().createDocument(value);
        }
    }

    @Override
    protected EditorEx createEditor() {
        final EditorEx ex = super.createEditor();

        if (myLanguage != null) {
            final FileType fileType = myLanguage.getAssociatedFileType();
            ex.setHighlighter(HighlighterFactory.createHighlighter(myProject, fileType));
        }
        ex.setEmbeddedIntoDialogWrapper(true);

        return ex;
    }
}
