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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.ExpectedTypeInfo;
import com.gome.maven.codeInsight.generation.GenerateMembersUtil;
import com.gome.maven.codeInsight.lookup.AutoCompletionPolicy;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementBuilder;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorModificationUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.impl.source.resolve.graphInference.FunctionalInterfaceParameterizationUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.ProcessingContext;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

/**
 * User: anna
 */
public class LambdaCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions( CompletionParameters parameters,
                                  ProcessingContext context,
                                   CompletionResultSet result) {
        result.addAllElements(getLambdaVariants(parameters));
    }

    static List<LookupElement> getLambdaVariants( CompletionParameters parameters) {
        if (!PsiUtil.isLanguageLevel8OrHigher(parameters.getOriginalFile())) return Collections.emptyList();

        List<LookupElement> result = ContainerUtil.newArrayList();
        for (ExpectedTypeInfo expectedType : JavaSmartCompletionContributor.getExpectedTypes(parameters)) {
            final PsiType defaultType = expectedType.getDefaultType();
            if (LambdaUtil.isFunctionalType(defaultType)) {
                final PsiType functionalInterfaceType = FunctionalInterfaceParameterizationUtil.getGroundTargetType(defaultType);
                final PsiMethod method = LambdaUtil.getFunctionalInterfaceMethod(functionalInterfaceType);
                if (method != null) {
                    PsiParameter[] params = method.getParameterList().getParameters();
                    final Project project = method.getProject();
                    final PsiElement originalPosition = parameters.getOriginalPosition();
                    final JVMElementFactory jvmElementFactory = originalPosition != null ? JVMElementFactories.getFactory(originalPosition.getLanguage(), project) : null;
                    final JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
                    if (jvmElementFactory != null) {
                        final PsiSubstitutor substitutor = LambdaUtil.getSubstitutor(method, PsiUtil.resolveGenericsClassInType(functionalInterfaceType));
                        params = GenerateMembersUtil.overriddenParameters(params, jvmElementFactory, javaCodeStyleManager, substitutor, originalPosition);
                    }

                    String paramsString =
                            params.length == 1 ? getParamName(params[0], javaCodeStyleManager, originalPosition) : "(" + StringUtil.join(params, new Function<PsiParameter, String>() {
                                @Override
                                public String fun(PsiParameter parameter) {
                                    return getParamName(parameter, javaCodeStyleManager, originalPosition);
                                }
                            }, ",") + ")";

                    final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
                    PsiLambdaExpression lambdaExpression = (PsiLambdaExpression)JavaPsiFacade.getElementFactory(project)
                            .createExpressionFromText(paramsString + " -> {}", null);
                    lambdaExpression = (PsiLambdaExpression)codeStyleManager.reformat(lambdaExpression);
                    paramsString = lambdaExpression.getParameterList().getText();
                    final LookupElementBuilder builder =
                            LookupElementBuilder.create(paramsString).withPresentableText(paramsString + " -> {}").withInsertHandler(new InsertHandler<LookupElement>() {
                                @Override
                                public void handleInsert(InsertionContext context, LookupElement item) {
                                    final Editor editor = context.getEditor();
                                    EditorModificationUtil.insertStringAtCaret(editor, " -> ");
                                }
                            });
                    result.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE));
                }
            }
        }
        return result;
    }

    private static String getParamName(PsiParameter param, JavaCodeStyleManager javaCodeStyleManager, PsiElement originalPosition) {
        return javaCodeStyleManager.suggestUniqueVariableName(param.getName(), originalPosition, true);
    }
}
