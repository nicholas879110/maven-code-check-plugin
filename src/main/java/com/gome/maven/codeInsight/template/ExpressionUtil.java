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
package com.gome.maven.codeInsight.template;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.codeStyle.SuggestedNameInfo;
import com.gome.maven.psi.codeStyle.VariableKind;
import com.gome.maven.psi.text.BlockSupport;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author mike
 */
public class ExpressionUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.template.ExpressionUtil");

    private ExpressionUtil() {
    }


    public static String[] getNames(final ExpressionContext context) {
        final Project project = context.getProject();
        final int offset = context.getStartOffset();

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        String[] names = null;
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
        PsiElement element = file.findElementAt(offset);
        if (element instanceof PsiIdentifier){
            names = getNamesForIdentifier(project, (PsiIdentifier)element);
        }
        else{
            final PsiFile fileCopy = (PsiFile)file.copy();
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    BlockSupport blockSupport = BlockSupport.getInstance(project);
                    try{
                        blockSupport.reparseRange(fileCopy, offset, offset, "xxx");
                    }
                    catch(IncorrectOperationException e){
                        LOG.error(e);
                    }
                }
            });
            PsiElement identifierCopy = fileCopy.findElementAt(offset);
            if (identifierCopy instanceof PsiIdentifier) {
                names = getNamesForIdentifier(project, (PsiIdentifier)identifierCopy);
            }
        }
        return names;
    }


    private static String[] getNamesForIdentifier(Project project, PsiIdentifier identifier){
        if (identifier.getParent() instanceof PsiVariable){
            PsiVariable var = (PsiVariable)identifier.getParent();
            if (identifier.equals(var.getNameIdentifier())){
                JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                VariableKind variableKind = codeStyleManager.getVariableKind(var);
                PsiExpression initializer = var.getInitializer();
                if (var instanceof PsiParameter && ((PsiParameter)var).getDeclarationScope() instanceof PsiForeachStatement) {
                    //synthesize initializer
                    PsiForeachStatement foreachStatement = (PsiForeachStatement)((PsiParameter)var).getDeclarationScope();
                    final PsiExpression iteratedValue = foreachStatement.getIteratedValue();
                    if (iteratedValue != null) {
                        try {
                            final PsiArrayAccessExpression expr = (PsiArrayAccessExpression)JavaPsiFacade.getInstance(iteratedValue.getProject())
                                    .getElementFactory().createExpressionFromText("a[0]", var);
                            expr.getArrayExpression().replace(iteratedValue);
                            initializer = expr; //note: non physical with no parent
                        }
                        catch (IncorrectOperationException e) {
                            //do nothing
                        }
                    }
                }
                SuggestedNameInfo suggestedInfo = codeStyleManager.suggestVariableName(variableKind, null, initializer, var.getType());
                return suggestedInfo.names;
            }
        }
        return null;
    }
}
