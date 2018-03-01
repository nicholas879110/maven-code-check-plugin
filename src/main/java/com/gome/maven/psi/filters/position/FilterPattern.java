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
package com.gome.maven.psi.filters.position;

import com.gome.maven.patterns.ObjectPattern;
import com.gome.maven.patterns.InitialPatternCondition;
import com.gome.maven.util.ProcessingContext;
import com.gome.maven.psi.filters.ElementFilter;
import com.gome.maven.psi.PsiElement;

/**
 * @author peter
 */
public class FilterPattern extends ObjectPattern<Object,FilterPattern> {
     private final ElementFilter myFilter;

    public FilterPattern( final ElementFilter filter) {
        super(new InitialPatternCondition<Object>(Object.class) {
            @Override
            public boolean accepts( final Object o, final ProcessingContext context) {
                return filter == null ||
                        o != null &&
                                filter.isClassAcceptable(o.getClass()) &&
                                filter.isAcceptable(o, o instanceof PsiElement ? (PsiElement)o : null);
            }
        });
        myFilter = filter;
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FilterPattern)) return false;

        final FilterPattern that = (FilterPattern)o;

        if (myFilter != null ? !myFilter.equals(that.myFilter) : that.myFilter != null) return false;

        return true;
    }

    public int hashCode() {
        return (myFilter != null ? myFilter.hashCode() : 0);
    }

    @Override
    public String toString() {
        return super.toString() + " & " + myFilter;
    }
}
