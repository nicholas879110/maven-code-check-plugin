/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.psi.injection;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.util.ProcessingContext;
import com.gome.maven.util.containers.ContainerUtil;

/**
 * This will work only in presence of IntelliLang plugin.
 *
 * @author Dmitry Avdeev
 *         Date: 01.08.13
 */
public abstract class ReferenceInjector extends Injectable {

    public static final ExtensionPointName<ReferenceInjector> EXTENSION_POINT_NAME = ExtensionPointName.create("com.gome.maven.referenceInjector");

    @Override
    public final Language getLanguage() {
        return null;
    }

    /**
     * Generated references should be soft ({@link com.gome.maven.psi.PsiReference#isSoft()})
     */
    
    public abstract PsiReference[] getReferences( PsiElement element,  final ProcessingContext context,  TextRange range);

    public static ReferenceInjector findById(final String id) {
        return ContainerUtil.find(EXTENSION_POINT_NAME.getExtensions(), new Condition<ReferenceInjector>() {
            @Override
            public boolean value(ReferenceInjector injector) {
                return id.equals(injector.getId());
            }
        });
    }
}
