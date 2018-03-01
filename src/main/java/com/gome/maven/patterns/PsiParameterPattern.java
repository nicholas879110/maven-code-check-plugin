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
package com.gome.maven.patterns;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiParameter;
import com.gome.maven.util.PairProcessor;
import com.gome.maven.util.ProcessingContext;

/**
 * @author Gregory Shrago
 */
public class PsiParameterPattern extends PsiModifierListOwnerPattern<PsiParameter, PsiParameterPattern> {

    protected PsiParameterPattern() {
        super(PsiParameter.class);
    }

    public PsiParameterPattern ofMethod(final int index, final ElementPattern pattern) {
        return with(new PatternConditionPlus<PsiParameter, PsiMethod>("ofMethod", pattern) {
            @Override
            public boolean processValues(PsiParameter t,
                                         ProcessingContext context,
                                         PairProcessor<PsiMethod, ProcessingContext> processor) {
                PsiElement scope = t.getDeclarationScope();
                if (!(scope instanceof PsiMethod)) return true;
                return processor.process((PsiMethod)scope, context);
            }

            public boolean accepts( final PsiParameter t, final ProcessingContext context) {
                if (!super.accepts(t, context)) return false;
                final PsiMethod psiMethod = (PsiMethod)t.getDeclarationScope();

                final PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
                if (index < 0 || index >= parameters.length || !t.equals(parameters[index])) return false;
                return true;
            }
        });
    }
}
