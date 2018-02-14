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
 * @author max
 */
package com.gome.maven.lang;

import com.gome.maven.psi.impl.source.tree.ForeignLeafPsiElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ILeafElementType;

public class ForeignLeafType extends TokenWrapper implements ILeafElementType {
    public ForeignLeafType(IElementType delegate, CharSequence value) {
        super(delegate, value);
    }

    @Override
    public ASTNode createLeafNode(CharSequence leafText) {
        return new ForeignLeafPsiElement(this, getValue());
    }
}
