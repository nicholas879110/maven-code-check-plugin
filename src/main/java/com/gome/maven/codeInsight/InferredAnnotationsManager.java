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
package com.gome.maven.codeInsight;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.psi.PsiAnnotation;
import com.gome.maven.psi.PsiModifierListOwner;

/**
 * Returns annotations inferred by bytecode or source code, for example contracts and nullity.
 *
 * @see NullableNotNullManager
 * @see Contract
 * @see Nullable
 * @see NotNull
 * @see AnnotationUtil
 */
public abstract class InferredAnnotationsManager {
    private static final NotNullLazyKey<InferredAnnotationsManager, Project> INSTANCE_KEY = ServiceManager.createLazyKey(InferredAnnotationsManager.class);

    public static InferredAnnotationsManager getInstance( Project project) {
        return INSTANCE_KEY.getValue(project);
    }

    /**
     * @return if exists, an inferred annotation by given qualified name on a given PSI element. Several invocations may return several
     * different instances of {@link PsiAnnotation}, which are not guaranteed to be equal.
     */
    
    public abstract PsiAnnotation findInferredAnnotation( PsiModifierListOwner listOwner,  String annotationFQN);

    /**
     * There is a number of well-known methods where automatic inference fails (for example, {@link java.util.Objects#requireNonNull(Object)}.
     * For such methods, contracts are hardcoded, and for their parameters inferred  are suppressed.<p/>
     *
     * {@link Contract} and {@link NotNull} annotations on methods are not necessarily applicable to the overridden implementations, so they're ignored, too.<p/>
     *
     * @return whether inference is to be suppressed the given annotation on the given method or parameter
     */
    public abstract boolean ignoreInference( PsiModifierListOwner owner,  String annotationFQN);

    /**
     * When annotation name is known, prefer {@link #findInferredAnnotation(PsiModifierListOwner, String)} as
     * potentially faster.
     *
     * @return all inferred annotations for the given element
     */
    
    public abstract PsiAnnotation[] findInferredAnnotations( PsiModifierListOwner listOwner);

    /**
     * @return whether the given annotation was inferred by this service.
     *
     * @see AnnotationUtil#isInferredAnnotation(PsiAnnotation)
     */
    public abstract boolean isInferredAnnotation( PsiAnnotation annotation);
}
