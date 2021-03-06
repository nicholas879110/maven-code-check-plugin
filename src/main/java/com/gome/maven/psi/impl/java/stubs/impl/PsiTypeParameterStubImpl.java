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
package com.gome.maven.psi.impl.java.stubs.impl;

import com.gome.maven.psi.PsiTypeParameter;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiAnnotationStub;
import com.gome.maven.psi.impl.java.stubs.PsiTypeParameterStub;
import com.gome.maven.psi.stubs.StubBase;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.io.StringRef;

import java.util.List;

public class PsiTypeParameterStubImpl extends StubBase<PsiTypeParameter> implements PsiTypeParameterStub {
    private final StringRef myName;

    public PsiTypeParameterStubImpl(final StubElement parent, final StringRef name) {
        super(parent, JavaStubElementTypes.TYPE_PARAMETER);
        myName = name;
    }

    @Override
    public String getName() {
        return StringRef.toString(myName);
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiTypeParameter[").append(myName).append(']');
        return builder.toString();
    }

    @Override

    public List<PsiAnnotationStub> getAnnotations() {
        List<StubElement> children = getChildrenStubs();

        return ContainerUtil.mapNotNull(children, new Function<StubElement, PsiAnnotationStub>() {
            @Override
            public PsiAnnotationStub fun(StubElement stubElement) {
                return stubElement instanceof PsiAnnotationStub ? (PsiAnnotationStub)stubElement : null;
            }
        });
    }
}