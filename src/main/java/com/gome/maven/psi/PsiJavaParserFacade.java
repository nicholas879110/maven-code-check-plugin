/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.javadoc.PsiDocTag;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author max
 */
public interface PsiJavaParserFacade {
    /**
     * Creates a JavaDoc tag from the specified text.
     *
     * @param docTagText the text of the JavaDoc tag.
     * @return the created tag.
     * @throws com.gome.maven.util.IncorrectOperationException if the text of the tag is not valid.
     */
    
    PsiDocTag createDocTagFromText( String docTagText) throws IncorrectOperationException;

    /**
     * Creates a JavaDoc comment from the specified text.
     *
     * @param docCommentText the text of the JavaDoc comment.
     * @return the created comment.
     * @throws com.gome.maven.util.IncorrectOperationException if the text of the comment is not valid.
     */
    
    PsiDocComment createDocCommentFromText( String docCommentText) throws IncorrectOperationException;

    /**
     * Creates a Java class with a dummy name from the specified body text (the text between the braces).
     *
     * @param text    the body text of the class to create.
     * @param context the PSI element used as context for resolving references which cannot be resolved
     *                within the class.
     * @return the created class instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text is not a valid class body.
     */
    
    PsiClass createClassFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java field from the specified text.
     *
     * @param text    the text of the field to create.
     * @param context the PSI element used as context for resolving references from the field.
     * @return the created field instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text is not a valid field body.
     */
    
    PsiField createFieldFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java method from the specified text with the specified language level.
     *
     * @param text          the text of the method to create.
     * @param context       the PSI element used as context for resolving references from the method.
     * @param languageLevel the language level used for creating the method.
     * @return the created method instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text is not a valid method body.
     */
    
    PsiMethod createMethodFromText(  String text,  PsiElement context, LanguageLevel languageLevel) throws IncorrectOperationException;

    /**
     * Creates a Java method from the specified text.
     *
     * @param text    the text of the method to create.
     * @param context the PSI element used as context for resolving references from the method.
     * @return the created method instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text is not a valid method body.
     */
    
    PsiMethod createMethodFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java parameter from the specified text.
     *
     * @param text    the text of the parameter to create.
     * @param context the PSI element used as context for resolving references from the parameter.
     * @return the created parameter instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text is not a valid parameter body.
     */
    
    PsiParameter createParameterFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java try-resource from the specified text.
     *
     * @param text    the text of the resource to create.
     * @param context the PSI element used as context for resolving references from the resource.
     * @return the created resource instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text is not a valid resource definition.
     */
    
    PsiResourceVariable createResourceFromText( String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java type from the specified text.
     *
     * @param text    the text of the type to create (for example, a primitive type keyword, an array
     *                declaration or the name of a class).
     * @param context the PSI element used as context for resolving the reference.
     * @return the created type instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid type.
     */
    
    PsiType createTypeFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java type element from the specified text.
     *
     * @param text    the text of the type to create (for example, a primitive type keyword, an array
     *                declaration or the name of a class).
     * @param context the PSI element used as context for resolving the reference.
     * @return the created type element.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid type.
     */
    
    PsiTypeElement createTypeElementFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java code reference from the specified text.
     *
     * @param text    the text of the type to create (for example, a primitive type keyword, an array
     *                declaration or the name of a class).
     * @param context the PSI element used as context for resolving the reference.
     * @return the created reference element.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid type.
     */
    
    PsiJavaCodeReferenceElement createReferenceFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java code block from the specified text.
     *
     * @param text    the text of the code block to create.
     * @param context the PSI element used as context for resolving references from the block.
     * @return the created code block instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid code block.
     */
    
    PsiCodeBlock createCodeBlockFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java statement from the specified text.
     *
     * @param text    the text of the statement to create.
     * @param context the PSI element used as context for resolving references from the statement.
     * @return the created statement instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid statement.
     */
    
    PsiStatement createStatementFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java expression from the specified text.
     *
     * @param text    the text of the expression to create.
     * @param context the PSI element used as context for resolving references from the expression.
     * @return the created expression instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid expression.
     */
    
    PsiExpression createExpressionFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java comment from the specified text.
     *
     * @param text    the text of the comment to create.
     * @param context the PSI element used as context for resolving references from the comment.
     * @return the created comment instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid comment.
     */
    
    PsiComment createCommentFromText( String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a type parameter from the specified text.
     *
     * @param text    the text of the type parameter to create.
     * @param context the context for resolving references.
     * @return the created type parameter instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid type parameter.
     */
    
    PsiTypeParameter createTypeParameterFromText(  String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates an annotation from the specified text.
     *
     * @param annotationText the text of the annotation to create.
     * @param context        the context for resolving references from the annotation.
     * @return the created annotation instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid annotation.
     */
    
    PsiAnnotation createAnnotationFromText(  String annotationText,  PsiElement context) throws IncorrectOperationException;

    
    PsiEnumConstant createEnumConstantFromText( String text,  PsiElement context) throws IncorrectOperationException;

    /**
     * Creates a Java type from the specified text.
     *
     * @param text        the text of the type to create (a primitive type keyword).
     * @param annotations array (possible empty) of annotations to annotate the created type.
     * @return the created type instance.
     * @throws IncorrectOperationException if some of the parameters are not valid.
     */
    
    PsiType createPrimitiveType( String text,  PsiAnnotation[] annotations) throws IncorrectOperationException;
}
