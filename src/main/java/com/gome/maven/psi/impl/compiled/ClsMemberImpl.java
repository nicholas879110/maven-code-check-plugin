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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.util.AtomicNotNullLazyValue;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.psi.PsiDocCommentOwner;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiIdentifier;
import com.gome.maven.psi.PsiNameIdentifierOwner;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.stubs.NamedStub;
import com.gome.maven.util.IncorrectOperationException;

public abstract class ClsMemberImpl<T extends NamedStub> extends ClsRepositoryPsiElement<T> implements PsiDocCommentOwner, PsiNameIdentifierOwner {
    private final NotNullLazyValue<PsiDocComment> myDocComment;
    private final NotNullLazyValue<PsiIdentifier> myNameIdentifier;

    protected ClsMemberImpl(T stub) {
        super(stub);
        myDocComment = !isDeprecated() ? null : new AtomicNotNullLazyValue<PsiDocComment>() {
            
            @Override
            protected PsiDocComment compute() {
                return new ClsDocCommentImpl(ClsMemberImpl.this);
            }
        };
        myNameIdentifier = new AtomicNotNullLazyValue<PsiIdentifier>() {
            
            @Override
            protected PsiIdentifier compute() {
                return new ClsIdentifierImpl(ClsMemberImpl.this, getName());
            }
        };
    }

    @Override
    public PsiDocComment getDocComment() {
        return myDocComment != null ? myDocComment.getValue() : null;
    }

    @Override
    
    public PsiIdentifier getNameIdentifier() {
        return myNameIdentifier.getValue();
    }

    @Override
    
    public String getName() {
        //noinspection ConstantConditions
        return getStub().getName();
    }

    @Override
    public PsiElement setName( String name) throws IncorrectOperationException {
        PsiImplUtil.setName(getNameIdentifier(), name);
        return this;
    }
}
