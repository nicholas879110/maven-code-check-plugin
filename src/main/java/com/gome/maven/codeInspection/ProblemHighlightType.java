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
package com.gome.maven.codeInspection;

public enum ProblemHighlightType {

    /** Underlying highlighting with color depending on the inspection {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel} */
    GENERIC_ERROR_OR_WARNING,

    /** Changes font color depending on the inspection {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel} */
    LIKE_UNKNOWN_SYMBOL,

    LIKE_DEPRECATED,

    LIKE_UNUSED_SYMBOL,

    /** The same as {@link #LIKE_UNKNOWN_SYMBOL} with enforced {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel#ERROR} severity level */
    ERROR,

    /** The same as {@link #GENERIC_ERROR_OR_WARNING} with enforced {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel#ERROR} severity level */
    GENERIC_ERROR,

//    @Deprecated
    /** Enforces {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel#INFO} severity level
     * use #WEAK_WARNING instead*/
            INFO,

    /** Enforces {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel#WEAK_WARNING} severity level */
    WEAK_WARNING,

    /** Enforces {@link com.gome.maven.codeHighlighting.HighlightDisplayLevel#DO_NOT_SHOW} severity level */
    INFORMATION
}
