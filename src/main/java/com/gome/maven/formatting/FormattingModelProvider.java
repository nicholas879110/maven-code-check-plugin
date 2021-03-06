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
package com.gome.maven.formatting;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleSettings;

/**
 * Allows plugins to create an instance of the standard {@link FormattingModel} implementation.
 *
 * @see FormattingModelBuilder#createModel(com.gome.maven.psi.PsiElement, com.gome.maven.psi.codeStyle.CodeStyleSettings)
 */

public class FormattingModelProvider {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.formatting.FormattingModelProvider");
    private static FormattingModelFactory myFactory;

    static void setFactory(FormattingModelFactory factory) {
        myFactory = factory;
    }

    /**
     * Creates an instance of the standard formatting model implementation for the specified file.
     *
     * @param file      the file containing the text to format.
     * @param rootBlock the root block of the formatting model.
     * @param settings  the code style settings used for formatting.
     * @return the formatting model instance.
     */

    public static FormattingModel createFormattingModelForPsiFile(PsiFile file,
                                                                   Block rootBlock,
                                                                  CodeStyleSettings settings){
        return myFactory.createFormattingModelForPsiFile(file, rootBlock, settings);
    }

}
