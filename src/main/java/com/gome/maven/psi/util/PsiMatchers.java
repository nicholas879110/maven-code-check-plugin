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

/*
 * @author max
 */
package com.gome.maven.psi.util;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.util.ArrayUtil;

public class PsiMatchers {

    private PsiMatchers() {
    }

    
    public static PsiMatcherExpression hasName( final String name) {
        return new PsiMatcherExpression() {
            @Override
            public Boolean match(PsiElement element) {
                if (element instanceof PsiNamedElement && name.equals(((PsiNamedElement) element).getName())) return Boolean.TRUE;
                return Boolean.FALSE;
            }
        };
    }

    
    public static PsiMatcherExpression hasText( final String text) {
        return new PsiMatcherExpression() {
            @Override
            public Boolean match(PsiElement element) {
                if (element.getTextLength() != text.length()) return Boolean.FALSE;
                return text.equals(element.getText());
            }
        };
    }

    
    public static PsiMatcherExpression hasText( final String... texts) {
        return new PsiMatcherExpression() {
            @Override
            public Boolean match(PsiElement element) {
                String text = element.getText();
                return ArrayUtil.find(texts, text) != -1;
            }
        };
    }

    
    public static PsiMatcherExpression hasClass( final Class<?> aClass) {
        return new PsiMatcherExpression() {
            @Override
            public Boolean match(PsiElement element) {
                if (aClass.isAssignableFrom(element.getClass())) return Boolean.TRUE;
                return Boolean.FALSE;
            }
        };
    }

    
    public static PsiMatcherExpression hasClass( final Class... classes) {
        return new PsiMatcherExpression() {
            @Override
            public Boolean match(PsiElement element) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(element.getClass())) return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        };
    }
}
