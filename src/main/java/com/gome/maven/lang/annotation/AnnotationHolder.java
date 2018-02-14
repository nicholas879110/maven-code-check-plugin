/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.lang.annotation;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;

/**
 * Allows a custom language plugin to define annotations for files in that language.
 *
 * @author max
 * @see Annotator#annotate(com.gome.maven.psi.PsiElement, AnnotationHolder)
 */
public interface AnnotationHolder {
    /**
     * Creates an error annotation with the specified message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the error message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createErrorAnnotation( PsiElement elt,  String message);

    /**
     * Creates an error annotation with the specified message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the error message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createErrorAnnotation( ASTNode node,  String message);

    /**
     * Creates an error annotation with the specified message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the error message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createErrorAnnotation( TextRange range,  String message);

    /**
     * Creates a warning annotation with the specified message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the warning message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createWarningAnnotation( PsiElement elt,  String message);

    /**
     * Creates a warning annotation with the specified message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the warning message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createWarningAnnotation( ASTNode node,  String message);

    /**
     * Creates a warning annotation with the specified message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the warning message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createWarningAnnotation( TextRange range,  String message);

    /**
     * Creates an annotation with severity {@link HighlightSeverity#WEAK_WARNING} ('weak warning') with the specified
     * message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the info message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createWeakWarningAnnotation( PsiElement elt,  String message);

    /**
     * Creates an annotation with severity {@link HighlightSeverity#WEAK_WARNING} ('weak warning') with the specified
     * message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the info message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createWeakWarningAnnotation( ASTNode node,  String message);

    /**
     * Creates an annotation with severity {@link HighlightSeverity#WEAK_WARNING} ('weak warning') with the specified
     * message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the info message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createWeakWarningAnnotation( TextRange range,  String message);

    /**
     * Creates an information annotation (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified PSI element.
     *
     * @param elt     the element over which the annotation is created.
     * @param message the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createInfoAnnotation( PsiElement elt,  String message);

    /**
     * Creates an information annotation (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified AST node.
     *
     * @param node    the node over which the annotation is created.
     * @param message the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createInfoAnnotation( ASTNode node,  String message);

    /**
     * Creates an information annotation (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation)with the specified message over the specified text range.
     *
     * @param range   the text range over which the annotation is created.
     * @param message the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createInfoAnnotation( TextRange range,  String message);

    /**
     * Creates an annotation with the given severity (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message over the specified text range.
     *
     * @param severity the severity.
     * @param range    the text range over which the annotation is created.
     * @param message  the information message.
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createAnnotation( HighlightSeverity severity,  TextRange range,  String message);

    /**
     * Creates an annotation with the given severity (colored highlighting only, with no gutter mark and not participating in
     * "Next Error/Warning" navigation) with the specified message and tooltip markup over the specified text range.
     *
     * @param severity the severity.
     * @param range    the text range over which the annotation is created.
     * @param message  the information message.
     * @param htmlTooltip  the tooltip to show (usually the message, but escaped as HTML and surrounded by a {@code <html>} tag
     * @return the annotation (which can be modified to set additional annotation parameters)
     */
    Annotation createAnnotation( HighlightSeverity severity,  TextRange range,  String message,
                                 String htmlTooltip);

    
    AnnotationSession getCurrentAnnotationSession();

    boolean isBatchMode();
}
