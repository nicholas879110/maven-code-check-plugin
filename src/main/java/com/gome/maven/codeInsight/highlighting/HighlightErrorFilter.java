/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.highlighting;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiErrorElement;

/**
 * @author spleaner
 */
public abstract class HighlightErrorFilter {
    public static final ExtensionPointName<HighlightErrorFilter> EP_NAME = ExtensionPointName.create("com.gome.maven.highlightErrorFilter");

    public abstract boolean shouldHighlightErrorElement( final PsiErrorElement element);

}
