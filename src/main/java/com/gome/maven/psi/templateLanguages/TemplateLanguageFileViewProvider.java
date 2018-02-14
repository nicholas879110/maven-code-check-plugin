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
package com.gome.maven.psi.templateLanguages;

import com.gome.maven.lang.Language;
import com.gome.maven.psi.FileViewProvider;

/**
 * @author peter
 */
public interface TemplateLanguageFileViewProvider extends FileViewProvider {

    /**
     * e.g. JSP
     * @return instanceof {@link com.gome.maven.psi.templateLanguages.TemplateLanguage}
     */
    @Override
    Language getBaseLanguage();

    /**
     * e.g. HTML for JSP files
     * @return not instanceof {@link com.gome.maven.lang.DependentLanguage}
     */

    Language getTemplateDataLanguage();
}
