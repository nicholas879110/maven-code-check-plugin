/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.psi.util;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.JavaSdkVersion;
import com.gome.maven.openapi.projectRoots.JavaVersionService;
import com.gome.maven.openapi.roots.LanguageLevelProjectExtension;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.infos.MethodCandidateInfo;
import com.gome.maven.psi.infos.MethodCandidateInfo.ApplicabilityLevel;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.search.ProjectScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.TimeoutUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.EmptyIterable;
import com.gome.maven.util.containers.HashMap;
import gnu.trove.THashSet;
//import org.intellij.lang.annotations.MagicConstant;

import java.util.*;

import static com.gome.maven.psi.CommonClassNames.JAVA_LANG_STRING;

public final class PsiUtil extends PsiUtilCore {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.util.PsiUtil");

    public static final int ACCESS_LEVEL_PUBLIC = 4;
    public static final int ACCESS_LEVEL_PROTECTED = 3;
    public static final int ACCESS_LEVEL_PACKAGE_LOCAL = 2;
    public static final int ACCESS_LEVEL_PRIVATE = 1;
    public static final Key<Boolean> VALID_VOID_TYPE_IN_CODE_FRAGMENT = Key.create("VALID_VOID_TYPE_IN_CODE_FRAGMENT");

    private PsiUtil() {}

    public static boolean isOnAssignmentLeftHand( PsiExpression expr) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, PsiParenthesizedExpression.class);
        return parent instanceof PsiAssignmentExpression &&
                PsiTreeUtil.isAncestor(((PsiAssignmentExpression)parent).getLExpression(), expr, false);
    }

    public static boolean isAccessibleFromPackage( PsiModifierListOwner element,  PsiPackage aPackage) {
        if (element.hasModifierProperty(PsiModifier.PUBLIC)) return true;
        return !element.hasModifierProperty(PsiModifier.PRIVATE) &&
                JavaPsiFacade.getInstance(element.getProject()).isInPackage(element, aPackage);
    }

    public static boolean isAccessedForWriting( PsiExpression expr) {
        if (isOnAssignmentLeftHand(expr)) return true;
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, PsiParenthesizedExpression.class);
        if (parent instanceof PsiPrefixExpression) {
            IElementType tokenType = ((PsiPrefixExpression) parent).getOperationTokenType();
            return tokenType == JavaTokenType.PLUSPLUS || tokenType == JavaTokenType.MINUSMINUS;
        }
        else if (parent instanceof PsiPostfixExpression) {
            IElementType tokenType = ((PsiPostfixExpression) parent).getOperationTokenType();
            return tokenType == JavaTokenType.PLUSPLUS || tokenType == JavaTokenType.MINUSMINUS;
        }
        else {
            return false;
        }
    }

    public static boolean isAccessedForReading( PsiExpression expr) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, PsiParenthesizedExpression.class);
        return !(parent instanceof PsiAssignmentExpression) ||
                !PsiTreeUtil.isAncestor(((PsiAssignmentExpression)parent).getLExpression(), expr, false) ||
                ((PsiAssignmentExpression)parent).getOperationTokenType() != JavaTokenType.EQ;
    }

    public static boolean isAccessible( PsiMember member,  PsiElement place,  PsiClass accessObjectClass) {
        return isAccessible(place.getProject(), member, place, accessObjectClass);
    }
    public static boolean isAccessible( Project project,  PsiMember member,
                                        PsiElement place,  PsiClass accessObjectClass) {
        return JavaPsiFacade.getInstance(project).getResolveHelper().isAccessible(member, place, accessObjectClass);
    }

    
    public static JavaResolveResult getAccessObjectClass( PsiExpression expression) {
        if (expression instanceof PsiSuperExpression) return JavaResolveResult.EMPTY;
        PsiType type = expression.getType();
        if (type instanceof PsiClassType) {
            return ((PsiClassType)type).resolveGenerics();
        }
        if (type instanceof PsiDisjunctionType) {
            final PsiType lub = ((PsiDisjunctionType)type).getLeastUpperBound();
            if (lub instanceof PsiClassType) {
                return ((PsiClassType)lub).resolveGenerics();
            }
        }
        if (type == null && expression instanceof PsiReferenceExpression) {
            JavaResolveResult resolveResult = ((PsiReferenceExpression)expression).advancedResolve(false);
            if (resolveResult.getElement() instanceof PsiClass) {
                return resolveResult;
            }
        }
        return JavaResolveResult.EMPTY;
    }

    public static boolean isConstantExpression( PsiExpression expression) {
        if (expression == null) return false;
        IsConstantExpressionVisitor visitor = new IsConstantExpressionVisitor();
        expression.accept(visitor);
        return visitor.myIsConstant;
    }

    // todo: move to PsiThrowsList?
    public static void addException( PsiMethod method,   String exceptionFQName) throws IncorrectOperationException {
        PsiClass exceptionClass = JavaPsiFacade.getInstance(method.getProject()).findClass(exceptionFQName, method.getResolveScope());
        addException(method, exceptionClass, exceptionFQName);
    }

    public static void addException( PsiMethod method,  PsiClass exceptionClass) throws IncorrectOperationException {
        addException(method, exceptionClass, exceptionClass.getQualifiedName());
    }

    private static void addException( PsiMethod method,  PsiClass exceptionClass,  String exceptionName) throws IncorrectOperationException {
        assert exceptionClass != null || exceptionName != null : "One of exceptionName, exceptionClass must be not null";
        PsiReferenceList throwsList = method.getThrowsList();
        PsiJavaCodeReferenceElement[] refs = throwsList.getReferenceElements();
        boolean replaced = false;
        for (PsiJavaCodeReferenceElement ref : refs) {
            if (ref.isReferenceTo(exceptionClass)) return;
            PsiClass aClass = (PsiClass)ref.resolve();
            if (exceptionClass == null || aClass == null) {
                continue;
            }
            if (aClass.isInheritor(exceptionClass, true)) {
                if (replaced) {
                    ref.delete();
                }
                else {
                    PsiElementFactory factory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();
                    PsiJavaCodeReferenceElement ref1;
                    if (exceptionName != null) {
                        ref1 = factory.createReferenceElementByFQClassName(exceptionName, method.getResolveScope());
                    }
                    else {
                        PsiClassType type = factory.createType(exceptionClass);
                        ref1 = factory.createReferenceElementByType(type);
                    }
                    ref.replace(ref1);
                    replaced = true;
                }
            }
            else if (exceptionClass.isInheritor(aClass, true)) {
                return;
            }
        }
        if (replaced) return;

        PsiElementFactory factory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();
        PsiJavaCodeReferenceElement ref;
        if (exceptionName != null) {
            ref = factory.createReferenceElementByFQClassName(exceptionName, method.getResolveScope());
        }
        else {
            PsiClassType type = factory.createType(exceptionClass);
            ref = factory.createReferenceElementByType(type);
        }
        throwsList.add(ref);
    }

    // todo: move to PsiThrowsList?
    public static void removeException( PsiMethod method,  String exceptionClass) throws IncorrectOperationException {
        PsiJavaCodeReferenceElement[] refs = method.getThrowsList().getReferenceElements();
        for (PsiJavaCodeReferenceElement ref : refs) {
            if (ref.getCanonicalText().equals(exceptionClass)) {
                ref.delete();
            }
        }
    }

    public static boolean isVariableNameUnique( String name,  PsiElement place) {
        PsiResolveHelper helper = JavaPsiFacade.getInstance(place.getProject()).getResolveHelper();
        return helper.resolveAccessibleReferencedVariable(name, place) == null;
    }

    /**
     * @return enclosing outermost (method or class initializer) body but not higher than scope
     */
    
    public static PsiElement getTopLevelEnclosingCodeBlock( PsiElement element, PsiElement scope) {
        PsiElement blockSoFar = null;
        while (element != null) {
            // variable can be defined in for loop initializer
            PsiElement parent = element.getParent();
            if (!(parent instanceof PsiExpression) || parent instanceof PsiLambdaExpression) {
                if (element instanceof PsiCodeBlock || element instanceof PsiForStatement || element instanceof PsiForeachStatement) {
                    blockSoFar = element;
                }

                if (parent instanceof PsiMethod
                        && parent.getParent() instanceof PsiClass
                        && !isLocalOrAnonymousClass((PsiClass)parent.getParent()))
                    break;
                if (parent instanceof PsiClassInitializer && !(parent.getParent() instanceof PsiAnonymousClass)) break;
                if (parent instanceof PsiField && ((PsiField) parent).getInitializer() == element) {
                    blockSoFar = element;
                }
                if (parent instanceof PsiClassLevelDeclarationStatement) {
                    parent = parent.getParent();
                }
                if (element instanceof PsiClass && !isLocalOrAnonymousClass((PsiClass)element)) {
                    break;
                }
                if (element instanceof PsiFile && PsiUtilCore.getTemplateLanguageFile(element) != null) {
                    return element;
                }
            }
            if (element == scope) break;
            element = parent;
        }
        return blockSoFar;
    }

    public static boolean isLocalOrAnonymousClass( PsiClass psiClass) {
        return psiClass instanceof PsiAnonymousClass || isLocalClass(psiClass);
    }

    public static boolean isLocalClass( PsiClass psiClass) {
        PsiElement parent = psiClass.getParent();
        return parent instanceof PsiDeclarationStatement && parent.getParent() instanceof PsiCodeBlock;
    }

    public static boolean isAbstractClass( PsiClass clazz) {
        PsiModifierList modifierList = clazz.getModifierList();
        return modifierList != null && modifierList.hasModifierProperty(PsiModifier.ABSTRACT);
    }

    /**
     * @return topmost code block where variable makes sense
     */
    
    public static PsiElement getVariableCodeBlock( PsiVariable variable,  PsiElement context) {
        PsiElement codeBlock = null;
        if (variable instanceof PsiParameter) {
            PsiElement declarationScope = ((PsiParameter)variable).getDeclarationScope();
            if (declarationScope instanceof PsiCatchSection) {
                codeBlock = ((PsiCatchSection)declarationScope).getCatchBlock();
            }
            else if (declarationScope instanceof PsiForeachStatement) {
                codeBlock = ((PsiForeachStatement)declarationScope).getBody();
            }
            else if (declarationScope instanceof PsiMethod) {
                codeBlock = ((PsiMethod)declarationScope).getBody();
            } else if (declarationScope instanceof PsiLambdaExpression) {
                codeBlock = ((PsiLambdaExpression)declarationScope).getBody();
            }
        }
        else if (variable instanceof PsiResourceVariable) {
            final PsiElement resourceList = variable.getParent();
            return resourceList != null ? resourceList.getParent() : null;  // use try statement as topmost
        }
        else if (variable instanceof PsiLocalVariable && variable.getParent() instanceof PsiForStatement) {
            return variable.getParent();
        }
        else if (variable instanceof PsiField && context != null) {
            final PsiClass aClass = ((PsiField) variable).getContainingClass();
            while (context != null && context.getParent() != aClass) {
                context = context.getParent();
                if (context instanceof PsiClassLevelDeclarationStatement) return null;
            }
            return context instanceof PsiMethod ?
                    ((PsiMethod) context).getBody() :
                    context instanceof PsiClassInitializer ? ((PsiClassInitializer) context).getBody() : null;
        }
        else {
            final PsiElement scope = variable.getParent() == null ? null : variable.getParent().getParent();
            codeBlock = getTopLevelEnclosingCodeBlock(variable, scope);
            if (codeBlock != null && codeBlock.getParent() instanceof PsiSwitchStatement) codeBlock = codeBlock.getParent().getParent();
        }
        return codeBlock;
    }

    public static boolean isIncrementDecrementOperation( PsiElement element) {
        if (element instanceof PsiPostfixExpression) {
            final IElementType sign = ((PsiPostfixExpression)element).getOperationTokenType();
            if (sign == JavaTokenType.PLUSPLUS || sign == JavaTokenType.MINUSMINUS)
                return true;
        }
        else if (element instanceof PsiPrefixExpression) {
            final IElementType sign = ((PsiPrefixExpression)element).getOperationTokenType();
            if (sign == JavaTokenType.PLUSPLUS || sign == JavaTokenType.MINUSMINUS)
                return true;
        }
        return false;
    }

//    @MagicConstant(intValues = {ACCESS_LEVEL_PUBLIC, ACCESS_LEVEL_PROTECTED, ACCESS_LEVEL_PACKAGE_LOCAL, ACCESS_LEVEL_PRIVATE})
    public @interface AccessLevel {}

    @AccessLevel
    public static int getAccessLevel( PsiModifierList modifierList) {
        if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
            return ACCESS_LEVEL_PRIVATE;
        }
        if (modifierList.hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) {
            return ACCESS_LEVEL_PACKAGE_LOCAL;
        }
        if (modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
            return ACCESS_LEVEL_PROTECTED;
        }
        return ACCESS_LEVEL_PUBLIC;
    }

    @PsiModifier.ModifierConstant
    
    public static String getAccessModifier(@AccessLevel int accessLevel) {
        @SuppressWarnings("UnnecessaryLocalVariable") @PsiModifier.ModifierConstant
        final String modifier = accessLevel > accessModifiers.length ? null : accessModifiers[accessLevel - 1];
        return modifier;
    }

    private static final String[] accessModifiers = {
            PsiModifier.PRIVATE, PsiModifier.PACKAGE_LOCAL, PsiModifier.PROTECTED, PsiModifier.PUBLIC
    };

    /**
     * @return true if element specified is statement or expression statement. see JLS 14.5-14.8
     */
    public static boolean isStatement( PsiElement element) {
        PsiElement parent = element.getParent();

        if (element instanceof PsiExpressionListStatement) {
            // statement list allowed in for() init or update only
            if (!(parent instanceof PsiForStatement)) return false;
            final PsiForStatement forStatement = (PsiForStatement)parent;
            if (!(element == forStatement.getInitialization() || element == forStatement.getUpdate())) return false;
            final PsiExpressionList expressionList = ((PsiExpressionListStatement) element).getExpressionList();
            final PsiExpression[] expressions = expressionList.getExpressions();
            for (PsiExpression expression : expressions) {
                if (!isStatement(expression)) return false;
            }
            return true;
        }
        else if (element instanceof PsiExpressionStatement) {
            return isStatement(((PsiExpressionStatement) element).getExpression());
        }
        if (element instanceof PsiDeclarationStatement) {
            if (parent instanceof PsiCodeBlock) return true;
            if (parent instanceof PsiCodeFragment) return true;

            if (!(parent instanceof PsiForStatement) || ((PsiForStatement)parent).getBody() == element) {
                return false;
            }
        }

        if (element instanceof PsiStatement) return true;
        if (element instanceof PsiAssignmentExpression) return true;
        if (isIncrementDecrementOperation(element)) return true;
        if (element instanceof PsiMethodCallExpression) return true;
        if (element instanceof PsiNewExpression) {
            return !(((PsiNewExpression) element).getType() instanceof PsiArrayType);
        }
        return element instanceof PsiCodeBlock;
    }

    
    public static PsiElement getEnclosingStatement(PsiElement element) {
        while (element != null) {
            if (element.getParent() instanceof PsiCodeBlock) return element;
            element = element.getParent();
        }
        return null;
    }


    
    public static PsiElement getElementInclusiveRange( PsiElement scope,  TextRange range) {
        PsiElement psiElement = scope.findElementAt(range.getStartOffset());
        while (psiElement != null && !psiElement.getTextRange().contains(range)) {
            if (psiElement == scope) return null;
            psiElement = psiElement.getParent();
        }
        return psiElement;
    }

    
    public static PsiClass resolveClassInType( PsiType type) {
        if (type instanceof PsiClassType) {
            return ((PsiClassType) type).resolve();
        }
        if (type instanceof PsiArrayType) {
            return resolveClassInType(((PsiArrayType) type).getComponentType());
        }
        if (type instanceof PsiDisjunctionType) {
            final PsiType lub = ((PsiDisjunctionType)type).getLeastUpperBound();
            if (lub instanceof PsiClassType) {
                return ((PsiClassType)lub).resolve();
            }
        }
        return null;
    }

    
    public static PsiClass resolveClassInClassTypeOnly( PsiType type) {
        return type instanceof PsiClassType ? ((PsiClassType)type).resolve() : null;
    }

    public static PsiClassType.ClassResolveResult resolveGenericsClassInType( PsiType type) {
        if (type instanceof PsiClassType) {
            final PsiClassType classType = (PsiClassType) type;
            return classType.resolveGenerics();
        }
        if (type instanceof PsiArrayType) {
            return resolveGenericsClassInType(((PsiArrayType) type).getComponentType());
        }
        if (type instanceof PsiDisjunctionType) {
            final PsiType lub = ((PsiDisjunctionType)type).getLeastUpperBound();
            if (lub instanceof PsiClassType) {
                return ((PsiClassType)lub).resolveGenerics();
            }
        }
        return PsiClassType.ClassResolveResult.EMPTY;
    }

    
    public static PsiType convertAnonymousToBaseType( PsiType type) {
        PsiClass psiClass = resolveClassInType(type);
        if (psiClass instanceof PsiAnonymousClass) {
            int dims = type.getArrayDimensions();
            type = ((PsiAnonymousClass) psiClass).getBaseClassType();
            while (dims != 0) {
                type = type.createArrayType();
                dims--;
            }
        }
        return type;
    }

    public static boolean isApplicable( PsiMethod method,  PsiSubstitutor substitutorForMethod,  PsiExpressionList argList) {
        return getApplicabilityLevel(method, substitutorForMethod, argList) != ApplicabilityLevel.NOT_APPLICABLE;
    }

    public static boolean isApplicable( PsiMethod method,  PsiSubstitutor substitutorForMethod,  PsiExpression[] argList) {
        final PsiType[] types = ContainerUtil.map2Array(argList, PsiType.class, PsiExpression.EXPRESSION_TO_TYPE);
        return getApplicabilityLevel(method, substitutorForMethod, types, getLanguageLevel(method)) != ApplicabilityLevel.NOT_APPLICABLE;
    }

    @MethodCandidateInfo.ApplicabilityLevelConstant
    public static int getApplicabilityLevel( PsiMethod method,  PsiSubstitutor substitutorForMethod,  PsiExpressionList argList) {
        return getApplicabilityLevel(method, substitutorForMethod, argList.getExpressionTypes(), getLanguageLevel(argList));
    }

    @MethodCandidateInfo.ApplicabilityLevelConstant
    public static int getApplicabilityLevel( final PsiMethod method,
                                             final PsiSubstitutor substitutorForMethod,
                                             final PsiType[] args,
                                             final LanguageLevel languageLevel) {
        return getApplicabilityLevel(method, substitutorForMethod, args, languageLevel, true, true);
    }


    public interface ApplicabilityChecker {
        ApplicabilityChecker ASSIGNABILITY_CHECKER = new ApplicabilityChecker() {
            @Override
            public boolean isApplicable(PsiType left, PsiType right, boolean allowUncheckedConversion, int argId) {
                return TypeConversionUtil.isAssignable(left, right, allowUncheckedConversion);
            }
        };

        boolean isApplicable(PsiType left, PsiType right, boolean allowUncheckedConversion, int argId);
    }

    @MethodCandidateInfo.ApplicabilityLevelConstant
    public static int getApplicabilityLevel( final PsiMethod method,
                                             final PsiSubstitutor substitutorForMethod,
                                             final PsiType[] args,
                                             final LanguageLevel languageLevel,
                                            final boolean allowUncheckedConversion,
                                            final boolean checkVarargs) {
        return getApplicabilityLevel(method, substitutorForMethod, args, languageLevel,
                allowUncheckedConversion, checkVarargs, ApplicabilityChecker.ASSIGNABILITY_CHECKER);
    }

    @MethodCandidateInfo.ApplicabilityLevelConstant
    public static int getApplicabilityLevel( final PsiMethod method,
                                             final PsiSubstitutor substitutorForMethod,
                                             final PsiType[] args,
                                             final LanguageLevel languageLevel,
                                            final boolean allowUncheckedConversion,
                                            final boolean checkVarargs,
                                             final ApplicabilityChecker function) {
        final PsiParameter[] parms = method.getParameterList().getParameters();
        if (args.length < parms.length - 1) return ApplicabilityLevel.NOT_APPLICABLE;

        final PsiClass containingClass = method.getContainingClass();
        final boolean isRaw = containingClass != null && isRawSubstitutor(method, substitutorForMethod) && isRawSubstitutor(containingClass, substitutorForMethod);
        if (!areFirstArgumentsApplicable(args, parms, languageLevel, substitutorForMethod, isRaw, allowUncheckedConversion, function)) return ApplicabilityLevel.NOT_APPLICABLE;
        if (args.length == parms.length) {
            if (parms.length == 0) return ApplicabilityLevel.FIXED_ARITY;
            PsiType parmType = getParameterType(parms[parms.length - 1], languageLevel, substitutorForMethod);
            PsiType argType = args[args.length - 1];
            if (argType == null) return ApplicabilityLevel.NOT_APPLICABLE;
            if (function.isApplicable(parmType, argType, allowUncheckedConversion, parms.length - 1)) return ApplicabilityLevel.FIXED_ARITY;

            if (isRaw) {
                final PsiType erasedParamType = TypeConversionUtil.erasure(parmType);
                final PsiType erasedArgType = TypeConversionUtil.erasure(argType);
                if (erasedArgType != null &&  erasedParamType != null &&
                        function.isApplicable(erasedParamType, erasedArgType, allowUncheckedConversion, parms.length - 1)) {
                    return ApplicabilityLevel.FIXED_ARITY;
                }
            }
        }

        if (checkVarargs && method.isVarArgs() && languageLevel.compareTo(LanguageLevel.JDK_1_5) >= 0) {
            if (args.length < parms.length) return ApplicabilityLevel.VARARGS;
            PsiParameter lastParameter = parms[parms.length - 1];
            if (!lastParameter.isVarArgs()) return ApplicabilityLevel.NOT_APPLICABLE;
            PsiType lastParmType = getParameterType(lastParameter, languageLevel, substitutorForMethod);
            if (!(lastParmType instanceof PsiArrayType)) return ApplicabilityLevel.NOT_APPLICABLE;
            lastParmType = ((PsiArrayType)lastParmType).getComponentType();
            if (lastParmType instanceof PsiCapturedWildcardType &&
                    !JavaVersionService.getInstance().isAtLeast(((PsiCapturedWildcardType)lastParmType).getContext(), JavaSdkVersion.JDK_1_8)) {
                lastParmType = ((PsiCapturedWildcardType)lastParmType).getWildcard();
            }
            for (int i = parms.length - 1; i < args.length; i++) {
                PsiType argType = args[i];
                if (argType == null || !function.isApplicable(lastParmType, argType, allowUncheckedConversion, i)) {
                    return ApplicabilityLevel.NOT_APPLICABLE;
                }
            }
            return ApplicabilityLevel.VARARGS;
        }

        return ApplicabilityLevel.NOT_APPLICABLE;
    }

    private static boolean areFirstArgumentsApplicable( PsiType[] args,
                                                        final PsiParameter[] parms,
                                                        LanguageLevel languageLevel,
                                                        final PsiSubstitutor substitutorForMethod,
                                                       boolean isRaw,
                                                       boolean allowUncheckedConversion, ApplicabilityChecker function) {
        for (int i = 0; i < parms.length - 1; i++) {
            final PsiType type = args[i];
            if (type == null) return false;
            final PsiParameter parameter = parms[i];
            final PsiType substitutedParmType = getParameterType(parameter, languageLevel, substitutorForMethod);
            if (isRaw) {
                final PsiType substErasure = TypeConversionUtil.erasure(substitutedParmType);
                final PsiType typeErasure = TypeConversionUtil.erasure(type);
                if (substErasure != null && typeErasure != null && !function.isApplicable(substErasure, typeErasure, allowUncheckedConversion, i)) {
                    return false;
                }
            }
            else if (!function.isApplicable(substitutedParmType, type, allowUncheckedConversion, i)) {
                return false;
            }
        }
        return true;
    }

    private static PsiType getParameterType( final PsiParameter parameter,
                                             LanguageLevel languageLevel,
                                             final PsiSubstitutor substitutor) {
        PsiType parmType = parameter.getType();
        if (parmType instanceof PsiClassType) {
            parmType = ((PsiClassType)parmType).setLanguageLevel(languageLevel);
        }
        return substitutor.substitute(parmType);
    }

    public static boolean equalOnClass( PsiSubstitutor s1,  PsiSubstitutor s2,  PsiClass aClass) {
        return equalOnEquivalentClasses(s1, aClass, s2, aClass);
    }

    public static boolean equalOnEquivalentClasses( PsiSubstitutor s1,  PsiClass aClass,  PsiSubstitutor s2,  PsiClass bClass) {
        // assume generic class equals to non-generic
        if (aClass.hasTypeParameters() != bClass.hasTypeParameters()) return true;
        final PsiTypeParameter[] typeParameters1 = aClass.getTypeParameters();
        final PsiTypeParameter[] typeParameters2 = bClass.getTypeParameters();
        if (typeParameters1.length != typeParameters2.length) return false;
        for (int i = 0; i < typeParameters1.length; i++) {
            final PsiType substituted2 = s2.substitute(typeParameters2[i]);
            final PsiType substituted1 = s1.substitute(typeParameters1[i]);
            if (!Comparing.equal(s1.substituteWithBoundsPromotion(typeParameters1[i]), substituted2) &&
                    !Comparing.equal(s2.substituteWithBoundsPromotion(typeParameters2[i]), substituted1) &&
                    !Comparing.equal(substituted1, substituted2)) return false;
        }
        if (aClass.hasModifierProperty(PsiModifier.STATIC)) return true;
        final PsiClass containingClass1 = aClass.getContainingClass();
        final PsiClass containingClass2 = bClass.getContainingClass();

        if (containingClass1 != null && containingClass2 != null) {
            return equalOnEquivalentClasses(s1, containingClass1, s2, containingClass2);
        }

        return containingClass1 == null && containingClass2 == null;
    }

    /**
     * JLS 15.28
     */
    public static boolean isCompileTimeConstant( final PsiField field) {
        return field.hasModifierProperty(PsiModifier.FINAL)
                && (TypeConversionUtil.isPrimitiveAndNotNull(field.getType()) || field.getType().equalsToText(JAVA_LANG_STRING))
                && field.hasInitializer()
                && isConstantExpression(field.getInitializer());
    }

    public static boolean allMethodsHaveSameSignature( PsiMethod[] methods) {
        if (methods.length == 0) return true;
        final MethodSignature methodSignature = methods[0].getSignature(PsiSubstitutor.EMPTY);
        for (int i = 1; i < methods.length; i++) {
            PsiMethod method = methods[i];
            if (!methodSignature.equals(method.getSignature(PsiSubstitutor.EMPTY))) return false;
        }
        return true;
    }

    
    public static PsiExpression deparenthesizeExpression(PsiExpression expression) {
        while (true) {
            if (expression instanceof PsiParenthesizedExpression) {
                expression = ((PsiParenthesizedExpression)expression).getExpression();
                continue;
            }
            if (expression instanceof PsiTypeCastExpression) {
                expression = ((PsiTypeCastExpression)expression).getOperand();
                continue;
            }
            return expression;
        }
    }

    /**
     * Checks whether given class is inner (as opposed to nested)
     *
     */
    public static boolean isInnerClass( PsiClass aClass) {
        return !aClass.hasModifierProperty(PsiModifier.STATIC) && aClass.getContainingClass() != null;
    }

    
    public static PsiElement findModifierInList( final PsiModifierList modifierList,  String modifier) {
        final PsiElement[] children = modifierList.getChildren();
        for (PsiElement child : children) {
            if (child.getText().equals(modifier)) return child;
        }
        return null;
    }

    
    public static PsiClass getTopLevelClass( PsiElement element) {
        final PsiFile file = element.getContainingFile();
        if (file instanceof PsiClassOwner) {
            final PsiClass[] classes = ((PsiClassOwner)file).getClasses();
            for (PsiClass aClass : classes) {
                if (PsiTreeUtil.isAncestor(aClass, element, false)) return aClass;
            }
        }
        return null;
    }

    /**
     * @param place place to start traversal
     * @param aClass level to stop traversal
     * @return element with static modifier enclosing place and enclosed by aClass (if not null)
     */
    
    public static PsiModifierListOwner getEnclosingStaticElement( PsiElement place,  PsiClass aClass) {
        LOG.assertTrue(aClass == null || !place.isPhysical() || PsiTreeUtil.isContextAncestor(aClass, place, false));
        PsiElement parent = place;
        while (parent != aClass) {
            if (parent instanceof PsiFile) break;
            if (parent instanceof PsiModifierListOwner && ((PsiModifierListOwner)parent).hasModifierProperty(PsiModifier.STATIC)) {
                return (PsiModifierListOwner)parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    
    public static PsiType getTypeByPsiElement( final PsiElement element) {
        if (element instanceof PsiVariable) {
            return ((PsiVariable)element).getType();
        }
        else if (element instanceof PsiMethod) return ((PsiMethod)element).getReturnType();
        return null;
    }

    
    public static PsiType captureToplevelWildcards( final PsiType type, final PsiElement context) {
        if (type instanceof PsiClassType) {
            final PsiClassType.ClassResolveResult result = ((PsiClassType)type).resolveGenerics();
            final PsiClass aClass = result.getElement();
            if (aClass != null) {
                final PsiSubstitutor substitutor = result.getSubstitutor();
                Map<PsiTypeParameter, PsiType> substitutionMap = null;
                for (PsiTypeParameter typeParameter : typeParametersIterable(aClass)) {
                    final PsiType substituted = substitutor.substitute(typeParameter);
                    if (substituted instanceof PsiWildcardType) {
                        if (substitutionMap == null) substitutionMap = new HashMap<PsiTypeParameter, PsiType>(substitutor.getSubstitutionMap());
                        substitutionMap.put(typeParameter, PsiCapturedWildcardType.create((PsiWildcardType)substituted, context, typeParameter));
                    }
                }

                if (substitutionMap != null) {
                    final PsiElementFactory factory = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory();
                    final PsiSubstitutor newSubstitutor = factory.createSubstitutor(substitutionMap);
                    return factory.createType(aClass, newSubstitutor);
                }
            }
        }
        else if (type instanceof PsiArrayType) {
            return captureToplevelWildcards(((PsiArrayType)type).getComponentType(), context).createArrayType();
        }

        return type;
    }

    public static boolean isInsideJavadocComment(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, PsiDocComment.class, true) != null;
    }

    
    public static List<PsiTypeElement> getParameterTypeElements( PsiParameter parameter) {
        PsiTypeElement typeElement = parameter.getTypeElement();
        return typeElement != null && typeElement.getType() instanceof PsiDisjunctionType
                ? PsiTreeUtil.getChildrenOfTypeAsList(typeElement, PsiTypeElement.class)
                : Collections.singletonList(typeElement);
    }

    public static void checkIsIdentifier( PsiManager manager, String text) throws IncorrectOperationException{
        if (!PsiNameHelper.getInstance(manager.getProject()).isIdentifier(text)){
            throw new IncorrectOperationException(PsiBundle.message("0.is.not.an.identifier", text) );
        }
    }

    
    public static VirtualFile getJarFile( PsiElement candidate) {
        VirtualFile file = candidate.getContainingFile().getVirtualFile();
        if (file != null && file.getFileSystem().getProtocol().equals("jar")) {
            return VfsUtilCore.getVirtualFileForJar(file);
        }
        return file;
    }

    public static boolean isAnnotationMethod(PsiElement element) {
        if (!(element instanceof PsiAnnotationMethod)) return false;
        PsiClass psiClass = ((PsiAnnotationMethod)element).getContainingClass();
        return psiClass != null && psiClass.isAnnotationType();
    }

    @PsiModifier.ModifierConstant
    public static String getMaximumModifierForMember(final PsiClass aClass) {
        return getMaximumModifierForMember(aClass, true);
    }

    @PsiModifier.ModifierConstant
    public static String getMaximumModifierForMember(final PsiClass aClass, boolean allowPublicAbstract) {
        String modifier = PsiModifier.PUBLIC;

        if (!allowPublicAbstract && aClass.hasModifierProperty(PsiModifier.ABSTRACT) && !aClass.isEnum()) {
            modifier =  PsiModifier.PROTECTED;
        }
        else if (aClass.hasModifierProperty(PsiModifier.PACKAGE_LOCAL) || aClass.isEnum()) {
            modifier = PsiModifier.PACKAGE_LOCAL;
        }
        else if (aClass.hasModifierProperty(PsiModifier.PRIVATE)) {
            modifier = PsiModifier.PRIVATE;
        }

        return modifier;
    }

    /*
     * Returns iterator of type parameters visible in owner. Type parameters are iterated in
     * inner-to-outer, right-to-left order.
     */
    
    public static Iterator<PsiTypeParameter> typeParametersIterator( PsiTypeParameterListOwner owner) {
        return typeParametersIterable(owner).iterator();
    }

    
    public static Iterable<PsiTypeParameter> typeParametersIterable( final PsiTypeParameterListOwner owner) {
        List<PsiTypeParameter> result = null;

        PsiTypeParameterListOwner currentOwner = owner;
        while (currentOwner != null) {
            PsiTypeParameter[] typeParameters = currentOwner.getTypeParameters();
            if (typeParameters.length > 0) {
                if (result == null) result = new ArrayList<PsiTypeParameter>(typeParameters.length);
                for (int i = typeParameters.length - 1; i >= 0; i--) {
                    result.add(typeParameters[i]);
                }
            }

            if (currentOwner.hasModifierProperty(PsiModifier.STATIC)) break;
            currentOwner = currentOwner.getContainingClass();
        }

        if (result == null) return EmptyIterable.getInstance();
        return result;
    }

    public static boolean canBeOverriden( PsiMethod method) {
        PsiClass parentClass = method.getContainingClass();
        return parentClass != null &&
                !method.isConstructor() &&
                !method.hasModifierProperty(PsiModifier.STATIC) &&
                !method.hasModifierProperty(PsiModifier.FINAL) &&
                !method.hasModifierProperty(PsiModifier.PRIVATE) &&
                !(parentClass instanceof PsiAnonymousClass) &&
                !parentClass.hasModifierProperty(PsiModifier.FINAL);
    }

    
    public static PsiElement[] mapElements( ResolveResult[] candidates) {
        PsiElement[] result = new PsiElement[candidates.length];
        for (int i = 0; i < candidates.length; i++) {
            result[i] = candidates[i].getElement();
        }
        return result;
    }

    
    public static PsiMember findEnclosingConstructorOrInitializer(PsiElement expression) {
        PsiMember parent = PsiTreeUtil.getParentOfType(expression, PsiClassInitializer.class, PsiEnumConstantInitializer.class, PsiMethod.class, PsiField.class);
        if (parent instanceof PsiMethod && !((PsiMethod)parent).isConstructor()) return null;
        if (parent instanceof PsiField && parent.hasModifierProperty(PsiModifier.STATIC)) return null;
        return parent;
    }

    public static boolean checkName( PsiElement element,  String name, final PsiElement context) {
        if (element instanceof PsiMetaOwner) {
            final PsiMetaData data = ((PsiMetaOwner) element).getMetaData();
            if (data != null) return name.equals(data.getName(context));
        }
        return element instanceof PsiNamedElement && name.equals(((PsiNamedElement)element).getName());
    }

    public static boolean isRawSubstitutor ( PsiTypeParameterListOwner owner,  PsiSubstitutor substitutor) {
        for (PsiTypeParameter parameter : typeParametersIterable(owner)) {
            if (substitutor.substitute(parameter) == null) return true;
        }
        return false;
    }

    public static final Key<LanguageLevel> FILE_LANGUAGE_LEVEL_KEY = Key.create("FORCE_LANGUAGE_LEVEL");

    public static boolean isLanguageLevel5OrHigher( final PsiElement element) {
        return getLanguageLevel(element).isAtLeast(LanguageLevel.JDK_1_5);
    }

    public static boolean isLanguageLevel6OrHigher( final PsiElement element) {
        return getLanguageLevel(element).isAtLeast(LanguageLevel.JDK_1_6);
    }

    public static boolean isLanguageLevel7OrHigher( final PsiElement element) {
        return getLanguageLevel(element).isAtLeast(LanguageLevel.JDK_1_7);
    }

    public static boolean isLanguageLevel8OrHigher( final PsiElement element) {
        return getLanguageLevel(element).isAtLeast(LanguageLevel.JDK_1_8);
    }

    public static boolean isLanguageLevel9OrHigher( final PsiElement element) {
        return getLanguageLevel(element).isAtLeast(LanguageLevel.JDK_1_9);
    }

    
    public static LanguageLevel getLanguageLevel( PsiElement element) {
        if (element instanceof PsiDirectory) {
            return JavaDirectoryService.getInstance().getLanguageLevel((PsiDirectory)element);
        }

        PsiFile file = element.getContainingFile();
        if (file instanceof PsiJavaFile) {
            return ((PsiJavaFile)file).getLanguageLevel();
        }

        if (file != null) {
            PsiElement context = file.getContext();
            if (context != null) {
                return getLanguageLevel(context);
            }
        }

        return getLanguageLevel(element.getProject());
    }

    
    public static LanguageLevel getLanguageLevel( Project project) {
        LanguageLevelProjectExtension instance = LanguageLevelProjectExtension.getInstance(project);
        return instance != null ? instance.getLanguageLevel() : LanguageLevel.HIGHEST;
    }

    public static boolean isInstantiatable( PsiClass clazz) {
        return !clazz.hasModifierProperty(PsiModifier.ABSTRACT) &&
                clazz.hasModifierProperty(PsiModifier.PUBLIC) &&
                hasDefaultConstructor(clazz);
    }

    public static boolean hasDefaultConstructor( PsiClass clazz) {
        return hasDefaultConstructor(clazz, false);
    }

    public static boolean hasDefaultConstructor( PsiClass clazz, boolean allowProtected) {
        return hasDefaultConstructor(clazz, allowProtected, true);
    }

    public static boolean hasDefaultConstructor( PsiClass clazz, boolean allowProtected, boolean checkModifiers) {
        return hasDefaultCtrInHierarchy(clazz, allowProtected, checkModifiers, null);
    }

    private static boolean hasDefaultCtrInHierarchy( PsiClass clazz, boolean allowProtected, boolean checkModifiers,  Set<PsiClass> visited) {
        final PsiMethod[] constructors = clazz.getConstructors();
        if (constructors.length > 0) {
            for (PsiMethod cls: constructors) {
                if ((!checkModifiers || cls.hasModifierProperty(PsiModifier.PUBLIC) ||
                        allowProtected && cls.hasModifierProperty(PsiModifier.PROTECTED)) &&
                        cls.getParameterList().getParametersCount() == 0) {
                    return true;
                }
            }
        }
        else {
            final PsiClass superClass = clazz.getSuperClass();
            if (superClass == null) {
                return true;
            }
            if (visited == null) visited = new THashSet<PsiClass>();
            if (!visited.add(clazz)) return false;
            return hasDefaultCtrInHierarchy(superClass, true, true, visited);
        }
        return false;
    }

    
    public static PsiType extractIterableTypeParameter( PsiType psiType, final boolean eraseTypeParameter) {
        final PsiType type = substituteTypeParameter(psiType, CommonClassNames.JAVA_LANG_ITERABLE, 0, eraseTypeParameter);
        return type != null ? type : substituteTypeParameter(psiType, CommonClassNames.JAVA_UTIL_COLLECTION, 0, eraseTypeParameter);
    }

    
    public static PsiType substituteTypeParameter( final PsiType psiType,  final String superClass, final int typeParamIndex,
                                                  final boolean eraseTypeParameter) {
        if (psiType == null) return null;

        if (!(psiType instanceof PsiClassType)) return null;

        final PsiClassType classType = (PsiClassType)psiType;
        final PsiClassType.ClassResolveResult classResolveResult = classType.resolveGenerics();
        final PsiClass psiClass = classResolveResult.getElement();
        if (psiClass == null) return null;

        final PsiClass baseClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(superClass, psiClass.getResolveScope());
        if (baseClass == null) return null;

        if (!psiClass.isEquivalentTo(baseClass) && !psiClass.isInheritor(baseClass, true)) return null;

        final PsiTypeParameter[] parameters = baseClass.getTypeParameters();
        if (parameters.length <= typeParamIndex) return PsiType.getJavaLangObject(psiClass.getManager(), psiClass.getResolveScope());

        final PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(baseClass, psiClass, classResolveResult.getSubstitutor());
        final PsiType type = substitutor.substitute(parameters[typeParamIndex]);
        if (type == null && eraseTypeParameter) {
            return TypeConversionUtil.typeParameterErasure(parameters[typeParamIndex]);
        }
        return type;
    }

    public static final Comparator<PsiElement> BY_POSITION = new Comparator<PsiElement>() {
        @Override
        public int compare(PsiElement o1, PsiElement o2) {
            return compareElementsByPosition(o1, o2);
        }
    };

    public static void setModifierProperty( PsiModifierListOwner owner,  @PsiModifier.ModifierConstant String property, boolean value) {
        final PsiModifierList modifierList = owner.getModifierList();
        assert modifierList != null : owner;
        modifierList.setModifierProperty(property, value);
    }

    public static boolean isTryBlock( final PsiElement element) {
        if (element == null) return false;
        final PsiElement parent = element.getParent();
        return parent instanceof PsiTryStatement && element == ((PsiTryStatement)parent).getTryBlock();
    }

    public static boolean isElseBlock( final PsiElement element) {
        if (element == null) return false;
        final PsiElement parent = element.getParent();
        return parent instanceof PsiIfStatement && element == ((PsiIfStatement)parent).getElseBranch();
    }

    public static boolean isJavaToken( PsiElement element, IElementType type) {
        return element instanceof PsiJavaToken && ((PsiJavaToken)element).getTokenType() == type;
    }

    public static boolean isJavaToken( PsiElement element,  TokenSet types) {
        return element instanceof PsiJavaToken && types.contains(((PsiJavaToken)element).getTokenType());
    }

    public static boolean isCatchParameter( final PsiElement element) {
        return element instanceof PsiParameter && element.getParent() instanceof PsiCatchSection;
    }

    public static boolean isIgnoredName( final String name) {
        return "ignore".equals(name) || "ignored".equals(name);
    }

    
    public static PsiMethod getResourceCloserMethod( final PsiResourceVariable resource) {
        final PsiType resourceType = resource.getType();
        if (!(resourceType instanceof PsiClassType)) return null;
        return getResourceCloserMethodForType((PsiClassType)resourceType);
    }

    
    public static PsiMethod getResourceCloserMethodForType( final PsiClassType resourceType) {
        final PsiClass resourceClass = resourceType.resolve();
        if (resourceClass == null) return null;

        final Project project = resourceClass.getProject();
        final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        final PsiClass autoCloseable = facade.findClass(CommonClassNames.JAVA_LANG_AUTO_CLOSEABLE, ProjectScope.getLibrariesScope(project));
        if (autoCloseable == null) return null;

        if (!InheritanceUtil.isInheritorOrSelf(resourceClass, autoCloseable, true)) return null;

        final PsiMethod[] closes = autoCloseable.findMethodsByName("close", false);
        return closes.length == 1 ? resourceClass.findMethodBySignature(closes[0], true) : null;
    }

    
    public static PsiExpression skipParenthesizedExprDown(PsiExpression initializer) {
        while (initializer instanceof PsiParenthesizedExpression) {
            initializer = ((PsiParenthesizedExpression)initializer).getExpression();
        }
        return initializer;
    }

    public static PsiElement skipParenthesizedExprUp(PsiElement parent) {
        while (parent instanceof PsiParenthesizedExpression) {
            parent = parent.getParent();
        }
        return parent;
    }

    public static void ensureValidType( PsiType type) {
        ensureValidType(type, null);
    }
    public static void ensureValidType( PsiType type,  String customMessage) {
        if (!type.isValid()) {
            TimeoutUtil.sleep(1); // to see if processing in another thread suddenly makes the type valid again (which is a bug)
            if (type.isValid()) {
                LOG.error("PsiType resurrected: " + type + " of " + type.getClass() + " " + customMessage);
                return;
            }
            if (type instanceof PsiClassType) {
                try {
                    PsiClass psiClass = ((PsiClassType)type).resolve(); // should throw exception
                    if (psiClass != null) {
                        ensureValid(psiClass);
                    }
                }
                catch (PsiInvalidElementAccessException e) {
                    throw customMessage == null? e : new RuntimeException(customMessage, e);
                }
            }
            throw new AssertionError("Invalid type: " + type + " of class " + type.getClass() + " " + customMessage);
        }
    }

    
    public static String getMemberQualifiedName( PsiMember member) {
        if (member instanceof PsiClass) {
            return ((PsiClass)member).getQualifiedName();
        }

        PsiClass containingClass = member.getContainingClass();
        if (containingClass == null) return null;
        String className = containingClass.getQualifiedName();
        if (className == null) return null;
        return className + "." + member.getName();
    }

    static boolean checkSameExpression(PsiElement templateExpr, final PsiExpression expression) {
        return templateExpr.equals(skipParenthesizedExprDown(expression));
    }

    public static boolean isCondition(PsiElement expr, PsiElement parent) {
        if (parent instanceof PsiIfStatement) {
            if (checkSameExpression(expr, ((PsiIfStatement)parent).getCondition())) {
                return true;
            }
        }
        else if (parent instanceof PsiWhileStatement) {
            if (checkSameExpression(expr, ((PsiWhileStatement)parent).getCondition())) {
                return true;
            }
        }
        else if (parent instanceof PsiForStatement) {
            if (checkSameExpression(expr, ((PsiForStatement)parent).getCondition())) {
                return true;
            }
        }
        else if (parent instanceof PsiDoWhileStatement) {
            if (checkSameExpression(expr, ((PsiDoWhileStatement)parent).getCondition())) {
                return true;
            }
        }
        else if (parent instanceof PsiConditionalExpression) {
            if (checkSameExpression(expr, ((PsiConditionalExpression)parent).getCondition())) {
                return true;
            }
        }
        return false;
    }

    public static PsiReturnStatement[] findReturnStatements(PsiMethod method) {
        return findReturnStatements(method.getBody());
    }

    public static PsiReturnStatement[] findReturnStatements(PsiCodeBlock body) {
        ArrayList<PsiReturnStatement> vector = new ArrayList<PsiReturnStatement>();
        if (body != null) {
            addReturnStatements(vector, body);
        }
        return vector.toArray(new PsiReturnStatement[vector.size()]);
    }

    private static void addReturnStatements(ArrayList<PsiReturnStatement> vector, PsiElement element) {
        if (element instanceof PsiReturnStatement) {
            vector.add((PsiReturnStatement)element);
        }
        else if (!(element instanceof PsiClass) && !(element instanceof PsiLambdaExpression)) {
            PsiElement[] children = element.getChildren();
            for (PsiElement child : children) {
                addReturnStatements(vector, child);
            }
        }
    }
}