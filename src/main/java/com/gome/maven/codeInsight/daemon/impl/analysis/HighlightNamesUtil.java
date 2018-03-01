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

/**
 * @author cdr
 */
package com.gome.maven.codeInsight.daemon.impl.analysis;

import com.gome.maven.application.options.colors.ScopeAttributesUtil;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfoType;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.colors.TextAttributesScheme;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.packageDependencies.DependencyValidationManager;
import com.gome.maven.packageDependencies.DependencyValidationManagerImpl;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.ElementType;
import com.gome.maven.psi.impl.source.tree.TreeUtil;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;
import com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder;
import com.gome.maven.psi.search.scope.packageSet.PackageSet;
import com.gome.maven.psi.util.PsiTreeUtil;

import java.util.List;

public class HighlightNamesUtil {
    private static final Logger LOG = Logger.getInstance("#" + HighlightNamesUtil.class.getName());

    
    public static HighlightInfo highlightMethodName( PsiMethod method,
                                                    final PsiElement elementToHighlight,
                                                    final boolean isDeclaration,
                                                     TextAttributesScheme colorsScheme) {
        return highlightMethodName(method, elementToHighlight, elementToHighlight.getTextRange(), colorsScheme, isDeclaration);
    }

    
    public static HighlightInfo highlightMethodName( PsiMethod method,
                                                    final PsiElement elementToHighlight,
                                                    TextRange range,  TextAttributesScheme colorsScheme, final boolean isDeclaration) {
        boolean isInherited = false;

        if (!isDeclaration) {
            if (isCalledOnThis(elementToHighlight)) {
                final PsiClass containingClass = method.getContainingClass();
                PsiClass enclosingClass = containingClass == null ? null : PsiTreeUtil.getParentOfType(elementToHighlight, PsiClass.class);
                while (enclosingClass != null) {
                    isInherited = enclosingClass.isInheritor(containingClass, true);
                    if (isInherited) break;
                    enclosingClass = PsiTreeUtil.getParentOfType(enclosingClass, PsiClass.class, true);
                }
            }
        }

        HighlightInfoType type = getMethodNameHighlightType(method, isDeclaration, isInherited);
        if (type != null && elementToHighlight != null) {
            TextAttributes attributes = mergeWithScopeAttributes(method, type, colorsScheme);
            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(type).range(range);
            if (attributes != null) {
                builder.textAttributes(attributes);
            }
            return builder.createUnconditionally();
        }
        return null;
    }

    private static boolean isCalledOnThis(PsiElement elementToHighlight) {
        PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementToHighlight, PsiMethodCallExpression.class);
        if (methodCallExpression != null) {
            PsiElement qualifier = methodCallExpression.getMethodExpression().getQualifier();
            if (qualifier == null || qualifier instanceof PsiThisExpression) {
                return true;
            }
        }
        return false;
    }

    private static TextAttributes mergeWithScopeAttributes(final PsiElement element,
                                                            HighlightInfoType type,
                                                            TextAttributesScheme colorsScheme) {
        TextAttributes regularAttributes = HighlightInfo.getAttributesByType(element, type, colorsScheme);
        if (element == null) return regularAttributes;
        TextAttributes scopeAttributes = getScopeAttributes(element, colorsScheme);
        return TextAttributes.merge(scopeAttributes, regularAttributes);
    }

    
    public static HighlightInfo highlightClassName(PsiClass aClass, PsiElement elementToHighlight,  TextAttributesScheme colorsScheme) {
        HighlightInfoType type = getClassNameHighlightType(aClass, elementToHighlight);
        if (elementToHighlight != null) {
            TextAttributes attributes = mergeWithScopeAttributes(aClass, type, colorsScheme);
            TextRange range = elementToHighlight.getTextRange();
            if (elementToHighlight instanceof PsiJavaCodeReferenceElement) {
                final PsiJavaCodeReferenceElement referenceElement = (PsiJavaCodeReferenceElement)elementToHighlight;
                PsiReferenceParameterList parameterList = referenceElement.getParameterList();
                if (parameterList != null) {
                    final TextRange paramListRange = parameterList.getTextRange();
                    if (paramListRange.getEndOffset() > paramListRange.getStartOffset()) {
                        range = new TextRange(range.getStartOffset(), paramListRange.getStartOffset());
                    }
                }
            }

            // This will highlight @ sign in annotation as well.
            final PsiElement parent = elementToHighlight.getParent();
            if (parent instanceof PsiAnnotation) {
                final PsiAnnotation psiAnnotation = (PsiAnnotation)parent;
                range = new TextRange(psiAnnotation.getTextRange().getStartOffset(), range.getEndOffset());
            }

            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(type).range(range);
            if (attributes != null) {
                builder.textAttributes(attributes);
            }
            return builder.createUnconditionally();
        }
        return null;
    }

    
    public static HighlightInfo highlightVariableName(final PsiVariable variable,
                                                      final PsiElement elementToHighlight,
                                                       TextAttributesScheme colorsScheme) {
        HighlightInfoType varType = getVariableNameHighlightType(variable);
        if (varType != null) {
            if (variable instanceof PsiField) {
                TextAttributes attributes = mergeWithScopeAttributes(variable, varType, colorsScheme);
                HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(varType).range(elementToHighlight.getTextRange());
                if (attributes != null) {
                    builder.textAttributes(attributes);
                }
                return builder.createUnconditionally();
            }
            return HighlightInfo.newHighlightInfo(varType).range(elementToHighlight).create();
        }
        return null;
    }

    
    public static HighlightInfo highlightClassNameInQualifier(final PsiJavaCodeReferenceElement element,
                                                               TextAttributesScheme colorsScheme) {
        PsiElement qualifierExpression = element.getQualifier();
        if (qualifierExpression instanceof PsiJavaCodeReferenceElement) {
            PsiElement resolved = ((PsiJavaCodeReferenceElement)qualifierExpression).resolve();
            if (resolved instanceof PsiClass) {
                return highlightClassName((PsiClass)resolved, qualifierExpression, colorsScheme);
            }
        }
        return null;
    }

    private static HighlightInfoType getMethodNameHighlightType( PsiMethod method, boolean isDeclaration, boolean isInheritedMethod) {
        if (method.isConstructor()) {
            return isDeclaration ? HighlightInfoType.CONSTRUCTOR_DECLARATION : HighlightInfoType.CONSTRUCTOR_CALL;
        }
        if (isDeclaration) return HighlightInfoType.METHOD_DECLARATION;
        if (method.hasModifierProperty(PsiModifier.STATIC)) {
            return HighlightInfoType.STATIC_METHOD;
        }
        if (isInheritedMethod) return HighlightInfoType.INHERITED_METHOD;
        if(method.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return HighlightInfoType.ABSTRACT_METHOD;
        }
        return HighlightInfoType.METHOD_CALL;
    }

    
    private static HighlightInfoType getVariableNameHighlightType(PsiVariable var) {
        if (var instanceof PsiLocalVariable
                || var instanceof PsiParameter && ((PsiParameter)var).getDeclarationScope() instanceof PsiForeachStatement) {
            return HighlightInfoType.LOCAL_VARIABLE;
        }
        if (var instanceof PsiField) {
            return var.hasModifierProperty(PsiModifier.STATIC) ? var.hasModifierProperty(PsiModifier.FINAL)
                    ? HighlightInfoType.STATIC_FINAL_FIELD
                    : HighlightInfoType.STATIC_FIELD : HighlightInfoType.INSTANCE_FIELD;
        }
        if (var instanceof PsiParameter) {
            return HighlightInfoType.PARAMETER;
        }
        return null;
    }

    
    private static HighlightInfoType getClassNameHighlightType( PsiClass aClass,  PsiElement element) {
        if (element instanceof PsiJavaCodeReferenceElement && element.getParent() instanceof PsiAnonymousClass) {
            return HighlightInfoType.ANONYMOUS_CLASS_NAME;
        }
        if (aClass != null) {
            if (aClass.isAnnotationType()) return HighlightInfoType.ANNOTATION_NAME;
            if (aClass.isInterface()) return HighlightInfoType.INTERFACE_NAME;
            if (aClass.isEnum()) return HighlightInfoType.ENUM_NAME;
            if (aClass instanceof PsiTypeParameter) return HighlightInfoType.TYPE_PARAMETER_NAME;
            final PsiModifierList modList = aClass.getModifierList();
            if (modList != null && modList.hasModifierProperty(PsiModifier.ABSTRACT)) return HighlightInfoType.ABSTRACT_CLASS_NAME;
        }
        // use class by default
        return HighlightInfoType.CLASS_NAME;
    }

    
    public static HighlightInfo highlightReassignedVariable(PsiVariable variable, PsiElement elementToHighlight) {
        if (variable instanceof PsiLocalVariable) {
            return HighlightInfo.newHighlightInfo(HighlightInfoType.REASSIGNED_LOCAL_VARIABLE).range(elementToHighlight).create();
        }
        if (variable instanceof PsiParameter) {
            return HighlightInfo.newHighlightInfo(HighlightInfoType.REASSIGNED_PARAMETER).range(elementToHighlight).create();
        }
        return null;
    }

    private static TextAttributes getScopeAttributes( PsiElement element,  TextAttributesScheme colorsScheme) {
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        TextAttributes result = null;
        DependencyValidationManagerImpl validationManager = (DependencyValidationManagerImpl)DependencyValidationManager.getInstance(file.getProject());
        List<Pair<NamedScope,NamedScopesHolder>> scopes = validationManager.getScopeBasedHighlightingCachedScopes();
        for (Pair<NamedScope, NamedScopesHolder> scope : scopes) {
            final NamedScope namedScope = scope.getFirst();
            final TextAttributesKey scopeKey = ScopeAttributesUtil.getScopeTextAttributeKey(namedScope.getName());
            final TextAttributes attributes = colorsScheme.getAttributes(scopeKey);
            if (attributes == null || attributes.isEmpty()) {
                continue;
            }
            final PackageSet packageSet = namedScope.getValue();
            if (packageSet != null && packageSet.contains(file, scope.getSecond())) {
                result = TextAttributes.merge(attributes, result);
            }
        }
        return result;
    }

    
    public static TextRange getMethodDeclarationTextRange( PsiMethod method) {
        if (method instanceof SyntheticElement) return TextRange.EMPTY_RANGE;
        int start = stripAnnotationsFromModifierList(method.getModifierList());
        final TextRange throwsRange = method.getThrowsList().getTextRange();
        LOG.assertTrue(throwsRange != null, method);
        int end = throwsRange.getEndOffset();
        return new TextRange(start, end);
    }

    
    public static TextRange getFieldDeclarationTextRange( PsiField field) {
        int start = stripAnnotationsFromModifierList(field.getModifierList());
        int end = field.getNameIdentifier().getTextRange().getEndOffset();
        return new TextRange(start, end);
    }

    
    public static TextRange getClassDeclarationTextRange( PsiClass aClass) {
        if (aClass instanceof PsiEnumConstantInitializer) {
            return ((PsiEnumConstantInitializer)aClass).getEnumConstant().getNameIdentifier().getTextRange();
        }
        final PsiElement psiElement = aClass instanceof PsiAnonymousClass
                ? ((PsiAnonymousClass)aClass).getBaseClassReference()
                : aClass.getModifierList() == null ? aClass.getNameIdentifier() : aClass.getModifierList();
        if(psiElement == null) return new TextRange(aClass.getTextRange().getStartOffset(), aClass.getTextRange().getStartOffset());
        int start = stripAnnotationsFromModifierList(psiElement);
        PsiElement endElement = aClass instanceof PsiAnonymousClass ?
                ((PsiAnonymousClass)aClass).getBaseClassReference() :
                aClass.getImplementsList();
        if (endElement == null) endElement = aClass.getNameIdentifier();
        TextRange endTextRange = endElement == null ? null : endElement.getTextRange();
        int end = endTextRange == null ? start : endTextRange.getEndOffset();
        return new TextRange(start, end);
    }

    private static int stripAnnotationsFromModifierList( PsiElement element) {
        TextRange textRange = element.getTextRange();
        if (textRange == null) return 0;
        PsiAnnotation lastAnnotation = null;
        for (PsiElement child : element.getChildren()) {
            if (child instanceof PsiAnnotation) lastAnnotation = (PsiAnnotation)child;
        }
        if (lastAnnotation == null) {
            return textRange.getStartOffset();
        }
        ASTNode node = lastAnnotation.getNode();
        if (node != null) {
            do {
                node = TreeUtil.nextLeaf(node);
            }
            while (node != null && ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(node.getElementType()));
        }
        if (node != null) return node.getTextRange().getStartOffset();
        return textRange.getStartOffset();
    }
}
