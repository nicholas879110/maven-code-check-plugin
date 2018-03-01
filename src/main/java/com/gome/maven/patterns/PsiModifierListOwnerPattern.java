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
package com.gome.maven.patterns;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.psi.PsiModifierList;
import com.gome.maven.psi.PsiModifierListOwner;
import com.gome.maven.util.ProcessingContext;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.codeInsight.AnnotationUtil;

/**
 * @author peter
 */
public class PsiModifierListOwnerPattern<T extends PsiModifierListOwner, Self extends PsiModifierListOwnerPattern<T,Self>> extends PsiElementPattern<T,Self> {
    public PsiModifierListOwnerPattern( final InitialPatternCondition<T> condition) {
        super(condition);
    }

    protected PsiModifierListOwnerPattern(final Class<T> aClass) {
        super(aClass);
    }

    public Self withModifiers(final String... modifiers) {
        return with(new PatternCondition<T>("withModifiers") {
            public boolean accepts( final T t, final ProcessingContext context) {
                return ContainerUtil.and(modifiers, new Condition<String>() {
                    public boolean value(final String s) {
                        return t.hasModifierProperty(s);
                    }
                });
            }
        });
    }

    public Self withoutModifiers(final String... modifiers) {
        return with(new PatternCondition<T>("withoutModifiers") {
            public boolean accepts( final T t, final ProcessingContext context) {
                return ContainerUtil.and(modifiers, new Condition<String>() {
                    public boolean value(final String s) {
                        return !t.hasModifierProperty(s);
                    }
                });
            }
        });
    }

    public Self withAnnotation( final String qualifiedName) {
        return with(new PatternCondition<T>("withAnnotation") {
            public boolean accepts( final T t, final ProcessingContext context) {
                final PsiModifierList modifierList = t.getModifierList();
                return modifierList != null && modifierList.findAnnotation(qualifiedName) != null;
            }
        });
    }

    public Self withAnnotations( final String... qualifiedNames) {
        return with(new PatternCondition<T>("withAnnotations") {
            public boolean accepts( final T t, final ProcessingContext context) {
                return AnnotationUtil.findAnnotation(t, qualifiedNames) != null;
            }
        });
    }

    public static class Capture<T extends PsiModifierListOwner> extends PsiModifierListOwnerPattern<T, Capture<T>> {
        public Capture( InitialPatternCondition<T> condition) {
            super(condition);
        }
    }
}
