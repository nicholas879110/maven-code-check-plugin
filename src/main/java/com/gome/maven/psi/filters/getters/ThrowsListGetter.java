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
package com.gome.maven.psi.filters.getters;

import com.gome.maven.codeInsight.completion.CompletionContext;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiType;
import com.gome.maven.psi.filters.ContextGetter;
import com.gome.maven.psi.util.PsiTreeUtil;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 20.03.2003
 * Time: 21:29:59
 * To change this template use Options | File Templates.
 */
public class ThrowsListGetter implements ContextGetter{
    @Override
    public PsiType[] get(PsiElement context, CompletionContext completionContext){
        final PsiMethod method = PsiTreeUtil.getContextOfType(context, PsiMethod.class, true);
        if(method != null){
            return method.getThrowsList().getReferencedTypes();
        }
        return PsiType.EMPTY_ARRAY;
    }
}