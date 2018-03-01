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

import com.gome.maven.codeInsight.CodeInsightUtil;
import com.gome.maven.codeInsight.completion.JavaSmartCompletionParameters;
import com.gome.maven.codeInsight.completion.PrefixMatcher;
import com.gome.maven.codeInsight.lookup.AutoCompletionPolicy;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.InheritanceUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.IncorrectOperationException;

public class ClassLiteralGetter {

    public static void addCompletions( final JavaSmartCompletionParameters parameters,
                                       Consumer<LookupElement> result, final PrefixMatcher matcher) {
        PsiType expectedType = parameters.getDefaultType();
        if (!InheritanceUtil.isInheritor(expectedType, CommonClassNames.JAVA_LANG_CLASS)) {
            expectedType = parameters.getExpectedType();
            if (!InheritanceUtil.isInheritor(expectedType, CommonClassNames.JAVA_LANG_CLASS)) {
                return;
            }
        }

        PsiType classParameter = PsiUtil.substituteTypeParameter(expectedType, CommonClassNames.JAVA_LANG_CLASS, 0, false);

        boolean addInheritors = false;
        PsiElement position = parameters.getPosition();
        if (classParameter instanceof PsiWildcardType) {
            final PsiWildcardType wildcardType = (PsiWildcardType)classParameter;
            classParameter = wildcardType.isSuper() ? wildcardType.getSuperBound() : wildcardType.getExtendsBound();
            addInheritors = wildcardType.isExtends() && classParameter instanceof PsiClassType;
        } else if (!matcher.getPrefix().isEmpty()) {
            addInheritors = true;
            classParameter = PsiType.getJavaLangObject(position.getManager(), position.getResolveScope());
        }
        if (classParameter != null) {
            PsiFile file = position.getContainingFile();
            addClassLiteralLookupElement(classParameter, result, file);
            if (addInheritors) {
                addInheritorClassLiterals(file, classParameter, result, matcher);
            }
        }
    }

    private static void addInheritorClassLiterals(final PsiFile context,
                                                  final PsiType classParameter,
                                                  final Consumer<LookupElement> result, PrefixMatcher matcher) {
        final String canonicalText = classParameter.getCanonicalText();
        if (CommonClassNames.JAVA_LANG_OBJECT.equals(canonicalText) && StringUtil.isEmpty(matcher.getPrefix())) {
            return;
        }

        CodeInsightUtil.processSubTypes(classParameter, context, true, matcher, new Consumer<PsiType>() {
            @Override
            public void consume(PsiType type) {
                addClassLiteralLookupElement(type, result, context);
            }
        });
    }

    private static void addClassLiteralLookupElement( final PsiType type, final Consumer<LookupElement> resultSet, final PsiFile context) {
        if (type instanceof PsiClassType &&
                PsiUtil.resolveClassInType(type) != null &&
                !((PsiClassType)type).hasParameters() &&
                !(((PsiClassType)type).resolve() instanceof PsiTypeParameter)) {
            try {
                resultSet.consume(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(new ClassLiteralLookupElement((PsiClassType)type, context)));
            }
            catch (IncorrectOperationException ignored) {
            }
        }
    }
}