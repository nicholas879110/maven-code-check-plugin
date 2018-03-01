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
package com.gome.maven.util.xml;

import com.gome.maven.codeInsight.completion.scope.JavaCompletionProcessor;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.gome.maven.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.gome.maven.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author peter
 */
public class CanonicalPsiTypeConverterImpl extends CanonicalPsiTypeConverter implements CustomReferenceConverter<PsiType> {
     static final String[] PRIMITIVES = {"boolean", "byte", "char", "double", "float", "int", "long", "short"};
     private static final String ARRAY_PREFIX = "[L";
    private static final JavaClassReferenceProvider CLASS_REFERENCE_PROVIDER = new JavaClassReferenceProvider();

    @Override
    public PsiType fromString(final String s, final ConvertContext context) {
        if (s == null) return null;
        try {
            return JavaPsiFacade.getInstance(context.getFile().getProject()).getElementFactory().createTypeFromText(s.replace('$', '.'), null);
        }
        catch (IncorrectOperationException e) {
            return null;
        }
    }

    @Override
    public String toString(final PsiType t, final ConvertContext context) {
        return t == null ? null : t.getCanonicalText();
    }

    @Override
    
    public PsiReference[] createReferences(final GenericDomValue<PsiType> genericDomValue, final PsiElement element, ConvertContext context) {
        final String typeText = genericDomValue.getStringValue();
        if (typeText == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        return getReferences(genericDomValue.getValue(), typeText, 0, element);
    }

    public PsiReference[] getReferences( PsiType type, String typeText, int startOffsetInText,  final PsiElement element) {
        final ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
        assert manipulator != null;
        String trimmed = typeText.trim();
        int offset = manipulator.getRangeInElement(element).getStartOffset() + startOffsetInText + typeText.indexOf(trimmed);
        if (trimmed.startsWith(ARRAY_PREFIX)) {
            offset += ARRAY_PREFIX.length();
            if (trimmed.endsWith(";")) {
                trimmed = trimmed.substring(ARRAY_PREFIX.length(), trimmed.length() - 1);
            } else {
                trimmed = trimmed.substring(ARRAY_PREFIX.length());
            }
        }

        if (type != null) {
            type = type.getDeepComponentType();
        }
        final boolean isPrimitiveType = type instanceof PsiPrimitiveType;

        return new JavaClassReferenceSet(trimmed, element, offset, false, CLASS_REFERENCE_PROVIDER) {
            @Override
            
            protected JavaClassReference createReference(int refIndex,  String subRefText,  TextRange textRange, boolean staticImport) {
                return new JavaClassReference(this, textRange, refIndex, subRefText, staticImport) {
                    @Override
                    public boolean isSoft() {
                        return true;
                    }

                    @Override
                    
                    public JavaResolveResult advancedResolve(final boolean incompleteCode) {
                        if (isPrimitiveType) {
                            return new CandidateInfo(element, PsiSubstitutor.EMPTY, false, false, element);
                        }

                        return super.advancedResolve(incompleteCode);
                    }

                    @Override
                    public void processVariants( final PsiScopeProcessor processor) {
                        if (processor instanceof JavaCompletionProcessor) {
                            ((JavaCompletionProcessor)processor).setCompletionElements(getVariants());
                        } else {
                            super.processVariants(processor);
                        }
                    }

                    @Override
                    
                    public Object[] getVariants() {
                        final Object[] variants = super.getVariants();
                        if (myIndex == 0) {
                            return ArrayUtil.mergeArrays(variants, PRIMITIVES, ArrayUtil.OBJECT_ARRAY_FACTORY);
                        }
                        return variants;
                    }
                };
            }
        }.getAllReferences();
    }
}
