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
package com.gome.maven.psi.impl.source.codeStyle;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleSettings;

public interface PostFormatProcessor {
    ExtensionPointName<PostFormatProcessor> EP_NAME = ExtensionPointName.create("com.gome.maven.postFormatProcessor");

    PsiElement processElement( PsiElement source,  CodeStyleSettings settings);
    TextRange processText( PsiFile source,  TextRange rangeToReformat,  CodeStyleSettings settings);
}
