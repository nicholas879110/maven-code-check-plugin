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
 * User: anna
 * Date: 20-Dec-2007
 */
package com.gome.maven.codeInspection.lang;

import com.gome.maven.codeInspection.HTMLComposer;
import com.gome.maven.codeInspection.reference.RefManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;

public abstract class InspectionExtensionsFactory {

    public static final ExtensionPointName<InspectionExtensionsFactory> EP_NAME = ExtensionPointName.create("com.intellij.codeInspection.InspectionExtension");

    public abstract GlobalInspectionContextExtension createGlobalInspectionContextExtension();
    
    public abstract RefManagerExtension createRefManagerExtension(RefManager refManager);
    
    public abstract HTMLComposerExtension createHTMLComposerExtension(final HTMLComposer composer);

    public abstract boolean isToCheckMember( PsiElement element,  String id);

    
    public abstract String getSuppressedInspectionIdsIn( PsiElement element);

    public abstract boolean isProjectConfiguredToRunInspections( Project project, boolean online);

}
