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
package com.gome.maven.psi.jsp;

import com.gome.maven.lang.jsp.JspxFileViewProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;

public interface BaseJspFile extends XmlFile {
    BaseJspFile[] EMPTY_ARRAY = new BaseJspFile[0];

    PsiElement[] getContentsElements();

    boolean isErrorPage();
    boolean isSessionPage();
    boolean isTagPage();

    XmlTag[] getDirectiveTags(JspDirectiveKind directiveKind, final boolean searchInIncludes);
    XmlTag createDirective(XmlTag context, JspDirectiveKind directiveKind);
    XmlTag createDirective(JspDirectiveKind directiveKind);

    /**
     * Method with a bad name. Returns file corresponding to getTemplateDataLanguage() method of ViewProvider
     * @see com.gome.maven.psi.templateLanguages.TemplateLanguageFileViewProvider#getTemplateDataLanguage()
     */
    PsiFile getBaseLanguageRoot();
    /**
     * @return file which the errorPage directive references,
     * or null, if there is no errorPage directive or directive references invalid file
     */
    PsiFile getErrorPage();

    
    JspxFileViewProvider getViewProvider();

    
    XmlTag getRootTag();
}
