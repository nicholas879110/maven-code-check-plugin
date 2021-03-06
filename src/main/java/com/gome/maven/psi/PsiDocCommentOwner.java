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
package com.gome.maven.psi;

import com.gome.maven.psi.javadoc.PsiDocComment;

/**
 * Represents a PSI element which can have an attached JavaDoc comment.
 */
public interface PsiDocCommentOwner extends PsiMember {
    /**
     * Returns the JavaDoc comment for the element.
     *
     * @return the JavaDoc comment instance, or null if the element has no JavaDoc comment.
     */

    PsiDocComment getDocComment();

    /**
     * Checks if the element is marked as deprecated via an annotation or JavaDoc tag.
     *
     * @return true is the element is marked as deprecated, false otherwise.
     */
    boolean isDeprecated();
}
