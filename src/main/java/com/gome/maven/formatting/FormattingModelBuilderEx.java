/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.formatting;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleSettings;
import com.gome.maven.psi.codeStyle.CommonCodeStyleSettings;

/**
 * @author Denis Zhdanov
 * @since 8/22/12 2:23 PM
 */
public interface FormattingModelBuilderEx extends FormattingModelBuilder {

    /**
     * Requests building the formatting model for a section of the file containing
     * the specified PSI element and its children.
     *
     * @param element  the top element for which formatting is requested.
     * @param settings the code style settings used for formatting.
     * @param mode     formatting mode
     * @return the formatting model for the file.
     */
    
    FormattingModel createModel( final PsiElement element,  final CodeStyleSettings settings,  FormattingMode mode);

    /**
     * Allows to adjust indent options to used during performing formatting operation on the given ranges of the given file.
     * <p/>
     * Default algorithm is to query given settings for indent options using given file's language as a key.
     *
     * @param file      target file which content is going to be reformatted
     * @param ranges    given file's ranges to reformat
     * @param settings  code style settings holder
     * @return          indent options to use for the target formatting operation (if any adjustment is required);
     *                  <code>null</code> to trigger default algorithm usage
     * @deprecated Use <code>FileIndentOptionsProvider</code> instead.
     * @see com.gome.maven.psi.codeStyle.FileIndentOptionsProvider
     */
    @SuppressWarnings("UnusedDeclaration")

    @Deprecated
    CommonCodeStyleSettings.IndentOptions getIndentOptionsToUse( PsiFile file,
                                                                 FormatTextRanges ranges,
                                                                 CodeStyleSettings settings);
}