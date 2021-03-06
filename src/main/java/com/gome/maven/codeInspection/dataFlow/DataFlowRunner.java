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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jan 28, 2002
 * Time: 10:16:39 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.dataFlow;

import com.gome.maven.codeInspection.dataFlow.instructions.*;
import com.gome.maven.codeInspection.dataFlow.value.DfaValueFactory;
import com.gome.maven.codeInspection.dataFlow.value.DfaVariableValue;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;

import java.util.*;

public class DataFlowRunner {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.dataFlow.DataFlowRunner");
    private static final Key<Integer> TOO_EXPENSIVE_HASH = Key.create("TOO_EXPENSIVE_HASH");

    private Instruction[] myInstructions;
    private final MultiMap<PsiElement, DfaMemoryState> myNestedClosures = new MultiMap<PsiElement, DfaMemoryState>();
    private final DfaValueFactory myValueFactory;

    // Maximum allowed attempts to process instruction. Fail as too complex to process if certain instruction
    // is executed more than this limit times.
    public static final int MAX_STATES_PER_BRANCH = 300;

    protected DataFlowRunner() {
        this(false, true);
    }

    protected DataFlowRunner(boolean unknownMembersAreNullable, boolean honorFieldInitializers) {
        myValueFactory = new DfaValueFactory(honorFieldInitializers, unknownMembersAreNullable);
    }

    public DfaValueFactory getFactory() {
        return myValueFactory;
    }

    protected void prepareAnalysis( PsiElement psiBlock, Iterable<DfaMemoryState> initialStates) {
    }

    
    private Collection<DfaMemoryState> createInitialStates( PsiElement psiBlock, InstructionVisitor visitor) {
        PsiClass containingClass = PsiTreeUtil.getParentOfType(psiBlock, PsiClass.class);
        if (containingClass != null && PsiUtil.isLocalOrAnonymousClass(containingClass)) {
            final PsiElement parent = containingClass.getParent();
            final PsiCodeBlock block = DfaPsiUtil.getTopmostBlockInSameClass(parent);
            if ((parent instanceof PsiNewExpression || parent instanceof PsiDeclarationStatement) && block != null) {
                final RunnerResult result = analyzeMethod(block, visitor);
                if (result == RunnerResult.OK) {
                    final Collection<DfaMemoryState> closureStates = myNestedClosures.get(DfaPsiUtil.getTopmostBlockInSameClass(psiBlock));
                    if (!closureStates.isEmpty()) {
                        return closureStates;
                    }
                }
                return null;
            }
        }

        return Arrays.asList(createMemoryState());
    }

    public final RunnerResult analyzeMethod( PsiElement psiBlock, InstructionVisitor visitor) {
        Collection<DfaMemoryState> initialStates = createInitialStates(psiBlock, visitor);
        return initialStates == null ? RunnerResult.NOT_APPLICABLE : analyzeMethod(psiBlock, visitor, false, initialStates);
    }

    public final RunnerResult analyzeMethod( PsiElement psiBlock,
                                            InstructionVisitor visitor,
                                            boolean ignoreAssertions,
                                             Collection<DfaMemoryState> initialStates) {
        try {
            prepareAnalysis(psiBlock, initialStates);

            final ControlFlow flow = createControlFlowAnalyzer().buildControlFlow(psiBlock, ignoreAssertions);
            if (flow == null) return RunnerResult.NOT_APPLICABLE;

            int endOffset = flow.getInstructionCount();
            myInstructions = flow.getInstructions();
            myNestedClosures.clear();

            Set<Instruction> joinInstructions = ContainerUtil.newHashSet();
            for (int index = 0; index < myInstructions.length; index++) {
                Instruction instruction = myInstructions[index];
                if (instruction instanceof GotoInstruction) {
                    joinInstructions.add(myInstructions[((GotoInstruction)instruction).getOffset()]);
                } else if (instruction instanceof ConditionalGotoInstruction) {
                    joinInstructions.add(myInstructions[((ConditionalGotoInstruction)instruction).getOffset()]);
                } else if (instruction instanceof MethodCallInstruction && !((MethodCallInstruction)instruction).getContracts().isEmpty()) {
                    joinInstructions.add(myInstructions[index + 1]);
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Analyzing code block: " + psiBlock.getText());
                for (int i = 0; i < myInstructions.length; i++) {
                    LOG.debug(i + ": " + myInstructions[i].toString());
                }
            }
            //for (int i = 0; i < myInstructions.length; i++) System.out.println(i + ": " + myInstructions[i].toString());

            Integer tooExpensiveHash = psiBlock.getUserData(TOO_EXPENSIVE_HASH);
            if (tooExpensiveHash != null && tooExpensiveHash == psiBlock.getText().hashCode()) {
                LOG.debug("Too complex because hasn't changed since being too complex already");
                return RunnerResult.TOO_COMPLEX;
            }

            final StateQueue queue = new StateQueue();
            for (final DfaMemoryState initialState : initialStates) {
                queue.offer(new DfaInstructionState(myInstructions[0], initialState));
            }

            MultiMap<BranchingInstruction, DfaMemoryState> processedStates = MultiMap.createSet();
            MultiMap<BranchingInstruction, DfaMemoryState> incomingStates = MultiMap.createSet();

            long msLimit = shouldCheckTimeLimit() ? Registry.intValue("ide.dfa.time.limit.online") : Registry.intValue("ide.dfa.time.limit.offline");
            WorkingTimeMeasurer measurer = new WorkingTimeMeasurer(msLimit * 1000 * 1000);
            int count = 0;
            while (!queue.isEmpty()) {
                for (DfaInstructionState instructionState : queue.getNextInstructionStates(joinInstructions)) {
                    if (count++ % 1024 == 0 && measurer.isTimeOver()) {
                        LOG.debug("Too complex because the analysis took too long");
                        psiBlock.putUserData(TOO_EXPENSIVE_HASH, psiBlock.getText().hashCode());
                        return RunnerResult.TOO_COMPLEX;
                    }
                    ProgressManager.checkCanceled();

                    if (LOG.isDebugEnabled()) {
                        LOG.debug(instructionState.toString());
                    }
                    //System.out.println(instructionState.toString());

                    Instruction instruction = instructionState.getInstruction();

                    if (instruction instanceof BranchingInstruction) {
                        BranchingInstruction branching = (BranchingInstruction)instruction;
                        Collection<DfaMemoryState> processed = processedStates.get(branching);
                        if (processed.contains(instructionState.getMemoryState())) {
                            continue;
                        }
                        if (processed.size() > MAX_STATES_PER_BRANCH) {
                            LOG.debug("Too complex because too many different possible states");
                            return RunnerResult.TOO_COMPLEX; // Too complex :(
                        }
                        processedStates.putValue(branching, instructionState.getMemoryState().createCopy());
                    }

                    DfaInstructionState[] after = acceptInstruction(visitor, instructionState);
                    for (DfaInstructionState state : after) {
                        Instruction nextInstruction = state.getInstruction();
                        if (nextInstruction.getIndex() >= endOffset) {
                            continue;
                        }
                        if (nextInstruction instanceof BranchingInstruction) {
                            BranchingInstruction branching = (BranchingInstruction)nextInstruction;
                            if (processedStates.get(branching).contains(state.getMemoryState()) ||
                                    incomingStates.get(branching).contains(state.getMemoryState())) {
                                continue;
                            }
                            incomingStates.putValue(branching, state.getMemoryState().createCopy());
                        }
                        queue.offer(state);
                    }
                }
            }

            psiBlock.putUserData(TOO_EXPENSIVE_HASH, null);
            LOG.debug("Analysis ok");
            return RunnerResult.OK;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            LOG.error(psiBlock.getText(), e);
            return RunnerResult.ABORTED;
        }
        catch (EmptyStackException e) {
            LOG.error(psiBlock.getText(), e);
            return RunnerResult.ABORTED;
        }
    }

    protected boolean shouldCheckTimeLimit() {
        return !ApplicationManager.getApplication().isUnitTestMode();
    }

    protected DfaInstructionState[] acceptInstruction(InstructionVisitor visitor, DfaInstructionState instructionState) {
        Instruction instruction = instructionState.getInstruction();
        PsiElement closure = DfaUtil.getClosureInside(instruction);
        if (closure instanceof PsiClass) {
            registerNestedClosures(instructionState, (PsiClass)closure);
        } else if (closure instanceof PsiLambdaExpression) {
            registerNestedClosures(instructionState, (PsiLambdaExpression)closure);
        }

        return instruction.accept(this, instructionState.getMemoryState(), visitor);
    }

    private void registerNestedClosures(DfaInstructionState instructionState, PsiClass nestedClass) {
        DfaMemoryState state = instructionState.getMemoryState();
        for (PsiMethod method : nestedClass.getMethods()) {
            PsiCodeBlock body = method.getBody();
            if (body != null) {
                myNestedClosures.putValue(body, createClosureState(state));
            }
        }
        for (PsiClassInitializer initializer : nestedClass.getInitializers()) {
            myNestedClosures.putValue(initializer.getBody(), createClosureState(state));
        }
        for (PsiField field : nestedClass.getFields()) {
            myNestedClosures.putValue(field, createClosureState(state));
        }
    }

    private void registerNestedClosures(DfaInstructionState instructionState, PsiLambdaExpression expr) {
        DfaMemoryState state = instructionState.getMemoryState();
        PsiElement body = expr.getBody();
        if (body != null) {
            myNestedClosures.putValue(body, createClosureState(state));
        }
    }

    protected ControlFlowAnalyzer createControlFlowAnalyzer() {
        return new ControlFlowAnalyzer(myValueFactory);
    }

    protected DfaMemoryState createMemoryState() {
        return new DfaMemoryStateImpl(myValueFactory);
    }

    public Instruction[] getInstructions() {
        return myInstructions;
    }

    public Instruction getInstruction(int index) {
        return myInstructions[index];
    }

    public MultiMap<PsiElement, DfaMemoryState> getNestedClosures() {
        return new MultiMap<PsiElement, DfaMemoryState>(myNestedClosures);
    }

    public Pair<Set<Instruction>,Set<Instruction>> getConstConditionalExpressions() {
        Set<Instruction> trueSet = new HashSet<Instruction>();
        Set<Instruction> falseSet = new HashSet<Instruction>();

        for (Instruction instruction : myInstructions) {
            if (instruction instanceof BranchingInstruction) {
                BranchingInstruction branchingInstruction = (BranchingInstruction)instruction;
                if (branchingInstruction.getPsiAnchor() != null && branchingInstruction.isConditionConst()) {
                    if (!branchingInstruction.isTrueReachable()) {
                        falseSet.add(branchingInstruction);
                    }

                    if (!branchingInstruction.isFalseReachable()) {
                        trueSet.add(branchingInstruction);
                    }
                }
            }
        }

        for (Instruction instruction : myInstructions) {
            if (instruction instanceof BranchingInstruction) {
                BranchingInstruction branchingInstruction = (BranchingInstruction)instruction;
                if (branchingInstruction.isTrueReachable()) {
                    falseSet.remove(branchingInstruction);
                }
                if (branchingInstruction.isFalseReachable()) {
                    trueSet.remove(branchingInstruction);
                }
            }
        }

        return Pair.create(trueSet, falseSet);
    }

    private static DfaMemoryStateImpl createClosureState(DfaMemoryState memState) {
        DfaMemoryStateImpl copy = (DfaMemoryStateImpl)memState.createCopy();
        copy.flushFields();
        Set<DfaVariableValue> vars = new HashSet<DfaVariableValue>(copy.getVariableStates().keySet());
        for (DfaVariableValue value : vars) {
            copy.flushDependencies(value);
        }
        copy.emptyStack();
        return copy;
    }
}
