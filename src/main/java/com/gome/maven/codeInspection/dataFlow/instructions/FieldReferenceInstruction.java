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
package com.gome.maven.codeInspection.dataFlow.instructions;

import com.gome.maven.codeInspection.dataFlow.DataFlowRunner;
import com.gome.maven.codeInspection.dataFlow.DfaInstructionState;
import com.gome.maven.codeInspection.dataFlow.DfaMemoryState;
import com.gome.maven.codeInspection.dataFlow.InstructionVisitor;
import com.gome.maven.psi.PsiArrayAccessExpression;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiReferenceExpression;

/**
 * @author max
 */
public class FieldReferenceInstruction extends Instruction {
    private final PsiExpression myExpression;
     private final String mySyntheticFieldName;

    public FieldReferenceInstruction( PsiExpression expression,   String syntheticFieldName) {
        myExpression = expression;
        mySyntheticFieldName = syntheticFieldName;
    }

    @Override
    public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
        return visitor.visitFieldReference(this, runner, stateBefore);
    }

    public String toString() {
        return "FIELD_REFERENCE: " + myExpression.getText();
    }

    
    public PsiExpression getExpression() {
        return myExpression;
    }

    
    public PsiExpression getElementToAssert() {
        if (mySyntheticFieldName != null) return myExpression;
        return myExpression instanceof PsiArrayAccessExpression
                ? ((PsiArrayAccessExpression)myExpression).getArrayExpression()
                : ((PsiReferenceExpression)myExpression).getQualifierExpression();
    }
}
