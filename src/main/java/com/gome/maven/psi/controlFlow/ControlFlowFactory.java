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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Aug 13, 2002
 * Time: 12:07:14 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.psi.controlFlow;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.util.containers.ConcurrentList;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.concurrent.ConcurrentMap;

public class ControlFlowFactory {
    // psiElements hold weakly, controlFlows softly
    private final ConcurrentMap<PsiElement, ConcurrentList<ControlFlowContext>> cachedFlows = ContainerUtil.createConcurrentWeakKeySoftValueMap(100, 0.75f, Runtime.getRuntime().availableProcessors(),
            ContainerUtil.<PsiElement>canonicalStrategy());

    private static final NotNullLazyKey<ControlFlowFactory, Project> INSTANCE_KEY = ServiceManager.createLazyKey(ControlFlowFactory.class);

    public static ControlFlowFactory getInstance(Project project) {
        return INSTANCE_KEY.getValue(project);
    }


    public ControlFlowFactory(PsiManagerEx psiManager) {
        psiManager.registerRunnableToRunOnChange(new Runnable(){
            @Override
            public void run() {
                clearCache();
            }
        });
    }

    private void clearCache() {
        cachedFlows.clear();
    }

    void registerSubRange(final PsiElement codeFragment,
                          final ControlFlowSubRange flow,
                          final boolean evaluateConstantIfConfition,
                          boolean enableShortCircuit, final ControlFlowPolicy policy) {
        registerControlFlow(codeFragment, flow, evaluateConstantIfConfition, enableShortCircuit, policy);
    }

    private static class ControlFlowContext {
        private final ControlFlowPolicy policy;
        private final boolean evaluateConstantIfCondition;
        private final boolean enableShortCircuit;
        private final long modificationCount;
        private final ControlFlow controlFlow;

        private ControlFlowContext(boolean evaluateConstantIfCondition, boolean enableShortCircuit,  ControlFlowPolicy policy, long modificationCount,  ControlFlow controlFlow) {
            this.evaluateConstantIfCondition = evaluateConstantIfCondition;
            this.enableShortCircuit = enableShortCircuit;
            this.policy = policy;
            this.modificationCount = modificationCount;
            this.controlFlow = controlFlow;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ControlFlowContext that = (ControlFlowContext)o;

            return isFor(that);
        }

        @Override
        public int hashCode() {
            int result = policy.hashCode();
            result = 31 * result + (evaluateConstantIfCondition ? 1 : 0);
            result = 31 * result + (int)(modificationCount ^ (modificationCount >>> 32));
            return result;
        }

        private boolean isFor( ControlFlowPolicy policy,
                              final boolean evaluateConstantIfCondition,
                              final boolean enableShortCircuit,
                              long modificationCount) {
            if (modificationCount != this.modificationCount) return false;
            if (!policy.equals(this.policy)) return false;
            if (enableShortCircuit != this.enableShortCircuit) return false;

            // optimization: when no constant condition were computed, both control flows are the same
            if (!controlFlow.isConstantConditionOccurred()) return true;

            return evaluateConstantIfCondition == this.evaluateConstantIfCondition;
        }

        private boolean isFor( ControlFlowContext that) {
            return isFor(that.policy, that.evaluateConstantIfCondition, that.enableShortCircuit, that.modificationCount);
        }
    }

    
    public ControlFlow getControlFlow( PsiElement element,  ControlFlowPolicy policy) throws AnalysisCanceledException {
        return getControlFlow(element, policy, true, true);
    }

    
    public ControlFlow getControlFlow( PsiElement element,  ControlFlowPolicy policy, boolean evaluateConstantIfCondition) throws AnalysisCanceledException {
        return getControlFlow(element, policy, true, evaluateConstantIfCondition);
    }

    
    public ControlFlow getControlFlow( PsiElement element,
                                       ControlFlowPolicy policy,
                                      boolean enableShortCircuit,
                                      boolean evaluateConstantIfCondition) throws AnalysisCanceledException {
        final long modificationCount = element.getManager().getModificationTracker().getModificationCount();
        ConcurrentList<ControlFlowContext> cached = getOrCreateCachedFlowsForElement(element);
        for (ControlFlowContext context : cached) {
            if (context.isFor(policy, evaluateConstantIfCondition, enableShortCircuit, modificationCount)) return context.controlFlow;
        }
        ControlFlow controlFlow = new ControlFlowAnalyzer(element, policy, enableShortCircuit, evaluateConstantIfCondition).buildControlFlow();
        ControlFlowContext context = createContext(evaluateConstantIfCondition, enableShortCircuit, policy, controlFlow, modificationCount);
        cached.addIfAbsent(context);
        return controlFlow;
    }

    
    private static ControlFlowContext createContext(final boolean evaluateConstantIfCondition,
                                                    boolean enableShortCircuit,
                                                     ControlFlowPolicy policy,
                                                     ControlFlow controlFlow,
                                                    final long modificationCount) {
        return new ControlFlowContext(evaluateConstantIfCondition, enableShortCircuit, policy, modificationCount,controlFlow);
    }

    private void registerControlFlow( PsiElement element,
                                      ControlFlow flow,
                                     boolean evaluateConstantIfCondition,
                                     boolean enableShortCircuit,
                                      ControlFlowPolicy policy) {
        final long modificationCount = element.getManager().getModificationTracker().getModificationCount();
        ControlFlowContext controlFlowContext = createContext(evaluateConstantIfCondition, enableShortCircuit, policy, flow, modificationCount);

        ConcurrentList<ControlFlowContext> cached = getOrCreateCachedFlowsForElement(element);
        cached.addIfAbsent(controlFlowContext);
    }

    
    private ConcurrentList<ControlFlowContext> getOrCreateCachedFlowsForElement( PsiElement element) {
        ConcurrentList<ControlFlowContext> cached = cachedFlows.get(element);
        if (cached == null) {
            cached = ContainerUtil.createConcurrentList();
            cachedFlows.put(element, cached);
        }
        return cached;
    }
}

