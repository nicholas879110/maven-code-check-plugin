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
package com.gome.maven.psi.impl;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.CachedValue;
import com.gome.maven.psi.util.CachedValueProvider;
import com.gome.maven.psi.util.CachedValuesManager;
import com.gome.maven.psi.util.PsiModificationTracker;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class JavaConstantExpressionEvaluator extends JavaRecursiveElementWalkingVisitor {
    private final Factory<ConcurrentMap<PsiElement, Object>> myMapFactory;
    private final Project myProject;

    private static final Key<CachedValue<ConcurrentMap<PsiElement,Object>>> CONSTANT_VALUE_WO_OVERFLOW_MAP_KEY = Key.create("CONSTANT_VALUE_WO_OVERFLOW_MAP_KEY");
    private static final Key<CachedValue<ConcurrentMap<PsiElement,Object>>> CONSTANT_VALUE_WITH_OVERFLOW_MAP_KEY = Key.create("CONSTANT_VALUE_WITH_OVERFLOW_MAP_KEY");
    private static final Object NO_VALUE = ObjectUtils.NULL;
    private final ConstantExpressionVisitor myConstantExpressionVisitor;

    private JavaConstantExpressionEvaluator(Set<PsiVariable> visitedVars,
                                            final boolean throwExceptionOnOverflow,
                                             Project project,
                                            final PsiConstantEvaluationHelper.AuxEvaluator auxEvaluator) {
        myMapFactory = auxEvaluator == null ? new Factory<ConcurrentMap<PsiElement, Object>>() {
            @Override
            public ConcurrentMap<PsiElement, Object> create() {
                final Key<CachedValue<ConcurrentMap<PsiElement, Object>>> key =
                        throwExceptionOnOverflow ? CONSTANT_VALUE_WITH_OVERFLOW_MAP_KEY : CONSTANT_VALUE_WO_OVERFLOW_MAP_KEY;
                return CachedValuesManager.getManager(myProject).getCachedValue(myProject, key, PROVIDER, false);
            }
        } : new Factory<ConcurrentMap<PsiElement, Object>>() {
            @Override
            public ConcurrentMap<PsiElement, Object> create() {
                return auxEvaluator.getCacheMap(throwExceptionOnOverflow);
            }
        };
        myProject = project;
        myConstantExpressionVisitor = new ConstantExpressionVisitor(visitedVars, throwExceptionOnOverflow, auxEvaluator);
    }

    @Override
    protected void elementFinished( PsiElement element) {
        Object value = getCached(element);
        if (value == null) {
            Object result = myConstantExpressionVisitor.handle(element);
            cache(element, result);
        }
    }

    @Override
    public void visitElement(PsiElement element) {
        Object value = getCached(element);
        if (value == null) {
            super.visitElement(element);
            // will cache back in elementFinished()
        }
        else {
            ConstantExpressionVisitor.store(element, value == NO_VALUE ? null : value);
        }
    }

    private static final CachedValueProvider<ConcurrentMap<PsiElement,Object>> PROVIDER = new CachedValueProvider<ConcurrentMap<PsiElement,Object>>() {
        @Override
        public Result<ConcurrentMap<PsiElement,Object>> compute() {
            ConcurrentMap<PsiElement, Object> value = ContainerUtil.createConcurrentSoftMap();
            return Result.create(value, PsiModificationTracker.MODIFICATION_COUNT);
        }
    };

    private Object getCached( PsiElement element) {
        return map().get(element);
    }
    private Object cache( PsiElement element,  Object value) {
        value = ConcurrencyUtil.cacheOrGet(map(), element, value == null ? NO_VALUE : value);
        if (value == NO_VALUE) {
            value = null;
        }
        return value;
    }

    
    private ConcurrentMap<PsiElement, Object> map() {
        return myMapFactory.create();
    }

    public static Object computeConstantExpression( PsiExpression expression,  Set<PsiVariable> visitedVars, boolean throwExceptionOnOverflow) {
        return computeConstantExpression(expression, visitedVars, throwExceptionOnOverflow, null);
    }

    public static Object computeConstantExpression( PsiExpression expression,
                                                    Set<PsiVariable> visitedVars,
                                                   boolean throwExceptionOnOverflow,
                                                   final PsiConstantEvaluationHelper.AuxEvaluator auxEvaluator) {
        if (expression == null) return null;

        JavaConstantExpressionEvaluator evaluator = new JavaConstantExpressionEvaluator(visitedVars, throwExceptionOnOverflow, expression.getProject(), auxEvaluator);

        if (expression instanceof PsiCompiledElement) {
            // in case of compiled elements we are not allowed to use PSI walking
            // but really in Cls there are only so many cases to handle
            if (expression instanceof PsiPrefixExpression) {
                PsiElement operand = ((PsiPrefixExpression)expression).getOperand();
                if (operand == null) return null;
                Object value = evaluator.myConstantExpressionVisitor.handle(operand);
                ConstantExpressionVisitor.store(operand, value);
            }
            return evaluator.myConstantExpressionVisitor.handle(expression);
        }
        expression.accept(evaluator);
        Object cached = evaluator.getCached(expression);
        return cached == NO_VALUE ? null : cached;
    }

    public static Object computeConstantExpression( PsiExpression expression, boolean throwExceptionOnOverflow) {
        return computeConstantExpression(expression, null, throwExceptionOnOverflow);
    }
}
