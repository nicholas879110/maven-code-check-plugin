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
package com.gome.maven.patterns;

import com.gome.maven.psi.PsiAnnotation;
import com.gome.maven.util.ProcessingContext;

/**
 * @author peter
 */
public class PsiAnnotationPattern extends PsiElementPattern<PsiAnnotation, PsiAnnotationPattern> {
    protected PsiAnnotationPattern() {
        super(PsiAnnotation.class);
    }

    public PsiAnnotationPattern qName(final ElementPattern<String> pattern) {
        return with(new PatternCondition<PsiAnnotation>("qName") {
            public boolean accepts( final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                return pattern.accepts(psiAnnotation.getQualifiedName(), context);
            }
        });
    }
    public PsiAnnotationPattern qName( String qname) {
        return qName(StandardPatterns.string().equalTo(qname));
    }
}
