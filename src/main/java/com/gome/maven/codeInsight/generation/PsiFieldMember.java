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

import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiField;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiSubstitutor;
import com.gome.maven.psi.util.PsiFormatUtil;
import com.gome.maven.psi.util.PsiFormatUtilBase;
import com.gome.maven.util.IncorrectOperationException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public class PsiFieldMember extends PsiElementClassMember<PsiField> implements PropertyClassMember {
    private static final int FIELD_OPTIONS = PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_TYPE | PsiFormatUtilBase.TYPE_AFTER;

    public PsiFieldMember( PsiField field) {
        super(field, PsiFormatUtil.formatVariable(field, FIELD_OPTIONS, PsiSubstitutor.EMPTY));
    }

    public PsiFieldMember( PsiField psiMember, PsiSubstitutor substitutor) {
        super(psiMember, substitutor, PsiFormatUtil.formatVariable(psiMember, FIELD_OPTIONS, PsiSubstitutor.EMPTY));
    }

    
    @Override
    public GenerationInfo generateGetter() throws IncorrectOperationException {
        final GenerationInfo[] infos = generateGetters(getElement().getContainingClass());
        return infos != null && infos.length > 0 ? infos[0] : null;
    }

    
    @Override
    public GenerationInfo[] generateGetters(PsiClass aClass) throws IncorrectOperationException {
        return createGenerateInfos(aClass, GetterSetterPrototypeProvider.generateGetterSetters(getElement(), true, false));
    }

    
    @Override
    public GenerationInfo generateSetter() throws IncorrectOperationException {
        final GenerationInfo[] infos = generateSetters(getElement().getContainingClass());
        return infos != null && infos.length > 0 ? infos[0] : null;
    }

    @Override
    
    public GenerationInfo[] generateSetters(PsiClass aClass) {
        final PsiField field = getElement();
        if (GetterSetterPrototypeProvider.isReadOnlyProperty(field)) {
            return null;
        }
        return createGenerateInfos(aClass, GetterSetterPrototypeProvider.generateGetterSetters(field, false, false));
    }

    private static GenerationInfo[] createGenerateInfos(PsiClass aClass, PsiMethod[] prototypes) {
        final List<GenerationInfo> methods = new ArrayList<GenerationInfo>();
        for (PsiMethod prototype : prototypes) {
            final PsiMethod method = createMethodIfNotExists(aClass, prototype);
            if (method != null) {
                methods.add(new PsiGenerationInfo(method));
            }
        }
        return methods.isEmpty() ? null : methods.toArray(new GenerationInfo[methods.size()]);
    }

    
    private static PsiMethod createMethodIfNotExists(PsiClass aClass, final PsiMethod template) {
        PsiMethod existing = aClass.findMethodBySignature(template, false);
        return existing == null ? template : null;
    }
}
