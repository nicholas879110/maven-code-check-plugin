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
package com.gome.maven.codeInsight.intention;

import com.gome.maven.codeInsight.daemon.QuickFixActionRegistrar;
import com.gome.maven.codeInspection.IntentionAndQuickFixAction;
import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.gome.maven.codeInspection.LocalQuickFixOnPsiElement;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.PropertyMemberType;

import java.util.Collection;
import java.util.List;

/**
 * @author cdr
 */
public abstract class QuickFixFactory {
    public static QuickFixFactory getInstance() {
        return ServiceManager.getService(QuickFixFactory.class);
    }

    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createModifierListFix( PsiModifierList modifierList,
                                                                                      @PsiModifier.ModifierConstant  String modifier,
                                                                                      boolean shouldHave,
                                                                                      final boolean showContainingClass);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createModifierListFix( PsiModifierListOwner owner,
                                                                                      @PsiModifier.ModifierConstant  String modifier,
                                                                                      boolean shouldHave,
                                                                                      final boolean showContainingClass);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createMethodReturnFix( PsiMethod method,  PsiType toReturn, boolean fixWholeHierarchy);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createAddMethodFix( PsiMethod method,  PsiClass toClass);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createAddMethodFix( String methodText,  PsiClass toClass,  String... exceptions);

    /**
     * @param psiElement psiClass or enum constant without class initializer
     */
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createImplementMethodsFix( PsiElement psiElement);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createImplementMethodsFix( PsiClass psiElement);
    
    public abstract LocalQuickFixOnPsiElement createMethodThrowsFix( PsiMethod method,  PsiClassType exceptionClass, boolean shouldThrow, boolean showContainingClass);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createAddDefaultConstructorFix( PsiClass aClass);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createAddConstructorFix( PsiClass aClass, @PsiModifier.ModifierConstant  String modifier);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createMethodParameterTypeFix( PsiMethod method, int index,  PsiType newType, boolean fixWholeHierarchy);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createMakeClassInterfaceFix( PsiClass aClass);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createMakeClassInterfaceFix( PsiClass aClass, final boolean makeInterface);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createExtendsListFix( PsiClass aClass,  PsiClassType typeToExtendFrom, boolean toAdd);
    
    public abstract LocalQuickFixAndIntentionActionOnPsiElement createRemoveUnusedParameterFix( PsiParameter parameter);
    
    public abstract IntentionAction createRemoveUnusedVariableFix( PsiVariable variable);

    
    public abstract IntentionAction createCreateClassOrPackageFix( PsiElement context,  String qualifiedName, final boolean createClass, final String superClass);
    
    public abstract IntentionAction createCreateClassOrInterfaceFix( PsiElement context,  String qualifiedName, final boolean createClass, final String superClass);
    
    public abstract IntentionAction createCreateFieldOrPropertyFix( PsiClass aClass,  String name,  PsiType type,  PropertyMemberType targetMember,  PsiAnnotation... annotations);

    
    public abstract IntentionAction createSetupJDKFix();

     public abstract IntentionAction createAddExceptionToCatchFix();

     public abstract IntentionAction createAddExceptionToThrowsFix( PsiElement element);

     public abstract IntentionAction createSurroundWithTryCatchFix( PsiElement element);

     public abstract IntentionAction createGeneralizeCatchFix( PsiElement element,  PsiClassType type);

     public abstract IntentionAction createChangeToAppendFix( IElementType sign,  PsiType type,  PsiAssignmentExpression assignment);

     public abstract IntentionAction createAddTypeCastFix( PsiType type,  PsiExpression expression);

     public abstract IntentionAction createWrapExpressionFix( PsiType type,  PsiExpression expression);

     public abstract IntentionAction createReuseVariableDeclarationFix( PsiLocalVariable variable);

     public abstract IntentionAction createConvertToStringLiteralAction();

     public abstract IntentionAction createDeleteCatchFix( PsiParameter parameter);

     public abstract IntentionAction createDeleteMultiCatchFix( PsiTypeElement element);

     public abstract IntentionAction createConvertSwitchToIfIntention( PsiSwitchStatement statement);

     public abstract IntentionAction createNegationBroadScopeFix( PsiPrefixExpression expr);

     public abstract IntentionAction createCreateFieldFromUsageFix( PsiReferenceExpression place);

     public abstract IntentionAction createReplaceWithListAccessFix( PsiArrayAccessExpression expression);

     public abstract IntentionAction createAddNewArrayExpressionFix( PsiArrayInitializerExpression expression);

     public abstract IntentionAction createMoveCatchUpFix( PsiCatchSection section,  PsiCatchSection section1);

     public abstract IntentionAction createRenameWrongRefFix( PsiReferenceExpression ref);

     public abstract IntentionAction createRemoveQualifierFix( PsiExpression qualifier,  PsiReferenceExpression expression,  PsiClass resolved);

     public abstract IntentionAction createRemoveParameterListFix( PsiMethod parent);

     public abstract IntentionAndQuickFixAction createShowModulePropertiesFix( PsiElement element);
     public abstract IntentionAndQuickFixAction createShowModulePropertiesFix( Module module);

     public abstract IntentionAction createIncreaseLanguageLevelFix( LanguageLevel level);

     public abstract IntentionAction createChangeParameterClassFix( PsiClass aClass,  PsiClassType type);

     public abstract IntentionAction createReplaceInaccessibleFieldWithGetterSetterFix( PsiElement element,  PsiMethod getter, boolean isSetter);

     public abstract IntentionAction createSurroundWithArrayFix( PsiCall methodCall,  PsiExpression expression);

     public abstract IntentionAction createImplementAbstractClassMethodsFix( PsiElement elementToHighlight);

     public abstract IntentionAction createMoveClassToSeparateFileFix( PsiClass aClass);

     public abstract IntentionAction createRenameFileFix( String newName);

     public abstract LocalQuickFixAndIntentionActionOnPsiElement createRenameElementFix( PsiNamedElement element);
     public abstract LocalQuickFixAndIntentionActionOnPsiElement createRenameElementFix( PsiNamedElement element,  String newName);

     public abstract IntentionAction createChangeExtendsToImplementsFix( PsiClass aClass,  PsiClassType classToExtendFrom);

     public abstract IntentionAction createCreateConstructorMatchingSuperFix( PsiClass aClass);

     public abstract IntentionAction createRemoveNewQualifierFix( PsiNewExpression expression,  PsiClass aClass);

     public abstract IntentionAction createSuperMethodReturnFix( PsiMethod superMethod,  PsiType superMethodType);

     public abstract IntentionAction createInsertNewFix( PsiMethodCallExpression call,  PsiClass aClass);

     public abstract IntentionAction createAddMethodBodyFix( PsiMethod method);

     public abstract IntentionAction createDeleteMethodBodyFix( PsiMethod method);

     public abstract IntentionAction createInsertSuperFix( PsiMethod constructor);

     public abstract IntentionAction createChangeMethodSignatureFromUsageFix( PsiMethod targetMethod,
                                                                                      PsiExpression[] expressions,
                                                                                      PsiSubstitutor substitutor,
                                                                                      PsiElement context,
                                                                                     boolean changeAllUsages, int minUsagesNumberToShowDialog);

     public abstract IntentionAction createChangeMethodSignatureFromUsageReverseOrderFix( PsiMethod targetMethod,
                                                                                                  PsiExpression[] expressions,
                                                                                                  PsiSubstitutor substitutor,
                                                                                                  PsiElement context,
                                                                                                 boolean changeAllUsages,
                                                                                                 int minUsagesNumberToShowDialog);

     public abstract IntentionAction createCreateMethodFromUsageFix( PsiMethodCallExpression call);
     public abstract IntentionAction createCreateMethodFromUsageFix(PsiMethodReferenceExpression methodReferenceExpression);

     public abstract IntentionAction createCreateAbstractMethodFromUsageFix( PsiMethodCallExpression call);

     public abstract IntentionAction createCreatePropertyFromUsageFix( PsiMethodCallExpression call);

     public abstract IntentionAction createCreateConstructorFromSuperFix( PsiMethodCallExpression call);

     public abstract IntentionAction createCreateConstructorFromThisFix( PsiMethodCallExpression call);

     public abstract IntentionAction createCreateGetterSetterPropertyFromUsageFix( PsiMethodCallExpression call);

     public abstract IntentionAction createStaticImportMethodFix( PsiMethodCallExpression call);

     public abstract IntentionAction createReplaceAddAllArrayToCollectionFix( PsiMethodCallExpression call);

     public abstract IntentionAction createCreateConstructorFromCallFix( PsiConstructorCall call);

    
    public abstract List<IntentionAction> getVariableTypeFromCallFixes( PsiMethodCallExpression call,  PsiExpressionList list);

     public abstract IntentionAction createAddReturnFix( PsiMethod method);

     public abstract IntentionAction createAddVariableInitializerFix( PsiVariable variable);

     public abstract IntentionAction createDeferFinalAssignmentFix( PsiVariable variable,  PsiReferenceExpression expression);

     public abstract IntentionAction createVariableAccessFromInnerClassFix( PsiVariable variable,  PsiElement scope);

     public abstract IntentionAction createCreateConstructorParameterFromFieldFix( PsiField field);

     public abstract IntentionAction createInitializeFinalFieldInConstructorFix( PsiField field);

     public abstract IntentionAction createRemoveTypeArgumentsFix( PsiElement variable);

     public abstract IntentionAction createChangeClassSignatureFromUsageFix( PsiClass owner,  PsiReferenceParameterList parameterList);

     public abstract IntentionAction createReplacePrimitiveWithBoxedTypeAction( PsiTypeElement element,  String typeName,  String boxedTypeName);

     public abstract IntentionAction createMakeVarargParameterLastFix( PsiParameter parameter);

     public abstract IntentionAction createMoveBoundClassToFrontFix( PsiClass aClass,  PsiClassType type);

    public abstract void registerPullAsAbstractUpFixes( PsiMethod method,  QuickFixActionRegistrar registrar);

    
    public abstract IntentionAction createCreateAnnotationMethodFromUsageFix( PsiNameValuePair pair);

    
    public abstract IntentionAction createOptimizeImportsFix(boolean onTheFly);

    public abstract void registerFixesForUnusedParameter( PsiParameter parameter,  Object highlightInfo);

    
    public abstract IntentionAction createAddToDependencyInjectionAnnotationsFix( Project project,  String qualifiedName,  String element);

    
    public abstract IntentionAction createCreateGetterOrSetterFix(boolean createGetter, boolean createSetter,  PsiField field);

    
    public abstract IntentionAction createRenameToIgnoredFix( PsiNamedElement namedElement);

    
    public abstract IntentionAction createEnableOptimizeImportsOnTheFlyFix();

    
    public abstract IntentionAction createSafeDeleteFix( PsiElement element);

    
    public abstract List<LocalQuickFix> registerOrderEntryFixes( QuickFixActionRegistrar registrar,
                                                                 PsiReference reference);

    
    public abstract IntentionAction createAddMissingRequiredAnnotationParametersFix( PsiAnnotation annotation,
                                                                                     PsiMethod[] annotationMethods,
                                                                                     Collection<String> missedElements);
    
    public abstract IntentionAction createSurroundWithQuotesAnnotationParameterValueFix( PsiAnnotationMemberValue value,  PsiType expectedType);

    
    public abstract IntentionAction addMethodQualifierFix( PsiMethodCallExpression methodCall);
}
