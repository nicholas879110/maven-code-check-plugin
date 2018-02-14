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

package com.gome.maven.ide.projectView;

import com.gome.maven.psi.PsiElement;

/**
 * @author yole
 */
public interface SelectableTreeStructureProvider extends TreeStructureProvider {
    /**
     * Returns the element which should be selected in the tree when the "Select In" action is
     * invoked for the specified target.
     *
     * @param element element on which "Select In" was invoked.
     * @return the element to select in the tree, or null if default selection logic should be used.
     */

    PsiElement getTopLevelElement(PsiElement element);
}
