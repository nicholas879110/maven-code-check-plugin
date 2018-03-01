
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
package com.gome.maven.codeInsight.guess;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiType;

import java.util.Map;

public abstract class GuessManager {
    public static GuessManager getInstance(Project project) {
        return ServiceManager.getService(project, GuessManager.class);
    }

    public abstract PsiType[] guessContainerElementType(PsiExpression containerExpr, TextRange rangeToIgnore);

    public abstract PsiType[] guessTypeToCast(PsiExpression expr);

    
    public abstract Map<PsiExpression, PsiType> getControlFlowExpressionTypes( PsiExpression forPlace);

    
    public abstract PsiType getControlFlowExpressionType( PsiExpression expr);
}