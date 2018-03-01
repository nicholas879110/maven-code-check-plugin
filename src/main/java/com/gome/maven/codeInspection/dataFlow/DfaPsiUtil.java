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
package com.gome.maven.codeInspection.dataFlow;

import com.gome.maven.codeInsight.NullableNotNullManager;
import com.gome.maven.codeInspection.dataFlow.instructions.Instruction;
import com.gome.maven.codeInspection.dataFlow.instructions.MethodCallInstruction;
import com.gome.maven.codeInspection.dataFlow.instructions.ReturnInstruction;
import com.gome.maven.codeInspection.dataFlow.value.DfaValueFactory;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.searches.ReferencesSearch;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.*;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.containers.Stack;

import java.util.*;

public class DfaPsiUtil {

    public static boolean isFinalField(PsiVariable var) {
        return var.hasModifierProperty(PsiModifier.FINAL) && !var.hasModifierProperty(PsiModifier.TRANSIENT) && var instanceof PsiField;
    }

    static PsiElement getEnclosingCodeBlock(final PsiVariable variable, final PsiElement context) {
        PsiElement codeBlock;
        if (variable instanceof PsiParameter) {
            codeBlock = ((PsiParameter)variable).getDeclarationScope();
            if (codeBlock instanceof PsiMethod) {
                codeBlock = ((PsiMethod)codeBlock).getBody();
            }
        }
        else if (variable instanceof PsiLocalVariable) {
            codeBlock = PsiTreeUtil.getParentOfType(variable, PsiCodeBlock.class);
        }
        else {
            codeBlock = getTopmostBlockInSameClass(context);
        }
        while (codeBlock != null) {
            PsiAnonymousClass anon = PsiTreeUtil.getParentOfType(codeBlock, PsiAnonymousClass.class);
            if (anon == null) break;
            codeBlock = PsiTreeUtil.getParentOfType(anon, PsiCodeBlock.class);
        }
        return codeBlock;
    }

    
    public static Nullness getElementNullability( PsiType resultType,  PsiModifierListOwner owner) {
        if (owner == null) {
            return Nullness.UNKNOWN;
        }

        if (NullableNotNullManager.isNullable(owner)) {
            return Nullness.NULLABLE;
        }
        if (NullableNotNullManager.isNotNull(owner)) {
            return Nullness.NOT_NULL;
        }

        if (resultType != null) {
            NullableNotNullManager nnn = NullableNotNullManager.getInstance(owner.getProject());
            for (PsiAnnotation annotation : resultType.getAnnotations()) {
                if (!annotation.isValid()) {
                    PsiUtilCore.ensureValid(owner);
                    PsiUtil.ensureValidType(resultType, owner + " of " + owner.getClass());
                    PsiUtilCore.ensureValid(annotation); //should fail
                }
                String qualifiedName = annotation.getQualifiedName();
                if (nnn.getNullables().contains(qualifiedName)) {
                    return Nullness.NULLABLE;
                }
                if (nnn.getNotNulls().contains(qualifiedName)) {
                    return Nullness.NOT_NULL;
                }
            }
        }

        return Nullness.UNKNOWN;
    }

    public static boolean isInitializedNotNull(PsiField field) {
        PsiClass containingClass = field.getContainingClass();
        if (containingClass == null) return false;

        PsiMethod[] constructors = containingClass.getConstructors();
        if (constructors.length == 0) return false;

        for (PsiMethod method : constructors) {
            if (!getNotNullInitializedFields(method, containingClass).contains(field)) {
                return false;
            }
        }
        return true;
    }

    private static Set<PsiField> getNotNullInitializedFields(final PsiMethod constructor, final PsiClass containingClass) {
        final PsiCodeBlock body = constructor.getBody();
        if (body == null) return Collections.emptySet();
        return CachedValuesManager.getCachedValue(constructor, new CachedValueProvider<Set<PsiField>>() {
            
            @Override
            public Result<Set<PsiField>> compute() {
                final PsiCodeBlock body = constructor.getBody();
                final Map<PsiField, Boolean> map = ContainerUtil.newHashMap();
                final StandardDataFlowRunner dfaRunner = new StandardDataFlowRunner(false, false) {
                    boolean shouldCheck;

                    @Override
                    protected void prepareAnalysis( PsiElement psiBlock, Iterable<DfaMemoryState> initialStates) {
                        super.prepareAnalysis(psiBlock, initialStates);
                        shouldCheck = psiBlock == body;
                    }

                    private boolean isCallExposingNonInitializedFields(Instruction instruction) {
                        if (!(instruction instanceof MethodCallInstruction) ||
                                ((MethodCallInstruction)instruction).getMethodType() != MethodCallInstruction.MethodType.REGULAR_METHOD_CALL) {
                            return false;
                        }

                        PsiCallExpression call = ((MethodCallInstruction)instruction).getCallExpression();
                        if (call == null) return false;

                        if (call instanceof PsiMethodCallExpression &&
                                DfaValueFactory.isEffectivelyUnqualified(((PsiMethodCallExpression)call).getMethodExpression())) {
                            return true;
                        }

                        PsiExpressionList argumentList = call.getArgumentList();
                        if (argumentList != null) {
                            for (PsiExpression expression : argumentList.getExpressions()) {
                                if (expression instanceof PsiThisExpression) return true;
                            }
                        }

                        return false;
                    }

                    @Override
                    protected DfaInstructionState[] acceptInstruction(InstructionVisitor visitor, DfaInstructionState instructionState) {
                        if (shouldCheck) {
                            Instruction instruction = instructionState.getInstruction();
                            if (isCallExposingNonInitializedFields(instruction) ||
                                    instruction instanceof ReturnInstruction && !((ReturnInstruction)instruction).isViaException()) {
                                for (PsiField field : containingClass.getFields()) {
                                    if (!instructionState.getMemoryState().isNotNull(getFactory().getVarFactory().createVariableValue(field, false))) {
                                        map.put(field, false);
                                    } else if (!map.containsKey(field)) {
                                        map.put(field, true);
                                    }
                                }
                                return DfaInstructionState.EMPTY_ARRAY;
                            }
                        }
                        return super.acceptInstruction(visitor, instructionState);
                    }
                };
                final RunnerResult rc = dfaRunner.analyzeMethod(body, new StandardInstructionVisitor());
                Set<PsiField> notNullFields = ContainerUtil.newHashSet();
                if (rc == RunnerResult.OK) {
                    for (PsiField field : map.keySet()) {
                        if (map.get(field)) {
                            notNullFields.add(field);
                        }
                    }
                }
                return Result.create(notNullFields, constructor, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
            }
        });
    }

    public static List<PsiExpression> findAllConstructorInitializers(PsiField field) {
        final List<PsiExpression> result = ContainerUtil.createLockFreeCopyOnWriteList();
        ContainerUtil.addIfNotNull(result, field.getInitializer());

        final PsiClass containingClass = field.getContainingClass();
        if (containingClass != null && !(containingClass instanceof PsiCompiledElement)) {
            result.addAll(getAllConstructorFieldInitializers(containingClass).get(field));
        }
        return result;
    }

    private static MultiMap<PsiField, PsiExpression> getAllConstructorFieldInitializers(final PsiClass psiClass) {
        if (psiClass instanceof PsiCompiledElement) {
            return MultiMap.EMPTY;
        }

        return CachedValuesManager.getCachedValue(psiClass, new CachedValueProvider<MultiMap<PsiField, PsiExpression>>() {
            
            @Override
            public Result<MultiMap<PsiField, PsiExpression>> compute() {
                final Set<String> fieldNames = ContainerUtil.newHashSet();
                for (PsiField field : psiClass.getFields()) {
                    ContainerUtil.addIfNotNull(fieldNames, field.getName());
                }

                final MultiMap<PsiField, PsiExpression> result = new MultiMap<PsiField, PsiExpression>();
                JavaRecursiveElementWalkingVisitor visitor = new JavaRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitAssignmentExpression(PsiAssignmentExpression assignment) {
                        super.visitAssignmentExpression(assignment);
                        PsiExpression lExpression = assignment.getLExpression();
                        PsiExpression rExpression = assignment.getRExpression();
                        if (rExpression != null &&
                                lExpression instanceof PsiReferenceExpression &&
                                fieldNames.contains(((PsiReferenceExpression)lExpression).getReferenceName())) {
                            PsiElement target = ((PsiReferenceExpression)lExpression).resolve();
                            if (target instanceof PsiField && ((PsiField)target).getContainingClass() == psiClass) {
                                result.putValue((PsiField)target, rExpression);
                            }
                        }
                    }
                };

                for (PsiMethod constructor : psiClass.getConstructors()) {
                    constructor.accept(visitor);
                }

                return Result.create(result, psiClass);
            }
        });
    }

    
    public static PsiCodeBlock getTopmostBlockInSameClass( PsiElement position) {
        PsiCodeBlock block = PsiTreeUtil.getParentOfType(position, PsiCodeBlock.class, false, PsiMember.class, PsiFile.class);
        if (block == null) {
            return null;
        }

        PsiCodeBlock lastBlock = block;
        while (true) {
            block = PsiTreeUtil.getParentOfType(block, PsiCodeBlock.class, true, PsiMember.class, PsiFile.class);
            if (block == null) {
                return lastBlock;
            }
            lastBlock = block;
        }
    }

    
    public static Collection<PsiExpression> getVariableAssignmentsInFile( PsiVariable psiVariable,
                                                                         final boolean literalsOnly,
                                                                         final PsiElement place) {
        final Ref<Boolean> modificationRef = Ref.create(Boolean.FALSE);
        final PsiCodeBlock codeBlock = place == null? null : getTopmostBlockInSameClass(place);
        final int placeOffset = codeBlock != null? place.getTextRange().getStartOffset() : 0;
        List<PsiExpression> list = ContainerUtil.mapNotNull(
                ReferencesSearch.search(psiVariable, new LocalSearchScope(new PsiElement[] {psiVariable.getContainingFile()}, null, true)).findAll(),
                new NullableFunction<PsiReference, PsiExpression>() {
                    @Override
                    public PsiExpression fun(final PsiReference psiReference) {
                        if (modificationRef.get()) return null;
                        final PsiElement parent = psiReference.getElement().getParent();
                        if (parent instanceof PsiAssignmentExpression) {
                            final PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression)parent;
                            final IElementType operation = assignmentExpression.getOperationTokenType();
                            if (assignmentExpression.getLExpression() == psiReference) {
                                if (JavaTokenType.EQ.equals(operation)) {
                                    final PsiExpression rValue = assignmentExpression.getRExpression();
                                    if (!literalsOnly || allOperandsAreLiterals(rValue)) {
                                        // if there's a codeBlock omit the values assigned later
                                        if (codeBlock != null && PsiTreeUtil.isAncestor(codeBlock, parent, true)
                                                && placeOffset < parent.getTextRange().getStartOffset()) {
                                            return null;
                                        }
                                        return rValue;
                                    }
                                    else {
                                        modificationRef.set(Boolean.TRUE);
                                    }
                                }
                                else if (JavaTokenType.PLUSEQ.equals(operation)) {
                                    modificationRef.set(Boolean.TRUE);
                                }
                            }
                        }
                        return null;
                    }
                });
        if (modificationRef.get()) return Collections.emptyList();
        PsiExpression initializer = psiVariable.getInitializer();
        if (initializer != null && (!literalsOnly || allOperandsAreLiterals(initializer))) {
            list = ContainerUtil.concat(list, Collections.singletonList(initializer));
        }
        return list;
    }

    public static boolean allOperandsAreLiterals( final PsiExpression expression) {
        if (expression == null) return false;
        if (expression instanceof PsiLiteralExpression) return true;
        if (expression instanceof PsiPolyadicExpression) {
            Stack<PsiExpression> stack = new Stack<PsiExpression>();
            stack.add(expression);
            while (!stack.isEmpty()) {
                PsiExpression psiExpression = stack.pop();
                if (psiExpression instanceof PsiPolyadicExpression) {
                    PsiPolyadicExpression binaryExpression = (PsiPolyadicExpression)psiExpression;
                    for (PsiExpression op : binaryExpression.getOperands()) {
                        stack.push(op);
                    }
                }
                else if (!(psiExpression instanceof PsiLiteralExpression)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
