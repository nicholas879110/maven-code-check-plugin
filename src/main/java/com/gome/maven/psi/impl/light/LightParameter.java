/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.light;

import com.gome.maven.lang.Language;
import com.gome.maven.psi.*;

/**
 * @author peter
 */
public class LightParameter extends LightVariableBuilder<LightVariableBuilder> implements PsiParameter {
    private final PsiElement myDeclarationScope;
    private final boolean myVarArgs;

    public LightParameter( String name,  PsiType type,  PsiElement declarationScope,  Language language) {
        this(name, type, declarationScope, language, type instanceof PsiEllipsisType);
    }

    public LightParameter( String name,  PsiType type,  PsiElement declarationScope,  Language language, boolean isVarArgs) {
        super(declarationScope.getManager(), name, type, language);
        myDeclarationScope = declarationScope;
        myVarArgs = isVarArgs;
    }

    
    @Override
    public PsiElement getDeclarationScope() {
        return myDeclarationScope;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitParameter(this);
        }
    }

    @Override
    public String toString() {
        return "Light Parameter";
    }

    @Override
    public boolean isVarArgs() {
        return myVarArgs;
    }
}
