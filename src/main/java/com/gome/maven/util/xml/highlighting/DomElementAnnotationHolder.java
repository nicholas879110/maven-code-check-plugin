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
package com.gome.maven.util.xml.highlighting;

import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemHighlightType;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.GenericDomValue;
import com.gome.maven.util.xml.reflect.DomCollectionChildDescription;

public interface DomElementAnnotationHolder extends Iterable<DomElementProblemDescriptor>{

    boolean isOnTheFly();

    
    DomElementProblemDescriptor createProblem( DomElement domElement,  String message, LocalQuickFix... fixes);

    
    DomElementProblemDescriptor createProblem( DomElement domElement, DomCollectionChildDescription childDescription,  String message);

    
    DomElementProblemDescriptor createProblem( DomElement domElement, HighlightSeverity highlightType, String message);

    DomElementProblemDescriptor createProblem( DomElement domElement, HighlightSeverity highlightType, String message, LocalQuickFix... fixes);

    DomElementProblemDescriptor createProblem( DomElement domElement, HighlightSeverity highlightType, String message, TextRange textRange, LocalQuickFix... fixes);

    DomElementProblemDescriptor createProblem( DomElement domElement, ProblemHighlightType highlightType, String message,  TextRange textRange, LocalQuickFix... fixes);

    
    DomElementResolveProblemDescriptor createResolveProblem( GenericDomValue element,  PsiReference reference);

    /**
     * Is useful only if called from {@link com.gome.maven.util.xml.highlighting.DomElementsAnnotator} instance
     * @param element element
     * @param severity highlight severity
     * @param message description
     * @return annotation
     */
    
    Annotation createAnnotation( DomElement element, HighlightSeverity severity,  String message);

    int getSize();
}
