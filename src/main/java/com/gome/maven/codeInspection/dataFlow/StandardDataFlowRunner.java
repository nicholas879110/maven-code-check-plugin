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

import com.gome.maven.codeInsight.NullableNotNullManager;
import com.gome.maven.codeInspection.dataFlow.instructions.InstanceofInstruction;
import com.gome.maven.codeInspection.dataFlow.instructions.Instruction;
import com.gome.maven.codeInspection.nullable.NullableStuffInspectionBase;
import com.gome.maven.psi.*;

import java.util.HashSet;
import java.util.Set;

public class StandardDataFlowRunner extends DataFlowRunner {
    private final Set<Instruction> myCCEInstructions = new HashSet<Instruction>();

    private boolean myInNullableMethod = false;
    private boolean myInNotNullMethod = false;
    private boolean myIsInMethod = false;

    public StandardDataFlowRunner() {
        this(false, true);
    }
    public StandardDataFlowRunner(boolean unknownMembersAreNullable, boolean honorFieldInitializers) {
        super(unknownMembersAreNullable, honorFieldInitializers);
    }

    @Override
    protected void prepareAnalysis( PsiElement psiBlock, Iterable<DfaMemoryState> initialStates) {
        PsiElement parent = psiBlock.getParent();
        myIsInMethod = parent instanceof PsiMethod;
        if (myIsInMethod) {
            PsiMethod method = (PsiMethod)parent;
            myInNullableMethod = isTreatedAsNullable(method);
            myInNotNullMethod = NullableNotNullManager.isNotNull(method);
        } else if (parent instanceof PsiLambdaExpression) {
            PsiMethod method = LambdaUtil.getFunctionalInterfaceMethod(((PsiLambdaExpression)parent).getFunctionalInterfaceType());
            if (method != null) {
                myIsInMethod = true;
                myInNullableMethod = isTreatedAsNullable(method);
                myInNotNullMethod = NullableNotNullManager.isNotNull(method);
            }
        }

        myCCEInstructions.clear();
    }

    private static boolean isTreatedAsNullable(PsiMethod method) {
        if (NullableStuffInspectionBase.isNullableNotInferred(method, true)) {
            return true;
        }

        PsiType returnType = method.getReturnType();
        return returnType != null && returnType.equalsToText(CommonClassNames.JAVA_LANG_VOID);
    }

    public void onInstructionProducesCCE(Instruction instruction) {
        myCCEInstructions.add(instruction);
    }

     public Set<Instruction> getCCEInstructions() {
        return myCCEInstructions;
    }

    public boolean isInNotNullMethod() {
        return myInNotNullMethod;
    }

    public boolean isInNullableMethod() {
        return myInNullableMethod;
    }

    public boolean isInMethod() {
        return myIsInMethod;
    }

     public static Set<Instruction> getRedundantInstanceofs(final DataFlowRunner runner, StandardInstructionVisitor visitor) {
        HashSet<Instruction> result = new HashSet<Instruction>(1);
        for (Instruction instruction : runner.getInstructions()) {
            if (instruction instanceof InstanceofInstruction && visitor.isInstanceofRedundant((InstanceofInstruction)instruction)) {
                result.add(instruction);
            }
        }

        return result;
    }
}
