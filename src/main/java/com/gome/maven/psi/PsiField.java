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
package com.gome.maven.psi;

import com.gome.maven.util.ArrayFactory;
import com.gome.maven.util.IncorrectOperationException;

/**
 * Represents a Java field or enum constant.
 */
public interface PsiField extends PsiMember, PsiVariable, PsiDocCommentOwner {
    /**
     * The empty array of PSI fields which can be reused to avoid unnecessary allocations.
     */
    PsiField[] EMPTY_ARRAY = new PsiField[0];

    ArrayFactory<PsiField> ARRAY_FACTORY = new ArrayFactory<PsiField>() {
        
        @Override
        public PsiField[] create(final int count) {
            return count == 0 ? EMPTY_ARRAY : new PsiField[count];
        }
    };

    /**
     * Adds initializer to the field declaration or, if <code>initializer</code> parameter is null,
     * removes the initializer from the field declaration.
     *
     * @param initializer the initializer to add.
     * @throws IncorrectOperationException if the modifications fails for some reason.
     * @since 5.0.2
     */
    void setInitializer( PsiExpression initializer) throws IncorrectOperationException;

    @Override
     PsiIdentifier getNameIdentifier();
}
