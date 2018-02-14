/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.psi.search;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.util.Processor;

import java.util.Arrays;

/**
 * @author peter
 */
public abstract class RequestResultProcessor {
    private final Object myEquality;

    protected RequestResultProcessor( Object... equality) {
        myEquality = Arrays.asList(equality);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestResultProcessor that = (RequestResultProcessor)o;

        return myEquality.equals(that.myEquality);
    }

    @Override
    public int hashCode() {
        return myEquality.hashCode();
    }

    public abstract boolean processTextOccurrence( PsiElement element, int offsetInElement,  Processor<PsiReference> consumer);
}
