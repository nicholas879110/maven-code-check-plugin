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
package com.gome.maven.util.xml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specifies that the annotated method return value ({@link DomElement}) should have
 * a non-null XML element under itself. And some other connected properties.
 *
 * @author peter
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
    /**
     * @return whether the annotated method return value ({@link DomElement}) should have
     * a non-null XML element under itself.
     */
    boolean value() default true;

    /**
     * @return whether the annotated method return value ({@link com.gome.maven.util.xml.GenericDomValue})
     * should have non-empty value: {@link GenericDomValue#getStringValue()} != null
     */
    boolean nonEmpty() default true;

    /**
     * @return whether the annotated method return value ({@link com.gome.maven.util.xml.GenericDomValue})
     * string value should be identifier: {@link GenericDomValue#getStringValue()}.
     *
     * @see com.gome.maven.psi.PsiNameHelper#isIdentifier(String)
     */
    boolean identifier() default false;
}
