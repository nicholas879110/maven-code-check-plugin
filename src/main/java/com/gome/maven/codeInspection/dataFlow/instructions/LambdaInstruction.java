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
package com.gome.maven.codeInspection.dataFlow.instructions;

import com.gome.maven.codeInspection.dataFlow.DataFlowRunner;
import com.gome.maven.codeInspection.dataFlow.DfaInstructionState;
import com.gome.maven.codeInspection.dataFlow.DfaMemoryState;
import com.gome.maven.codeInspection.dataFlow.InstructionVisitor;
import com.gome.maven.psi.PsiLambdaExpression;

public class LambdaInstruction extends Instruction {
    private final PsiLambdaExpression myLambdaExpression;

    public LambdaInstruction(PsiLambdaExpression lambdaExpression) {
        myLambdaExpression = lambdaExpression;
    }

    public PsiLambdaExpression getLambdaExpression() {
        return myLambdaExpression;
    }

    @Override
    public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
        return visitor.visitLambdaExpression(this, runner, stateBefore);
    }

    @Override
    public String toString() {
        return "LambdaInstruction";
    }
}
