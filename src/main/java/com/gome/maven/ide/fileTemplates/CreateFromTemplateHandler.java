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

package com.gome.maven.ide.fileTemplates;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Map;

/**
 * @author yole
 */
public interface CreateFromTemplateHandler {
    ExtensionPointName<CreateFromTemplateHandler> EP_NAME = ExtensionPointName.create("com.gome.maven.createFromTemplateHandler");

    boolean handlesTemplate(FileTemplate template);

    
    PsiElement createFromTemplate(Project project, PsiDirectory directory, final String fileName, FileTemplate template, String templateText,
                                   Map<String, Object> props) throws IncorrectOperationException;

    boolean canCreate(final PsiDirectory[] dirs);
    boolean isNameRequired();
    String getErrorMessage();

    void prepareProperties(Map<String, Object> props);
}
