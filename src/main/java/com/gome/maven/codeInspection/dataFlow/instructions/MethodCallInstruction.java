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
 * Time: 10:48:52 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.dataFlow.instructions;

import com.gome.maven.codeInspection.dataFlow.*;
import com.gome.maven.codeInspection.dataFlow.value.DfaValue;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PropertyUtil;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class MethodCallInstruction extends Instruction {
     private final PsiCallExpression myCall;
     private final PsiType myType;
     private final PsiExpression[] myArgs;
    private final boolean myShouldFlushFields;
     private final PsiExpression myContext;
     private final PsiMethod myTargetMethod;
    private final List<MethodContract> myContracts;
    private final MethodType myMethodType;
     private final DfaValue myPrecalculatedReturnValue;
    private final boolean myVarArgCall;
    private final Map<PsiExpression, Nullness> myArgRequiredNullability;

    public enum MethodType {
        BOXING, UNBOXING, REGULAR_METHOD_CALL, CAST
    }

    public MethodCallInstruction( PsiExpression context, MethodType methodType,  PsiType resultType) {
        myContext = context;
        myContracts = Collections.emptyList();
        myMethodType = methodType;
        myCall = null;
        myArgs = PsiExpression.EMPTY_ARRAY;
        myType = resultType;
        myShouldFlushFields = false;
        myPrecalculatedReturnValue = null;
        myTargetMethod = null;
        myVarArgCall = false;
        myArgRequiredNullability = Collections.emptyMap();
    }

    public MethodCallInstruction( PsiCallExpression call,  DfaValue precalculatedReturnValue, List<MethodContract> contracts) {
        myContext = call;
        myContracts = contracts;
        myMethodType = MethodType.REGULAR_METHOD_CALL;
        myCall = call;
        final PsiExpressionList argList = call.getArgumentList();
        myArgs = argList != null ? argList.getExpressions() : PsiExpression.EMPTY_ARRAY;
        myType = myCall.getType();

        JavaResolveResult result = call.resolveMethodGenerics();
        myTargetMethod = (PsiMethod)result.getElement();

        PsiSubstitutor substitutor = result.getSubstitutor();
        if (argList != null && myTargetMethod != null) {
            PsiParameter[] parameters = myTargetMethod.getParameterList().getParameters();
            myVarArgCall = isVarArgCall(myTargetMethod, substitutor, myArgs, parameters);
            myArgRequiredNullability = calcArgRequiredNullability(substitutor, parameters);
        } else {
            myVarArgCall = false;
            myArgRequiredNullability = Collections.emptyMap();
        }

        myShouldFlushFields = !(call instanceof PsiNewExpression && myType != null && myType.getArrayDimensions() > 0) && !isPureCall();
        myPrecalculatedReturnValue = precalculatedReturnValue;
    }

    private Map<PsiExpression, Nullness> calcArgRequiredNullability(PsiSubstitutor substitutor, PsiParameter[] parameters) {
        int checkedCount = Math.min(myArgs.length, parameters.length) - (myVarArgCall ? 1 : 0);

        Map<PsiExpression, Nullness> map = ContainerUtil.newHashMap();
        for (int i = 0; i < checkedCount; i++) {
            map.put(myArgs[i], DfaPsiUtil.getElementNullability(substitutor.substitute(parameters[i].getType()), parameters[i]));
        }
        return map;
    }

    private static boolean isVarArgCall(PsiMethod method, PsiSubstitutor substitutor, PsiExpression[] args, PsiParameter[] parameters) {
        if (!method.isVarArgs()) {
            return false;
        }

        int argCount = args.length;
        int paramCount = parameters.length;
        if (argCount > paramCount) {
            return true;
        }

        if (paramCount > 0 && argCount == paramCount) {
            PsiType lastArgType = args[argCount - 1].getType();
            if (lastArgType != null && !substitutor.substitute(parameters[paramCount - 1].getType()).isAssignableFrom(lastArgType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPureCall() {
        if (myTargetMethod == null) return false;
        return ControlFlowAnalyzer.isPure(myTargetMethod) || PropertyUtil.isSimplePropertyGetter(myTargetMethod);
    }

    
    public PsiType getResultType() {
        return myType;
    }

    
    public PsiExpression[] getArgs() {
        return myArgs;
    }

    public MethodType getMethodType() {
        return myMethodType;
    }

    public boolean shouldFlushFields() {
        return myShouldFlushFields;
    }

    
    public PsiMethod getTargetMethod() {
        return myTargetMethod;
    }

    public boolean isVarArgCall() {
        return myVarArgCall;
    }

    
    public Nullness getArgRequiredNullability( PsiExpression arg) {
        return myArgRequiredNullability.get(arg);
    }

    public List<MethodContract> getContracts() {
        return myContracts;
    }

    @Override
    public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
        return visitor.visitMethodCall(this, runner, stateBefore);
    }

    
    public PsiCallExpression getCallExpression() {
        return myCall;
    }

    
    public PsiExpression getContext() {
        return myContext;
    }

    
    public DfaValue getPrecalculatedReturnValue() {
        return myPrecalculatedReturnValue;
    }

    public String toString() {
        return myMethodType == MethodType.UNBOXING
                ? "UNBOX"
                : myMethodType == MethodType.BOXING
                ? "BOX" :
                "CALL_METHOD: " + (myCall == null ? "null" : myCall.getText());
    }
}
