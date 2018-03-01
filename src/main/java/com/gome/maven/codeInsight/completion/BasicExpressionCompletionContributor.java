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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.guess.GuessManager;
import com.gome.maven.codeInsight.lookup.*;
import com.gome.maven.codeInsight.template.SmartCompletionContextType;
import com.gome.maven.codeInsight.template.impl.TemplateImpl;
import com.gome.maven.codeInsight.template.impl.TemplateSettings;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.filters.getters.ClassLiteralGetter;
import com.gome.maven.psi.filters.getters.ThisGetter;
import com.gome.maven.psi.scope.BaseScopeProcessor;
import com.gome.maven.psi.scope.util.PsiScopesUtil;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Map;

/**
 * @author peter
 */
public class BasicExpressionCompletionContributor {

    private static void addKeyword(final Consumer<LookupElement> result, final PsiElement element, final String s) {
        result.consume(createKeywordLookupItem(element, s));
    }

    public static LookupElement createKeywordLookupItem(final PsiElement element, final String s) {
        try {
            final PsiKeyword keyword = JavaPsiFacade.getInstance(element.getProject()).getElementFactory().createKeyword(s, element);
            return new KeywordLookupItem(keyword, element).setAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE);
        }
        catch (IncorrectOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fillCompletionVariants(JavaSmartCompletionParameters parameters,
                                              final Consumer<LookupElement> result,
                                              PrefixMatcher matcher) {
        final PsiElement element = parameters.getPosition();
        if (JavaCompletionData.isAfterTypeDot(element)) {
            addKeyword(result, element, PsiKeyword.CLASS);
            addKeyword(result, element, PsiKeyword.THIS);

        }

        if (!JavaCompletionData.AFTER_DOT.accepts(element)) {
            if (parameters.getParameters().getInvocationCount() <= 1) {
                new CollectionsUtilityMethodsProvider(parameters.getPosition(),
                        parameters.getExpectedType(),
                        parameters.getDefaultType(), result)
                        .addCompletions(StringUtil.isNotEmpty(matcher.getPrefix()));
            }
            ClassLiteralGetter.addCompletions(parameters, result, matcher);

            final PsiElement position = parameters.getPosition();
            final PsiType expectedType = parameters.getExpectedType();

            for (final TemplateImpl template : TemplateSettings.getInstance().getTemplates()) {
                if (!template.isDeactivated() && template.getTemplateContext().isEnabled(new SmartCompletionContextType())) {
                    result.consume(new SmartCompletionTemplateItem(template, position));
                }
            }

            addKeyword(result, position, PsiKeyword.TRUE);
            addKeyword(result, position, PsiKeyword.FALSE);

            final PsiElement parent = position.getParent();
            if (parent != null && !(parent.getParent() instanceof PsiSwitchLabelStatement)) {
                for (final PsiExpression expression : ThisGetter.getThisExpressionVariants(position)) {
                    result.consume(new ExpressionLookupItem(expression));
                }
            }

            processDataflowExpressionTypes(position, expectedType, matcher, result);
        }

    }

    public static void processDataflowExpressionTypes(PsiElement position,  PsiType expectedType, final PrefixMatcher matcher, Consumer<LookupElement> consumer) {
        final PsiExpression context = PsiTreeUtil.getParentOfType(position, PsiExpression.class);
        if (context == null) return;

        final Map<PsiExpression,PsiType> map = GuessManager.getInstance(position.getProject()).getControlFlowExpressionTypes(context);
        if (map.isEmpty()) {
            return;
        }

        PsiScopesUtil.treeWalkUp(new BaseScopeProcessor() {
            @Override
            public boolean execute( PsiElement element,  ResolveState state) {
                if (element instanceof PsiLocalVariable) {
                    if (!matcher.prefixMatches(((PsiLocalVariable)element).getName())) {
                        return true;
                    }

                    final PsiExpression expression = ((PsiLocalVariable)element).getInitializer();
                    if (expression instanceof PsiTypeCastExpression) {
                        PsiTypeCastExpression typeCastExpression = (PsiTypeCastExpression)expression;
                        final PsiExpression operand = typeCastExpression.getOperand();
                        if (operand != null) {
                            final PsiType dfaCasted = map.get(operand);
                            if (dfaCasted != null && dfaCasted.equals(typeCastExpression.getType())) {
                                map.remove(operand);
                            }
                        }
                    }
                }
                return true;
            }
        }, context, context.getContainingFile());

        for (final PsiExpression expression : map.keySet()) {
            final PsiType castType = map.get(expression);
            final PsiType baseType = expression.getType();
            if (expectedType == null || (expectedType.isAssignableFrom(castType) && (baseType == null || !expectedType.isAssignableFrom(baseType)))) {
                consumer.consume(CastingLookupElementDecorator.createCastingElement(expressionToLookupElement(expression), castType));
            }
        }
    }

    
    private static LookupElement expressionToLookupElement( PsiExpression expression) {
        if (expression instanceof PsiReferenceExpression) {
            final PsiReferenceExpression refExpr = (PsiReferenceExpression)expression;
            if (!refExpr.isQualified()) {
                final PsiElement target = refExpr.resolve();
                if (target instanceof PsiVariable) {
                    final VariableLookupItem item = new VariableLookupItem((PsiVariable)target);
                    item.setSubstitutor(PsiSubstitutor.EMPTY);
                    return item;
                }
            }
        }
        if (expression instanceof PsiMethodCallExpression) {
            final PsiMethodCallExpression call = (PsiMethodCallExpression)expression;
            if (!call.getMethodExpression().isQualified()) {
                final PsiMethod method = call.resolveMethod();
                if (method != null) {
                    return new JavaMethodCallElement(method);
                }
            }
        }

        return new ExpressionLookupItem(expression);
    }

}
