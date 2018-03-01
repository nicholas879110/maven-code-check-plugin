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
package com.gome.maven.codeInspection.dataFlow;

import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.searches.MethodReferencesSearch;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.PsiUtilCore;

/**
 * @author peter
 */
public class InferenceFromSourceUtil {
    static boolean shouldInferFromSource( PsiMethod method) {
        if (isLibraryCode(method) ||
                method.hasModifierProperty(PsiModifier.ABSTRACT) ||
                PsiUtil.canBeOverriden(method) ||
                method.getBody() == null) {
            return false;
        }

        if (method.hasModifierProperty(PsiModifier.STATIC)) return true;

        return !isUnusedInAnonymousClass(method);
    }

    private static boolean isUnusedInAnonymousClass( PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        return containingClass instanceof PsiAnonymousClass &&
                MethodReferencesSearch.search(method, new LocalSearchScope(containingClass), false).findFirst() == null;
    }

    private static boolean isLibraryCode( PsiMethod method) {
        if (method instanceof PsiCompiledElement) return true;
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(method);
        return virtualFile != null && FileIndexFacade.getInstance(method.getProject()).isInLibrarySource(virtualFile);
    }

    static boolean isReturnTypeCompatible( PsiType returnType,  MethodContract.ValueConstraint returnValue) {
        if (returnValue == MethodContract.ValueConstraint.ANY_VALUE || returnValue == MethodContract.ValueConstraint.THROW_EXCEPTION) {
            return true;
        }
        if (PsiType.VOID.equals(returnType)) return false;

        if (PsiType.BOOLEAN.equals(returnType)) {
            return returnValue == MethodContract.ValueConstraint.TRUE_VALUE ||
                    returnValue == MethodContract.ValueConstraint.FALSE_VALUE;
        }

        if (!(returnType instanceof PsiPrimitiveType)) {
            return returnValue == MethodContract.ValueConstraint.NULL_VALUE ||
                    returnValue == MethodContract.ValueConstraint.NOT_NULL_VALUE;
        }

        return false;
    }

    static boolean suppressNullable(PsiMethod method) {
        if (method.getParameterList().getParametersCount() == 0) return false;

        for (MethodContract contract : ControlFlowAnalyzer.getMethodContracts(method)) {
            if (contract.returnValue == MethodContract.ValueConstraint.NULL_VALUE) {
                return true;
            }
        }
        return false;
    }
}
