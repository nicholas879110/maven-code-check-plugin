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
package com.gome.maven.psi.infos;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.JavaSdkVersion;
import com.gome.maven.openapi.projectRoots.JavaVersionService;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.RecursionGuard;
import com.gome.maven.openapi.util.RecursionManager;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.resolve.DefaultParameterTypeInferencePolicy;
import com.gome.maven.psi.impl.source.resolve.ParameterTypeInferencePolicy;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.containers.ContainerUtil;
//import org.gome.maven.lang.annotations.MagicConstant;

import java.util.Map;

/**
 * @author ik, dsl
 */
public class MethodCandidateInfo extends CandidateInfo{
    public static final RecursionGuard ourOverloadGuard = RecursionManager.createGuard("overload.guard");
    public static final ThreadLocal<Map<PsiElement,  CurrentCandidateProperties>> CURRENT_CANDIDATE = new ThreadLocal<Map<PsiElement, CurrentCandidateProperties>>();
    @ApplicabilityLevelConstant private int myApplicabilityLevel; // benign race
    @ApplicabilityLevelConstant private int myPertinentApplicabilityLevel;
    private final PsiElement myArgumentList;
    private final PsiType[] myArgumentTypes;
    private final PsiType[] myTypeArguments;
    private PsiSubstitutor myCalcedSubstitutor; // benign race
    private final LanguageLevel myLanguageLevel;

    public MethodCandidateInfo( PsiElement candidate,
                               PsiSubstitutor substitutor,
                               boolean accessProblem,
                               boolean staticsProblem,
                               PsiElement argumentList,
                               PsiElement currFileContext,
                                PsiType[] argumentTypes,
                               PsiType[] typeArguments) {
        this(candidate, substitutor, accessProblem, staticsProblem, argumentList, currFileContext, argumentTypes, typeArguments,
                PsiUtil.getLanguageLevel(argumentList));
    }

    public MethodCandidateInfo( PsiElement candidate,
                                PsiSubstitutor substitutor,
                               boolean accessProblem,
                               boolean staticsProblem,
                               PsiElement argumentList,
                               PsiElement currFileContext,
                                PsiType[] argumentTypes,
                               PsiType[] typeArguments,
                                LanguageLevel languageLevel) {
        super(candidate, substitutor, accessProblem, staticsProblem, currFileContext);
        myArgumentList = argumentList;
        myArgumentTypes = argumentTypes;
        myTypeArguments = typeArguments;
        myLanguageLevel = languageLevel;
    }

    public boolean isVarargs() {
        return false;
    }

    public boolean isApplicable(){
        return getApplicabilityLevel() != ApplicabilityLevel.NOT_APPLICABLE;
    }

    @ApplicabilityLevelConstant
    private int getApplicabilityLevelInner() {
        final PsiType[] argumentTypes = getArgumentTypes();

        if (argumentTypes == null) return ApplicabilityLevel.NOT_APPLICABLE;

        int level = PsiUtil.getApplicabilityLevel(getElement(), getSubstitutor(), argumentTypes, myLanguageLevel);
        if (level > ApplicabilityLevel.NOT_APPLICABLE && !isTypeArgumentsApplicable()) level = ApplicabilityLevel.NOT_APPLICABLE;
        return level;
    }


    @ApplicabilityLevelConstant
    public int getApplicabilityLevel() {
        if(myApplicabilityLevel == 0){
            myApplicabilityLevel = getApplicabilityLevelInner();
        }
        return myApplicabilityLevel;
    }

    @ApplicabilityLevelConstant
    public int getPertinentApplicabilityLevel() {
        if (myPertinentApplicabilityLevel == 0) {
            myPertinentApplicabilityLevel = getPertinentApplicabilityLevelInner();
        }
        return myPertinentApplicabilityLevel;
    }

    public int getPertinentApplicabilityLevelInner() {
        if (myArgumentList == null || !PsiUtil.isLanguageLevel8OrHigher(myArgumentList)) {
            return getApplicabilityLevel();
        }
        final PsiSubstitutor substitutor = getSubstitutor(false);
        final PsiMethod method = getElement();
        @ApplicabilityLevelConstant int level = computeForOverloadedCandidate(new Computable<Integer>() {
            @Override
            public Integer compute() {
                PsiType[] argumentTypes = getArgumentTypes();
                if (argumentTypes == null) {
                    return ApplicabilityLevel.NOT_APPLICABLE;
                }

                int level = PsiUtil.getApplicabilityLevel(method, substitutor, argumentTypes, myLanguageLevel);
                if (!isVarargs() && level < ApplicabilityLevel.FIXED_ARITY) {
                    return ApplicabilityLevel.NOT_APPLICABLE;
                }
                return level;
            }
        }, substitutor);
        if (level > ApplicabilityLevel.NOT_APPLICABLE && !isTypeArgumentsApplicable(new Computable<PsiSubstitutor>() {
            @Override
            public PsiSubstitutor compute() {
                return substitutor;
            }
        })) {
            level = ApplicabilityLevel.NOT_APPLICABLE;
        }
        return level;
    }

    public PsiType[] getPertinentArgumentTypes() {
        return computeForOverloadedCandidate(new Computable<PsiType[]>() {
            public PsiType[] compute() {
                return getArgumentTypes();
            }
        }, getSubstitutor(false));
    }

    private <T> T computeForOverloadedCandidate(final Computable<T> computable, final PsiSubstitutor substitutor) {
        Map<PsiElement, CurrentCandidateProperties> map = CURRENT_CANDIDATE.get();
        if (map == null) {
            map = ContainerUtil.createConcurrentWeakMap();
            CURRENT_CANDIDATE.set(map);
        }
        final CurrentCandidateProperties alreadyThere = map.put(getMarkerList(),
                new CurrentCandidateProperties(getElement(), substitutor, isVarargs(), true));
        try {
            return computable.compute();
        }
        finally {
            if (alreadyThere == null) {
                map.remove(getMarkerList());
            } else {
                map.put(getMarkerList(), alreadyThere);
            }
        }
    }

    
    public PsiSubstitutor getSiteSubstitutor() {
        PsiSubstitutor incompleteSubstitutor = super.getSubstitutor();
        if (myTypeArguments != null) {
            PsiMethod method = getElement();
            PsiTypeParameter[] typeParams = method.getTypeParameters();
            for (int i = 0; i < myTypeArguments.length && i < typeParams.length; i++) {
                incompleteSubstitutor = incompleteSubstitutor.put(typeParams[i], myTypeArguments[i]);
            }
        }
        return incompleteSubstitutor;
    }

    
    @Override
    public PsiSubstitutor getSubstitutor() {
        return getSubstitutor(true);
    }

    
    public PsiSubstitutor getSubstitutor(boolean includeReturnConstraint) {
        PsiSubstitutor substitutor = myCalcedSubstitutor;
        if (substitutor == null || !includeReturnConstraint && myLanguageLevel.isAtLeast(LanguageLevel.JDK_1_8) || isOverloadCheck()) {
            PsiSubstitutor incompleteSubstitutor = super.getSubstitutor();
            PsiMethod method = getElement();
            if (myTypeArguments == null) {
                final RecursionGuard.StackStamp stackStamp = PsiDiamondType.ourDiamondGuard.markStack();

                final PsiSubstitutor inferredSubstitutor = inferTypeArguments(DefaultParameterTypeInferencePolicy.INSTANCE, includeReturnConstraint);

                if (!stackStamp.mayCacheNow() ||
                        isOverloadCheck() ||
                        !includeReturnConstraint && myLanguageLevel.isAtLeast(LanguageLevel.JDK_1_8) ||
                        getMarkerList() != null && PsiResolveHelper.ourGraphGuard.currentStack().contains(getMarkerList().getParent())) {
                    return inferredSubstitutor;
                }

                myCalcedSubstitutor = substitutor = inferredSubstitutor;
            }
            else {
                PsiTypeParameter[] typeParams = method.getTypeParameters();
                for (int i = 0; i < myTypeArguments.length && i < typeParams.length; i++) {
                    incompleteSubstitutor = incompleteSubstitutor.put(typeParams[i], myTypeArguments[i]);
                }
                myCalcedSubstitutor = substitutor = incompleteSubstitutor;
            }
        }

        return substitutor;
    }

    public static boolean isOverloadCheck() {
        return !ourOverloadGuard.currentStack().isEmpty();
    }


    public boolean isTypeArgumentsApplicable() {
        return isTypeArgumentsApplicable(new Computable<PsiSubstitutor>() {
            @Override
            public PsiSubstitutor compute() {
                return getSubstitutor(false);
            }
        });
    }

    private boolean isTypeArgumentsApplicable(Computable<PsiSubstitutor> computable) {
        final PsiMethod psiMethod = getElement();
        PsiTypeParameter[] typeParams = psiMethod.getTypeParameters();
        if (myTypeArguments != null && typeParams.length != myTypeArguments.length && !PsiUtil.isLanguageLevel7OrHigher(psiMethod)){
            return typeParams.length == 0 && JavaVersionService.getInstance().isAtLeast(psiMethod, JavaSdkVersion.JDK_1_7);
        }
        return GenericsUtil.isTypeArgumentsApplicable(typeParams, computable.compute(), getParent());
    }

    protected PsiElement getParent() {
        return myArgumentList != null ? myArgumentList.getParent() : null;
    }

    @Override
    public boolean isValidResult(){
        return super.isValidResult() && isApplicable();
    }

    
    @Override
    public PsiMethod getElement(){
        return (PsiMethod)super.getElement();
    }

    
    public PsiSubstitutor inferTypeArguments( ParameterTypeInferencePolicy policy, boolean includeReturnConstraint) {
        return inferTypeArguments(policy, myArgumentList instanceof PsiExpressionList
                ? ((PsiExpressionList)myArgumentList).getExpressions()
                : PsiExpression.EMPTY_ARRAY, includeReturnConstraint);
    }

    public PsiSubstitutor inferSubstitutorFromArgs( ParameterTypeInferencePolicy policy, final PsiExpression[] arguments) {
        if (myTypeArguments == null) {
            return inferTypeArguments(policy, arguments, true);
        }
        else {
            return getSiteSubstitutor();
        }
    }

    
    public PsiSubstitutor inferTypeArguments( ParameterTypeInferencePolicy policy,
                                              PsiExpression[] arguments,
                                             boolean includeReturnConstraint) {
        Map<PsiElement, CurrentCandidateProperties> map = CURRENT_CANDIDATE.get();
        if (map == null) {
            map = ContainerUtil.createConcurrentWeakMap();
            CURRENT_CANDIDATE.set(map);
        }
        final PsiMethod method = getElement();
        final CurrentCandidateProperties alreadyThere =
                map.put(getMarkerList(), new CurrentCandidateProperties(method, super.getSubstitutor(), policy.isVarargsIgnored() || isVarargs(), !includeReturnConstraint));
        try {
            PsiTypeParameter[] typeParameters = method.getTypeParameters();

            if (!method.hasModifierProperty(PsiModifier.STATIC)) {
                final PsiClass containingClass = method.getContainingClass();
                if (containingClass != null && PsiUtil.isRawSubstitutor(containingClass, mySubstitutor)) {
                    Project project = containingClass.getProject();
                    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                    return javaPsiFacade.getElementFactory().createRawSubstitutor(mySubstitutor, typeParameters);
                }
            }

            final PsiElement parent = getParent();
            if (parent == null) return PsiSubstitutor.EMPTY;
            Project project = method.getProject();
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            return javaPsiFacade.getResolveHelper()
                    .inferTypeArguments(typeParameters, method.getParameterList().getParameters(), arguments, mySubstitutor, parent, policy, myLanguageLevel);
        }
        finally {
            if (alreadyThere == null) {
                map.remove(getMarkerList());
            } else {
                map.put(getMarkerList(), alreadyThere);
            }
        }
    }

    protected PsiElement getMarkerList() {
        return myArgumentList;
    }

    public boolean isInferencePossible() {
        return myArgumentList != null && myArgumentList.isValid();
    }


    public static CurrentCandidateProperties getCurrentMethod(PsiElement context) {
        final Map<PsiElement, CurrentCandidateProperties> currentMethodCandidates = CURRENT_CANDIDATE.get();
        return currentMethodCandidates != null ? currentMethodCandidates.get(context) : null;
    }

    public static void updateSubstitutor(PsiElement context, PsiSubstitutor newSubstitutor) {
        CurrentCandidateProperties candidateProperties = getCurrentMethod(context);
        if (candidateProperties != null) {
            candidateProperties.setSubstitutor(newSubstitutor);
        }
    }

    public PsiType[] getArgumentTypes() {
        return myArgumentTypes;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && isVarargs() == ((MethodCandidateInfo)o).isVarargs();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (isVarargs() ? 1 : 0);
    }

    public static class CurrentCandidateProperties {
        private final PsiMethod myMethod;
        private PsiSubstitutor mySubstitutor;
        private boolean myVarargs;
        private boolean myApplicabilityCheck;

        public CurrentCandidateProperties(PsiMethod method, PsiSubstitutor substitutor, boolean varargs, boolean applicabilityCheck) {
            myMethod = method;
            mySubstitutor = substitutor;
            myVarargs = varargs;
            myApplicabilityCheck = applicabilityCheck;
        }

        public PsiMethod getMethod() {
            return myMethod;
        }

        public PsiSubstitutor getSubstitutor() {
            return mySubstitutor;
        }

        public void setSubstitutor(PsiSubstitutor substitutor) {
            mySubstitutor = substitutor;
        }

        public boolean isVarargs() {
            return myVarargs;
        }

        public void setVarargs(boolean varargs) {
            myVarargs = varargs;
        }

        public boolean isApplicabilityCheck() {
            return myApplicabilityCheck;
        }

        public void setApplicabilityCheck(boolean applicabilityCheck) {
            myApplicabilityCheck = applicabilityCheck;
        }
    }

    public static class ApplicabilityLevel {
        public static final int NOT_APPLICABLE = 1;
        public static final int VARARGS = 2;
        public static final int FIXED_ARITY = 3;
    }

//    @MagicConstant(intValues = {ApplicabilityLevel.NOT_APPLICABLE, ApplicabilityLevel.VARARGS, ApplicabilityLevel.FIXED_ARITY})
    public @interface ApplicabilityLevelConstant {
    }
}
