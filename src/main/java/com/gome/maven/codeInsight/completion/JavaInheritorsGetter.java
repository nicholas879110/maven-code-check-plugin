/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.CodeInsightUtil;
import com.gome.maven.codeInsight.ExpectedTypeInfo;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightClassUtil;
import com.gome.maven.codeInsight.lookup.AutoCompletionPolicy;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementDecorator;
import com.gome.maven.codeInsight.lookup.PsiTypeLookupItem;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.filters.getters.ExpectedTypesGetter;
import com.gome.maven.psi.impl.source.PsiClassReferenceType;
import com.gome.maven.psi.statistics.JavaStatisticsManager;
import com.gome.maven.psi.statistics.StatisticsInfo;
import com.gome.maven.psi.statistics.StatisticsManager;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.gome.maven.patterns.PsiJavaPatterns.psiElement;

/**
 * @author peter
 */
public class JavaInheritorsGetter extends CompletionProvider<CompletionParameters> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.completion.JavaInheritorsGetter");
    private final ConstructorInsertHandler myConstructorInsertHandler;

    public JavaInheritorsGetter(final ConstructorInsertHandler constructorInsertHandler) {
        myConstructorInsertHandler = constructorInsertHandler;
    }

    private static boolean shouldAddArrayInitializer(PsiElement position) {
        if (!JavaCompletionContributor.isInJavaContext(position) || !JavaSmartCompletionContributor.AFTER_NEW.accepts(position)) {
            return false;
        }
        PsiNewExpression newExpression = PsiTreeUtil.getParentOfType(position, PsiNewExpression.class);
        return newExpression != null && newExpression.getParent() instanceof PsiExpressionList;
    }

    @Override
    public void addCompletions( final CompletionParameters parameters, final ProcessingContext matchingContext,  final CompletionResultSet result) {
        final ExpectedTypeInfo[] infos = JavaSmartCompletionContributor.getExpectedTypes(parameters);

        final List<ExpectedTypeInfo> infoCollection = Arrays.asList(infos);
        generateVariants(parameters, result.getPrefixMatcher(), infos, new Consumer<LookupElement>() {
            @Override
            public void consume(LookupElement lookupElement) {
                result.addElement(JavaSmartCompletionContributor.decorate(lookupElement, infoCollection));
            }
        });
    }

    public void generateVariants(final CompletionParameters parameters, final PrefixMatcher prefixMatcher, final Consumer<LookupElement> consumer) {
        generateVariants(parameters, prefixMatcher, JavaSmartCompletionContributor.getExpectedTypes(parameters), consumer);
    }

    private void generateVariants(final CompletionParameters parameters, final PrefixMatcher prefixMatcher,
                                  final ExpectedTypeInfo[] infos, final Consumer<LookupElement> consumer) {

        addArrayTypes(parameters.getPosition(), infos, consumer);

        processInheritors(parameters, extractClassTypes(infos), prefixMatcher, new Consumer<PsiType>() {
            @Override
            public void consume(final PsiType type) {
                final LookupElement element = addExpectedType(type, parameters);
                if (element != null) {
                    consumer.consume(element);
                }
            }
        });
    }

    private static void addArrayTypes(PsiElement identifierCopy,
                                      ExpectedTypeInfo[] infos, final Consumer<LookupElement> consumer) {

        for (final PsiType type : ExpectedTypesGetter.extractTypes(infos, true)) {
            if (type instanceof PsiArrayType) {
                consumer.consume(createNewArrayItem(identifierCopy, type));

                if (shouldAddArrayInitializer(identifierCopy)) {
                    PsiTypeLookupItem item = createNewArrayItem(identifierCopy, type);
                    item.setAddArrayInitializer();
                    consumer.consume(item);
                }
            }
        }
    }

    private static PsiTypeLookupItem createNewArrayItem(PsiElement identifierCopy, PsiType type) {
        PsiTypeLookupItem item = PsiTypeLookupItem.createLookupItem(TypeConversionUtil.erasure(type), identifierCopy);
        if (item.getObject() instanceof PsiClass) {
            JavaCompletionUtil.setShowFQN(item);
        }
        item.setInsertHandler(new DefaultInsertHandler()); //braces & shortening
        return item;
    }

    private static List<PsiClassType> extractClassTypes(ExpectedTypeInfo[] infos) {
        final List<PsiClassType> expectedClassTypes = new SmartList<PsiClassType>();
        for (PsiType type : ExpectedTypesGetter.extractTypes(infos, true)) {
            if (type instanceof PsiClassType) {
                final PsiClassType classType = (PsiClassType)type;
                if (classType.resolve() != null) {
                    expectedClassTypes.add(classType);
                }
            }
        }
        return expectedClassTypes;
    }


    private LookupElement addExpectedType(final PsiType type,
                                          final CompletionParameters parameters) {
        if (!JavaCompletionUtil.hasAccessibleConstructor(type)) return null;

        final PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if (psiClass == null || psiClass.getName() == null) return null;

        PsiElement position = parameters.getPosition();
        if ((parameters.getInvocationCount() < 2 || psiClass instanceof PsiCompiledElement) &&
                HighlightClassUtil.checkCreateInnerClassFromStaticContext(position, null, psiClass) != null &&
                !psiElement().afterLeaf(psiElement().withText(PsiKeyword.NEW).afterLeaf(".")).accepts(position)) {
            return null;
        }

        PsiType psiType = GenericsUtil.eliminateWildcards(type);
        if (JavaSmartCompletionContributor.AFTER_NEW.accepts(parameters.getOriginalPosition()) &&
                PsiUtil.getLanguageLevel(parameters.getOriginalFile()).isAtLeast(LanguageLevel.JDK_1_7)) {
            final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
            if (psiClass.hasTypeParameters() && !((PsiClassType)type).isRaw()) {
                final String erasedText = TypeConversionUtil.erasure(psiType).getCanonicalText();
                String canonicalText = psiType.getCanonicalText();
                if (canonicalText.contains("?extends") || canonicalText.contains("?super")) {
                    LOG.error("Malformed canonical text: " + canonicalText + "; presentable text: " + psiType + " of " + psiType.getClass() + "; " +
                            (psiType instanceof PsiClassReferenceType ? ((PsiClassReferenceType)psiType).getReference().getClass() : ""));
                    return null;
                }
                try {
                    final PsiStatement statement = elementFactory
                            .createStatementFromText(canonicalText + " v = new " + erasedText + "<>()", parameters.getOriginalFile());
                    final PsiVariable declaredVar = (PsiVariable)((PsiDeclarationStatement)statement).getDeclaredElements()[0];
                    final PsiNewExpression initializer = (PsiNewExpression)declaredVar.getInitializer();
                    final boolean hasDefaultConstructorOrNoGenericsOne = PsiDiamondTypeImpl.hasDefaultConstructor(psiClass) ||
                            !PsiDiamondTypeImpl.haveConstructorsGenericsParameters(psiClass);
                    if (hasDefaultConstructorOrNoGenericsOne) {
                        final PsiDiamondTypeImpl.DiamondInferenceResult inferenceResult = PsiDiamondTypeImpl.resolveInferredTypes(initializer);
                        if (inferenceResult.getErrorMessage() == null &&
                                !psiClass.hasModifierProperty(PsiModifier.ABSTRACT) &&
                                areInferredTypesApplicable(inferenceResult.getTypes(), parameters.getPosition())) {
                            psiType = initializer.getType();
                        }
                    }
                }
                catch (IncorrectOperationException ignore) {}
            }
        }
        final PsiTypeLookupItem item = PsiTypeLookupItem.createLookupItem(psiType, position);
        JavaCompletionUtil.setShowFQN(item);

        if (psiClass.isInterface() || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            item.setAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
            item.setIndicateAnonymous(true);
        }

        return LookupElementDecorator.withInsertHandler(item, myConstructorInsertHandler);
    }

    private static boolean areInferredTypesApplicable( PsiType[] types, PsiElement position) {
        final PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(position, PsiMethodCallExpression.class);
        if (methodCallExpression != null) {
            if (PsiUtil.isLanguageLevel8OrHigher(methodCallExpression)) {
                final PsiNewExpression newExpression = PsiTreeUtil.getParentOfType(position, PsiNewExpression.class, false);
                if (newExpression != null) {
                    PsiElement parent = newExpression;
                    while (parent.getParent() instanceof PsiParenthesizedExpression) {
                        parent = parent.getParent();
                    }
                    final int idx = ArrayUtil.find(methodCallExpression.getArgumentList().getExpressions(), parent);
                    if (idx > -1) {
                        final JavaResolveResult resolveResult = methodCallExpression.resolveMethodGenerics();
                        final PsiMethod method = (PsiMethod)resolveResult.getElement();
                        if (method != null) {
                            final PsiParameter[] parameters = method.getParameterList().getParameters();
                            if (idx < parameters.length) {
                                final PsiType expectedType = resolveResult.getSubstitutor().substitute(parameters[idx].getType());
                                final PsiClass aClass = PsiUtil.resolveClassInType(expectedType);
                                if (aClass != null) {
                                    final PsiClassType inferredArg = JavaPsiFacade.getElementFactory(method.getProject()).createType(aClass, types);
                                    LOG.assertTrue(expectedType != null);
                                    return TypeConversionUtil.isAssignable(expectedType, inferredArg);
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    public static void processInheritors(final CompletionParameters parameters,
                                         final Collection<PsiClassType> expectedClassTypes,
                                         final PrefixMatcher matcher, final Consumer<PsiType> consumer) {
        //quick
        if (!processMostProbableInheritors(parameters, expectedClassTypes, consumer)) return;

        //long
        for (final PsiClassType type : expectedClassTypes) {
            CodeInsightUtil.processSubTypes(type, parameters.getPosition(), false, matcher, consumer);
        }
    }

    private static boolean processMostProbableInheritors(CompletionParameters parameters,
                                                         Collection<PsiClassType> expectedClassTypes,
                                                         Consumer<PsiType> consumer) {
        PsiFile file = parameters.getOriginalFile();
        for (final PsiClassType type : expectedClassTypes) {
            consumer.consume(type);

            final PsiClassType.ClassResolveResult baseResult = JavaCompletionUtil.originalize(type).resolveGenerics();
            final PsiClass baseClass = baseResult.getElement();
            if (baseClass == null) return false;

            final PsiSubstitutor baseSubstitutor = baseResult.getSubstitutor();

            final Processor<PsiClass> processor = CodeInsightUtil.createInheritorsProcessor(parameters.getPosition(), type, 0, false,
                    consumer, baseClass, baseSubstitutor);
            final StatisticsInfo[] stats = StatisticsManager.getInstance().getAllValues(JavaStatisticsManager.getAfterNewKey(type));
            for (final StatisticsInfo statisticsInfo : stats) {
                final String value = statisticsInfo.getValue();
                if (value.startsWith(JavaStatisticsManager.CLASS_PREFIX)) {
                    final String qname = value.substring(JavaStatisticsManager.CLASS_PREFIX.length());
                    final PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(qname, file.getResolveScope());
                    if (psiClass != null && !PsiTreeUtil.isAncestor(file, psiClass, true) && !processor.process(psiClass)) break;
                }
            }
        }
        return true;
    }
}
