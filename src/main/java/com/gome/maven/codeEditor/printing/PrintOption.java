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
 * User: anna
 * Date: 25-Jan-2008
 */
package com.gome.maven.codeEditor.printing;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.options.UnnamedConfigurable;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;

import java.util.Map;
import java.util.TreeMap;

public abstract class PrintOption {
    public static final ExtensionPointName<PrintOption> EP_NAME = ExtensionPointName.create("com.intellij.printOption");

    
    public abstract TreeMap<Integer, PsiReference> collectReferences(PsiFile psiFile, Map<PsiFile, PsiFile> filesMap);

    
    public abstract UnnamedConfigurable createConfigurable();
}