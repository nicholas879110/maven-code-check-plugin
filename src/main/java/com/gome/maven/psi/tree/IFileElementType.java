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
package com.gome.maven.psi.tree;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.psi.PsiElement;

public class IFileElementType extends ILazyParseableElementType {
    public IFileElementType(final Language language) {
        super("FILE", language);
    }

    public IFileElementType(  final String debugName, final Language language) {
        super(debugName, language);
    }

    public IFileElementType(  final String debugName, final Language language, boolean register) {
        super(debugName, language, register);
    }

    
    @Override
    public ASTNode parseContents(final ASTNode chameleon) {
        final PsiElement psi = chameleon.getPsi();
        assert psi != null : "Bad chameleon: " + chameleon;
        return doParseContents(chameleon, psi);
    }
}
