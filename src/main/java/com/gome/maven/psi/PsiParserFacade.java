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

package com.gome.maven.psi;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author yole
 */
public interface PsiParserFacade {
    /**
     * Creates an PsiWhiteSpace with the specified text.
     *
     * @param s the text of whitespace
     * @return the created whitespace instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid whitespace.
     */
    
    PsiElement createWhiteSpaceFromText(  String s) throws IncorrectOperationException;

    /**
     * Creates a line comment for the specified language.
     */
    
    PsiComment createLineCommentFromText( LanguageFileType fileType,  String text) throws IncorrectOperationException;

    /**
     * Creates a line comment for the specified language.
     */
    
    PsiComment createBlockCommentFromText( Language language,  String text) throws IncorrectOperationException;

    /**
     * Creates a line comment for the specified language or block comment if language doesn't support line ones
     */
    
    PsiComment createLineOrBlockCommentFromText( Language lang,  String text) throws IncorrectOperationException;

    class SERVICE {
        private SERVICE() {
        }

        public static PsiParserFacade getInstance(Project project) {
            return ServiceManager.getService(project, PsiParserFacade.class);
        }
    }
}
