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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiType;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.JavaElementType;

public class PsiEmptyExpressionImpl extends ExpressionPsiElement implements PsiExpression{
    public PsiEmptyExpressionImpl() {
        super(JavaElementType.EMPTY_EXPRESSION);
    }

    @Override
    public PsiType getType() {
        return null;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiExpression(empty)";
    }
}
