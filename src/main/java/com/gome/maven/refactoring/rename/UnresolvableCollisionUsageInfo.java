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
 * User: dsl
 * Date: 05.06.2002
 * Time: 12:42:12
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.refactoring.rename;

import com.gome.maven.psi.PsiElement;

public abstract class UnresolvableCollisionUsageInfo extends CollisionUsageInfo {
    public UnresolvableCollisionUsageInfo(PsiElement element, PsiElement referencedElement) {
        super(element, referencedElement);
    }

    public abstract String getDescription();
}