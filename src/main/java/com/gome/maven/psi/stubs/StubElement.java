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
package com.gome.maven.psi.stubs;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.ArrayFactory;

import java.util.List;

public interface StubElement<T extends PsiElement> extends Stub {
    IStubElementType getStubType();
    StubElement getParentStub();
    List<StubElement> getChildrenStubs();

    
    <P extends PsiElement> StubElement<P> findChildStubByType(IStubElementType<?, P> elementType);

    T getPsi();

    <E extends PsiElement> E[] getChildrenByType(final IElementType elementType, final E[] array);
    <E extends PsiElement> E[] getChildrenByType(final TokenSet filter, final E[] array);

    <E extends PsiElement> E[] getChildrenByType(final IElementType elementType, ArrayFactory<E> f);
    <E extends PsiElement> E[] getChildrenByType(final TokenSet filter, ArrayFactory<E> f);

    
    <E extends PsiElement> E getParentStubOfType(final Class<E> parentClass);
}