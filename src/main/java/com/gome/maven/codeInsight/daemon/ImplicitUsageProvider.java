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

package com.gome.maven.codeInsight.daemon;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiElement;

/**
 * Allows to disable highlighting of certain elements as unused when such elements are not referenced
 * from the code but are referenced in some other way (for example, from generated code).
 *
 * @author yole
 * @since 6.0
 */
public interface ImplicitUsageProvider {
    ExtensionPointName<ImplicitUsageProvider> EP_NAME = new ExtensionPointName<ImplicitUsageProvider>("com.gome.maven.implicitUsageProvider");

    boolean isImplicitUsage(PsiElement element);
    boolean isImplicitRead(PsiElement element);
    boolean isImplicitWrite(PsiElement element);
}
