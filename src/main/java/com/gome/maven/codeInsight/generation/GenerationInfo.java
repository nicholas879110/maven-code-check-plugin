/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiMember;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author Medvedev Max
 */
public interface GenerationInfo {
    GenerationInfo[] EMPTY_ARRAY = new GenerationInfo[0];

    void insert(PsiClass aClass, PsiElement anchor, boolean before) throws IncorrectOperationException;

    PsiMember getPsiMember();

    /**
     * @param aClass
     * @param leaf leaf element. Is guaranteed to be a tree descendant of aClass.
     * @return the value that will be passed to the {@link #insert(com.gome.maven.psi.PsiClass, com.gome.maven.psi.PsiElement, boolean)} method later.
     */
    
    PsiElement findInsertionAnchor( PsiClass aClass,  PsiElement leaf);

    /**
     * Position caret in generated element in correct way
     */
    void positionCaret(Editor editor, boolean toEditMethodBody);
}