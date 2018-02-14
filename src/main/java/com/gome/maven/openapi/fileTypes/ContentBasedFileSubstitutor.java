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
package com.gome.maven.openapi.fileTypes;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

/** @deprecated use com.gome.maven.psi.compiled.ClassFileDecompilers or com.gome.maven.psi.LanguageSubstitutors API (to remove in IDEA 14) */
@SuppressWarnings("deprecation")
public interface ContentBasedFileSubstitutor {
    ExtensionPointName<ContentBasedFileSubstitutor> EP_NAME = ExtensionPointName.create("com.gome.maven.contentBasedClassFileProcessor");

    boolean isApplicable(Project project, VirtualFile vFile);

    
    String obtainFileText(Project project, VirtualFile file);

    
    Language obtainLanguageForFile(VirtualFile file);
}
