/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.daemon.impl.quickfix;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.daemon.QuickFixBundle;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.daemon.impl.analysis.JavaHighlightUtil;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Arrays;

/**
 * @author Danila Ponomarenko
 */
public class RemoveRedundantArgumentsFix implements IntentionAction {
    private final PsiMethod myTargetMethod;
    private final PsiExpression[] myArguments;
    private final PsiSubstitutor mySubstitutor;

    private RemoveRedundantArgumentsFix( PsiMethod targetMethod,
                                         PsiExpression[] arguments,
                                         PsiSubstitutor substitutor) {
        myTargetMethod = targetMethod;
        myArguments = arguments;
        mySubstitutor = substitutor;
    }

    
    @Override
    public String getText() {
        return QuickFixBundle.message("remove.redundant.arguments.text", JavaHighlightUtil.formatMethod(myTargetMethod));
    }

    
    @Override
    public String getFamilyName() {
        return QuickFixBundle.message("remove.redundant.arguments.family");
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        if (!myTargetMethod.isValid() || myTargetMethod.getContainingClass() == null) return false;
        for (PsiExpression expression : myArguments) {
            if (!expression.isValid()) return false;
        }
        if (!mySubstitutor.isValid()) return false;

        return findRedundantArgument(myArguments, myTargetMethod.getParameterList().getParameters(), mySubstitutor) != null;
    }

    
    private static PsiExpression[] findRedundantArgument( PsiExpression[] arguments,
                                                          PsiParameter[] parameters,
                                                          PsiSubstitutor substitutor) {
        if (arguments.length <= parameters.length) return null;

        for (int i = 0; i < parameters.length; i++) {
            final PsiExpression argument = arguments[i];
            final PsiParameter parameter = parameters[i];

            final PsiType argumentType = argument.getType();
            if (argumentType == null) return null;
            final PsiType parameterType = substitutor.substitute(parameter.getType());

            if (!TypeConversionUtil.isAssignable(parameterType, argumentType)) {
                return null;
            }
        }

        return Arrays.copyOfRange(arguments, parameters.length, arguments.length);
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
        final PsiExpression[] redundantArguments = findRedundantArgument(myArguments, myTargetMethod.getParameterList().getParameters(), mySubstitutor);
        if (redundantArguments != null) {
            for (PsiExpression argument : redundantArguments) {
                argument.delete();
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static void registerIntentions( JavaResolveResult[] candidates,
                                           PsiExpressionList arguments,
                                           HighlightInfo highlightInfo,
                                          TextRange fixRange) {
        for (JavaResolveResult candidate : candidates) {
            registerIntention(arguments, highlightInfo, fixRange, candidate, arguments);
        }
    }

    private static void registerIntention( PsiExpressionList arguments,
                                           HighlightInfo highlightInfo,
                                          TextRange fixRange,
                                           JavaResolveResult candidate,
                                           PsiElement context) {
        if (!candidate.isStaticsScopeCorrect()) return;
        PsiMethod method = (PsiMethod)candidate.getElement();
        PsiSubstitutor substitutor = candidate.getSubstitutor();
        if (method != null && context.getManager().isInProject(method)) {
            QuickFixAction.registerQuickFixAction(highlightInfo, fixRange, new RemoveRedundantArgumentsFix(method, arguments.getExpressions(), substitutor));
        }
    }
}
