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
 * Time: 6:31:08 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.dataFlow.value;

import com.gome.maven.codeInsight.NullableNotNullManager;
import com.gome.maven.codeInsight.daemon.impl.analysis.JavaGenericsUtil;
import com.gome.maven.codeInspection.dataFlow.DfaPsiUtil;
import com.gome.maven.codeInspection.dataFlow.Nullness;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Trinity;
import com.gome.maven.patterns.ElementPattern;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.MultiMap;

import java.util.List;

import static com.gome.maven.patterns.PsiJavaPatterns.*;

public class DfaVariableValue extends DfaValue {

    private static final ElementPattern<? extends PsiModifierListOwner> MEMBER_OR_METHOD_PARAMETER =
            or(psiMember(), psiParameter().withSuperParent(2, psiMember()));

    public static class Factory {
        private final MultiMap<Trinity<Boolean,String,DfaVariableValue>,DfaVariableValue> myExistingVars = new MultiMap<Trinity<Boolean, String, DfaVariableValue>, DfaVariableValue>();
        private final DfaValueFactory myFactory;

        Factory(DfaValueFactory factory) {
            myFactory = factory;
        }

        public DfaVariableValue createVariableValue(PsiVariable myVariable, boolean isNegated) {
            PsiType varType = myVariable.getType();
            if (varType instanceof PsiEllipsisType) {
                varType = new PsiArrayType(((PsiEllipsisType)varType).getComponentType());
            }
            return createVariableValue(myVariable, varType, isNegated, null);
        }
        
        public DfaVariableValue createVariableValue( PsiModifierListOwner myVariable,
                                                     PsiType varType,
                                                    boolean isNegated,
                                                     DfaVariableValue qualifier) {
            Trinity<Boolean,String,DfaVariableValue> key = Trinity.create(isNegated, ((PsiNamedElement)myVariable).getName(), qualifier);
            for (DfaVariableValue aVar : myExistingVars.get(key)) {
                if (aVar.hardEquals(myVariable, varType, isNegated, qualifier)) return aVar;
            }

            DfaVariableValue result = new DfaVariableValue(myVariable, varType, isNegated, myFactory, qualifier);
            myExistingVars.putValue(key, result);
            while (qualifier != null) {
                qualifier.myDependents.add(result);
                qualifier = qualifier.getQualifier();
            }
            return result;
        }

        public List<DfaVariableValue> getAllQualifiedBy(DfaVariableValue value) {
            return value.myDependents;
        }

    }

    private final PsiModifierListOwner myVariable;
    private final PsiType myVarType;
     private final DfaVariableValue myQualifier;
    private DfaVariableValue myNegatedValue;
    private final boolean myIsNegated;
    private Nullness myInherentNullability;
    private final DfaTypeValue myTypeValue;
    private final List<DfaVariableValue> myDependents = new SmartList<DfaVariableValue>();

    private DfaVariableValue( PsiModifierListOwner variable,  PsiType varType, boolean isNegated, DfaValueFactory factory,  DfaVariableValue qualifier) {
        super(factory);
        myVariable = variable;
        myIsNegated = isNegated;
        myQualifier = qualifier;
        myVarType = varType;
        DfaValue typeValue = myFactory.createTypeValue(varType, Nullness.UNKNOWN);
        myTypeValue = typeValue instanceof DfaTypeValue ? (DfaTypeValue)typeValue : null;
        if (varType != null && !varType.isValid()) {
            PsiUtil.ensureValidType(varType, "Variable: " + variable + " of class " + variable.getClass());
        }
    }

    
    public DfaTypeValue getTypeValue() {
        return myTypeValue;
    }

    
    public PsiModifierListOwner getPsiVariable() {
        return myVariable;
    }

    
    public PsiType getVariableType() {
        return myVarType;
    }

    public boolean isNegated() {
        return myIsNegated;
    }

    
    public DfaVariableValue getNegatedValue() {
        return myNegatedValue;
    }

    @Override
    public DfaVariableValue createNegated() {
        if (myNegatedValue != null) {
            return myNegatedValue;
        }
        return myNegatedValue = myFactory.getVarFactory().createVariableValue(myVariable, myVarType, !myIsNegated, myQualifier);
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
        return (myIsNegated ? "!" : "") + ((PsiNamedElement)myVariable).getName() + (myQualifier == null ? "" : "|" + myQualifier.toString());
    }

    private boolean hardEquals(PsiModifierListOwner psiVar, PsiType varType, boolean negated, DfaVariableValue qualifier) {
        return psiVar == myVariable &&
                Comparing.equal(TypeConversionUtil.erasure(varType), TypeConversionUtil.erasure(myVarType)) &&
                negated == myIsNegated &&
                (myQualifier == null ? qualifier == null : myQualifier.hardEquals(qualifier.getPsiVariable(), qualifier.getVariableType(),
                        qualifier.isNegated(), qualifier.getQualifier()));
    }

    
    public DfaVariableValue getQualifier() {
        return myQualifier;
    }

    public Nullness getInherentNullability() {
        if (myInherentNullability != null) {
            return myInherentNullability;
        }

        return myInherentNullability = calcInherentNullability();
    }

    private Nullness calcInherentNullability() {
        PsiModifierListOwner var = getPsiVariable();
        Nullness nullability = DfaPsiUtil.getElementNullability(getVariableType(), var);
        if (nullability != Nullness.UNKNOWN) {
            return nullability;
        }

        Nullness defaultNullability = myFactory.isUnknownMembersAreNullable() && MEMBER_OR_METHOD_PARAMETER.accepts(var) ? Nullness.NULLABLE : Nullness.UNKNOWN;

        if (var instanceof PsiParameter && var.getParent() instanceof PsiForeachStatement) {
            PsiExpression iteratedValue = ((PsiForeachStatement)var.getParent()).getIteratedValue();
            if (iteratedValue != null) {
                PsiType itemType = JavaGenericsUtil.getCollectionItemType(iteratedValue);
                if (itemType != null) {
                    return DfaPsiUtil.getElementNullability(itemType, var);
                }
            }
        }

        if (var instanceof PsiField && DfaPsiUtil.isFinalField((PsiVariable)var) && myFactory.isHonorFieldInitializers()) {
            List<PsiExpression> initializers = DfaPsiUtil.findAllConstructorInitializers((PsiField)var);
            if (initializers.isEmpty()) {
                return defaultNullability;
            }

            boolean hasUnknowns = false;
            for (PsiExpression expression : initializers) {
                if (!(expression instanceof PsiReferenceExpression)) {
                    hasUnknowns = true;
                    continue;
                }
                PsiElement target = ((PsiReferenceExpression)expression).resolve();
                if (!(target instanceof PsiParameter)) {
                    hasUnknowns = true;
                    continue;
                }
                if (NullableNotNullManager.isNullable((PsiParameter)target)) {
                    return Nullness.NULLABLE;
                }
                if (!NullableNotNullManager.isNotNull((PsiParameter)target)) {
                    hasUnknowns = true;
                }
            }

            if (hasUnknowns) {
                if (DfaPsiUtil.isInitializedNotNull((PsiField)var)) {
                    return Nullness.NOT_NULL;
                }
                return defaultNullability;
            }

            return Nullness.NOT_NULL;
        }

        return defaultNullability;
    }

    public boolean isFlushableByCalls() {
        if (myVariable instanceof PsiLocalVariable || myVariable instanceof PsiParameter) return false;
        if (myVariable instanceof PsiVariable && myVariable.hasModifierProperty(PsiModifier.FINAL)) {
            return myQualifier != null && myQualifier.isFlushableByCalls();
        }
        return true;
    }

    public boolean containsCalls() {
        return myVariable instanceof PsiMethod || myQualifier != null && myQualifier.containsCalls();
    }

}
