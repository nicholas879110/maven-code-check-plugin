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
package com.gome.maven.codeInsight.generation;

import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiDocCommentOwner;
import com.gome.maven.psi.PsiSubstitutor;
import com.gome.maven.psi.util.PsiFormatUtil;
import com.gome.maven.psi.util.PsiFormatUtilBase;

/**
 * @author peter
 */
public abstract class PsiElementClassMember<T extends PsiDocCommentOwner> extends PsiDocCommentOwnerMemberChooserObject implements ClassMemberWithElement {
    private final T myPsiMember;
    private PsiSubstitutor mySubstitutor;

    protected PsiElementClassMember( T psiMember, String text) {
        this(psiMember, PsiSubstitutor.EMPTY, text);
    }

    protected PsiElementClassMember( T psiMember,  PsiSubstitutor substitutor, String text) {
        super(psiMember, text, psiMember.getIcon(Iconable.ICON_FLAG_VISIBILITY));
        myPsiMember = psiMember;
        mySubstitutor = substitutor;
    }

    @Override
    
    public T getElement() {
        return myPsiMember;
    }

    public PsiSubstitutor getSubstitutor() {
        return mySubstitutor;
    }

    public void setSubstitutor(PsiSubstitutor substitutor) {
        mySubstitutor = substitutor;
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        final PsiClass psiClass = getContainingClass();
        final String text = PsiFormatUtil.formatClass(psiClass, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_FQ_NAME);
        return new PsiDocCommentOwnerMemberChooserObject(psiClass, text, psiClass.getIcon(0));
    }

    protected PsiClass getContainingClass() {
        return myPsiMember.getContainingClass();
    }
}