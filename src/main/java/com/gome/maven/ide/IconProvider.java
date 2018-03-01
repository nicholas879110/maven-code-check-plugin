/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.ide;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.psi.PsiElement;

import javax.swing.*;

/**
 * Customize icons for {@link PsiElement}s.
 *
 * @author peter
 */
public abstract class IconProvider {
    public static final ExtensionPointName<IconProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.gome.maven.iconProvider");

    /**
     * @param element for which icon is shown
     * @param flags   used for customizing the icon appearance. Flags are listed in {@link com.gome.maven.openapi.util.Iconable}
     * @return {@code null} if this provider cannot provide icon for given element.
     * @see com.gome.maven.openapi.util.Iconable
     */

    public abstract Icon getIcon( PsiElement element, @Iconable.IconFlags int flags);
}
