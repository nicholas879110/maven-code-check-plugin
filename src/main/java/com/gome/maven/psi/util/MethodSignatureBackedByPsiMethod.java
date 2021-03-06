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
package com.gome.maven.psi.util;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;

public class MethodSignatureBackedByPsiMethod extends MethodSignatureBase {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.util.MethodSignatureBackedByPsiMethod");

    private final PsiMethod myMethod;
    private final boolean myIsRaw;
    private final String myName;

    protected MethodSignatureBackedByPsiMethod( PsiMethod method,
                                                PsiSubstitutor substitutor,
                                               boolean isRaw,
                                                PsiType[] parameterTypes,
                                                PsiTypeParameter[] methodTypeParameters) {
        super(substitutor, parameterTypes, methodTypeParameters);
        myIsRaw = isRaw;
        myMethod = method;
        myName = method.getName();
    }

    
    @Override
    public String getName() {
        return myName;
    }

    @Override
    public boolean isRaw() {
        return myIsRaw;
    }

    @Override
    public boolean isConstructor() {
        return myMethod.isConstructor();
    }

    public boolean equals(Object o) {
        if (o instanceof MethodSignatureBackedByPsiMethod){ // optimization
            if (((MethodSignatureBackedByPsiMethod)o).myMethod == myMethod) return true;
        }

        return super.equals(o);
    }

    
    public PsiMethod getMethod() {
        return myMethod;
    }

    
    public static MethodSignatureBackedByPsiMethod create( PsiMethod method,  PsiSubstitutor substitutor) {
        return create(method, substitutor, PsiUtil.isRawSubstitutor(method, substitutor));
    }

    
    public static MethodSignatureBackedByPsiMethod create( PsiMethod method,  PsiSubstitutor substitutor, boolean isRaw) {
        PsiTypeParameter[] methodTypeParameters = method.getTypeParameters();
        if (isRaw) {
            substitutor = JavaPsiFacade.getInstance(method.getProject()).getElementFactory().createRawSubstitutor(substitutor, methodTypeParameters);
            methodTypeParameters = PsiTypeParameter.EMPTY_ARRAY;
        }

        assert substitutor.isValid();

        final PsiParameter[] parameters = method.getParameterList().getParameters();
        PsiType[] parameterTypes = PsiType.createArray(parameters.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            PsiParameter parameter = parameters[i];
            PsiType type = parameter.getType();
            parameterTypes[i] = isRaw ? TypeConversionUtil.erasure(substitutor.substitute(type)) : type;
            if (!parameterTypes[i].isValid()) {
                PsiUtil.ensureValidType(parameterTypes[i], "Method " + method + " of " + method.getClass() + "; param " + parameter + " of " + parameter.getClass());
            }
        }

        return new MethodSignatureBackedByPsiMethod(method, substitutor, isRaw, parameterTypes, methodTypeParameters);
    }
}
