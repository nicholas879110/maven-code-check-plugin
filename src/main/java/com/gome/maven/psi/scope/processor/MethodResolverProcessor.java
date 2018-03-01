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
package com.gome.maven.psi.scope.processor;

import com.gome.maven.psi.*;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.scope.JavaScopeProcessorEvent;
import com.gome.maven.psi.scope.PsiConflictResolver;
import com.gome.maven.psi.scope.conflictResolvers.JavaMethodsConflictResolver;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.SmartList;

public class MethodResolverProcessor extends MethodCandidatesProcessor {
    private boolean myStopAcceptingCandidates = false;

    public MethodResolverProcessor( PsiMethodCallExpression place,  PsiFile placeFile) {
        this(place, place.getArgumentList(), placeFile);
    }

    public MethodResolverProcessor( PsiCallExpression place,
                                    PsiExpressionList argumentList,
                                    PsiFile placeFile){
        this(place, placeFile, new PsiConflictResolver[]{new JavaMethodsConflictResolver(argumentList, PsiUtil.getLanguageLevel(placeFile))});
        setArgumentList(argumentList);
        obtainTypeArguments(place);
    }

    public MethodResolverProcessor(PsiClass classConstr,  PsiExpressionList argumentList,  PsiElement place,  PsiFile placeFile) {
        super(place, placeFile, new PsiConflictResolver[]{new JavaMethodsConflictResolver(argumentList,
                PsiUtil.getLanguageLevel(placeFile))}, new SmartList<CandidateInfo>());
        setIsConstructor(true);
        setAccessClass(classConstr);
        setArgumentList(argumentList);
    }

    public MethodResolverProcessor( PsiElement place,  PsiFile placeFile,  PsiConflictResolver[] resolvers) {
        super(place, placeFile, resolvers, new SmartList<CandidateInfo>());
    }

    @Override
    public void handleEvent( Event event, Object associated) {
        if (event == JavaScopeProcessorEvent.CHANGE_LEVEL) {
            if (myHasAccessibleStaticCorrectCandidate) myStopAcceptingCandidates = true;
        }
        super.handleEvent(event, associated);
    }

    @Override
    public boolean execute( PsiElement element,  ResolveState state) {
        return !myStopAcceptingCandidates && super.execute(element, state);
    }

    @Override
    protected boolean acceptVarargs() {
        return true;
    }
}
