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
 * Date: Jan 26, 2002
 * Time: 10:47:33 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.dataFlow.instructions;

import com.gome.maven.codeInspection.dataFlow.DataFlowRunner;
import com.gome.maven.codeInspection.dataFlow.DfaInstructionState;
import com.gome.maven.codeInspection.dataFlow.DfaMemoryState;
import com.gome.maven.codeInspection.dataFlow.InstructionVisitor;
import com.gome.maven.codeInspection.dataFlow.value.DfaValue;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiVariable;

public class AssignInstruction extends Instruction {
    private final PsiExpression myRExpression;
     private final DfaValue myAssignedValue;

    public AssignInstruction(PsiExpression RExpression,  DfaValue assignedValue) {
        myRExpression = RExpression;
        myAssignedValue = assignedValue;
    }

    @Override
    public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
        return visitor.visitAssign(this, runner, stateBefore);
    }

    
    public PsiExpression getRExpression() {
        return myRExpression;
    }

    public boolean isVariableInitializer() {
        return myRExpression != null && myRExpression.getParent() instanceof PsiVariable;
    }

    
    public DfaValue getAssignedValue() {
        return myAssignedValue;
    }

    public String toString() {
        return "ASSIGN";
    }
}