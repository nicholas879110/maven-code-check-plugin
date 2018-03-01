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
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 01.11.2006
 * Time: 17:15:24
 */
package com.gome.maven.usages.impl.rules;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.openapi.extensions.ExtensionPointName;

public interface UsageTypeProvider {
    ExtensionPointName<UsageTypeProvider> EP_NAME = new ExtensionPointName<UsageTypeProvider>("com.gome.maven.usageTypeProvider");

     UsageType getUsageType(PsiElement element);
}