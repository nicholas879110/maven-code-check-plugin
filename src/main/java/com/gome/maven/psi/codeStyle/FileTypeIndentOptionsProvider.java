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

/*
 * Created by IntelliJ IDEA.
 * User: Maxim.Mossienko
 * Date: Nov 28, 2006
 * Time: 4:33:11 PM
 */
package com.gome.maven.psi.codeStyle;

import com.gome.maven.application.options.IndentOptionsEditor;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.psi.PsiFile;

/**
 * Allows to specify indent options for specific file types as opposed to languages. For a language it is highly recommended to use
 * <code>LanguageCodeStyleSettingsProvider</code>.
 * @see LanguageCodeStyleSettingsProvider
 * @see com.gome.maven.psi.codeStyle.CodeStyleSettings#getIndentOptions(FileType)
 */
public interface FileTypeIndentOptionsProvider {
    ExtensionPointName<FileTypeIndentOptionsProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.fileTypeIndentOptionsProvider");

    CommonCodeStyleSettings.IndentOptions createIndentOptions();

    FileType getFileType();

    IndentOptionsEditor createOptionsEditor();

    String getPreviewText();

    void prepareForReformat(final PsiFile psiFile);
}