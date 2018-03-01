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
package com.gome.maven.refactoring.util.occurrences;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiExpressionList;
import com.gome.maven.psi.PsiMethodCallExpression;

/**
 * @author dsl
 */
public abstract class NotInSuperOrThisCallFilterBase implements OccurrenceFilter {
    public boolean isOK(PsiExpression occurrence) {
        PsiElement parent = occurrence.getParent();
        while(parent instanceof PsiExpression) {
            parent = parent.getParent();
        }
        if(!(parent instanceof PsiExpressionList)) return true;
        parent = parent.getParent();
        if(!(parent instanceof PsiMethodCallExpression)) return true;
        final String text = ((PsiMethodCallExpression) parent).getMethodExpression().getText();
        return !getKeywordText().equals(text);
    }

    protected abstract  String getKeywordText();
}
