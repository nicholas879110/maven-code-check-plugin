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
package com.gome.maven.codeInsight.daemon.impl.quickfix;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.daemon.QuickFixBundle;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.infos.MethodCandidateInfo;
import com.gome.maven.util.IncorrectOperationException;

import java.util.HashSet;
import java.util.Set;

import static com.gome.maven.psi.CommonClassNames.JAVA_LANG_STRING;

public class ChangeStringLiteralToCharInMethodCallFix implements IntentionAction {
    private final PsiLiteralExpression myLiteral;
    private final PsiCall myCall;

    public ChangeStringLiteralToCharInMethodCallFix(final PsiLiteralExpression literal, final PsiCall methodCall) {
        myLiteral = literal;
        myCall = methodCall;
    }

    @Override
    
    public String getText() {
        final String convertedValue = convertedValue();
        final boolean isString = isString(myLiteral.getType());
        return QuickFixBundle.message("fix.single.character.string.to.char.literal.text", myLiteral.getText(),
                quote(convertedValue, ! isString), isString ? PsiType.CHAR.getCanonicalText() : "String");
    }

    @Override
    
    public String getFamilyName() {
        return QuickFixBundle.message("fix.single.character.string.to.char.literal.family");
    }

    @Override
    public boolean isAvailable( final Project project, final Editor editor, final PsiFile file) {
        return myCall.isValid() && myLiteral.isValid() && myCall.getManager().isInProject(myCall);
    }

    @Override
    public void invoke( final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

        final Object value = myLiteral.getValue();
        if (value != null && value.toString().length() == 1) {
            final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

            final PsiExpression newExpression = factory.createExpressionFromText(quote(convertedValue(), ! isString(myLiteral.getType())),
                    myLiteral.getParent());
            myLiteral.replace(newExpression);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private static String quote(final String value, final boolean doubleQuotes) {
        final char quote = doubleQuotes ? '"' : '\'';
        return quote + value + quote;
    }

    private String convertedValue() {
        String value = String.valueOf(myLiteral.getValue());
        final StringBuilder builder = new StringBuilder();
        StringUtil.escapeStringCharacters(value.length(), value, "\"'", builder);
        return builder.toString();
    }

    public static void registerFixes( final PsiMethod[] candidates,  final PsiConstructorCall call,
                                      final HighlightInfo out) {
        final Set<PsiLiteralExpression> literals = new HashSet<PsiLiteralExpression>();
        if (call.getArgumentList() == null) {
            return;
        }
        boolean exactMatch = false;
        for (PsiMethod method : candidates) {
            exactMatch |= findMatchingExpressions(call.getArgumentList().getExpressions(), method, literals);
        }
        if (! exactMatch) {
            processLiterals(literals, call, out);
        }
    }

    public static void registerFixes( final CandidateInfo[] candidates,
                                      final PsiMethodCallExpression methodCall,
                                      final HighlightInfo info) {
        if (info == null) return;
        final Set<PsiLiteralExpression> literals = new HashSet<PsiLiteralExpression>();
        boolean exactMatch = false;
        for (CandidateInfo candidate : candidates) {
            if (candidate instanceof MethodCandidateInfo) {
                final PsiMethod method = ((MethodCandidateInfo) candidate).getElement();
                exactMatch |= findMatchingExpressions(methodCall.getArgumentList().getExpressions(), method, literals);
            }
        }
        if (!exactMatch) {
            processLiterals(literals, methodCall, info);
        }
    }

    private static void processLiterals( final Set<PsiLiteralExpression> literals,
                                         final PsiCall call,
                                         final HighlightInfo info) {
        for (PsiLiteralExpression literal : literals) {
            final ChangeStringLiteralToCharInMethodCallFix fix = new ChangeStringLiteralToCharInMethodCallFix(literal, call);
            QuickFixAction.registerQuickFixAction(info, fix);
        }
    }

    /**
     * @return <code>true</code> if exact TYPEs match
     */
    private static boolean findMatchingExpressions(final PsiExpression[] arguments, final PsiMethod existingMethod,
                                                   final Set<PsiLiteralExpression> result) {
        final PsiParameterList parameterList = existingMethod.getParameterList();
        final PsiParameter[] parameters = parameterList.getParameters();

        if (arguments.length != parameters.length) {
            return false;
        }

        boolean typeMatch = true;
        for (int i = 0; i < parameters.length && i < arguments.length; i++) {
            final PsiParameter parameter = parameters[i];
            final PsiType parameterType = parameter.getType();
            final PsiType argumentType = arguments[i].getType();

            typeMatch &= Comparing.equal(parameterType, argumentType);

            if (arguments[i] instanceof PsiLiteralExpression && ! result.contains(arguments[i]) &&
                    (charToString(parameterType, argumentType) || charToString(argumentType, parameterType))) {

                final String value = String.valueOf(((PsiLiteralExpression) arguments[i]).getValue());
                if (value != null && value.length() == 1) {
                    result.add((PsiLiteralExpression) arguments[i]);
                }
            }
        }
        return typeMatch;
    }

    private static boolean charToString(final PsiType firstType, final PsiType secondType) {
        return Comparing.equal(PsiType.CHAR, firstType) && isString(secondType);
    }

    private static boolean isString(final PsiType type) {
        return type != null && type.equalsToText(JAVA_LANG_STRING);
    }
}