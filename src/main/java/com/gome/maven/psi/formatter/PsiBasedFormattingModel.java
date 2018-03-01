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

package com.gome.maven.psi.formatter;

import com.gome.maven.formatting.Block;
import com.gome.maven.formatting.FormattingDocumentModel;
import com.gome.maven.formatting.FormattingModelEx;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.TokenType;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;

public class PsiBasedFormattingModel implements FormattingModelEx {

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.formatter.PsiBasedFormattingModel");

    private final Project myProject;
    private final ASTNode myASTNode;
    private final FormattingDocumentModelImpl myDocumentModel;
     private final Block myRootBlock;
    protected boolean myCanModifyAllWhiteSpaces = false;

    public PsiBasedFormattingModel(final PsiFile file,
                                    final Block rootBlock,
                                   final FormattingDocumentModelImpl documentModel) {
        myASTNode = SourceTreeToPsiMap.psiElementToTree(file);
        myDocumentModel = documentModel;
        myRootBlock = rootBlock;
        myProject = file.getProject();
    }



    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
        return replaceWhiteSpace(textRange, null, whiteSpace);
    }

    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, ASTNode nodeAfter, String whiteSpace) {
        String whiteSpaceToUse
                = myDocumentModel.adjustWhiteSpaceIfNecessary(whiteSpace, textRange.getStartOffset(), textRange.getEndOffset(), nodeAfter, true).toString();
        final String wsReplaced = replaceWithPSI(textRange, whiteSpaceToUse);

        if (wsReplaced != null){
            return new TextRange(textRange.getStartOffset(), textRange.getStartOffset() + wsReplaced.length());
        } else {
            return textRange;
        }
    }

    @Override
    public TextRange shiftIndentInsideRange(ASTNode node, TextRange textRange, int shift) {
        return textRange; // TODO: Remove this method from here...
    }

    @Override
    public void commitChanges() {
    }


    
    private String replaceWithPSI(final TextRange textRange, final String whiteSpace) {
        final int offset = textRange.getEndOffset();
        ASTNode leafElement = findElementAt(offset);

        if (leafElement != null) {
            if (leafElement.getPsi() instanceof PsiFile) {
                return null;
            } else {
                if (!leafElement.getPsi().isValid()) {
                    String message = "Invalid element found in '\n" +
                            myASTNode.getText() +
                            "\n' at " +
                            offset +
                            "(" +
                            myASTNode.getText().substring(offset, Math.min(offset + 10, myASTNode.getTextLength()));
                    LOG.error(message);
                }
                return replaceWithPsiInLeaf(textRange, whiteSpace, leafElement);
            }
        } else if (textRange.getEndOffset() == myASTNode.getTextLength()){

            CodeStyleManager.getInstance(myProject).performActionWithFormatterDisabled(new Runnable() {
                @Override
                public void run() {
                    FormatterUtil.replaceLastWhiteSpace(myASTNode, whiteSpace, textRange);
                }
            });

            return whiteSpace;
        } else {
            return null;
        }
    }

    
    protected String replaceWithPsiInLeaf(final TextRange textRange, final String whiteSpace, final ASTNode leafElement) {
        if (!myCanModifyAllWhiteSpaces) {
            if (leafElement.getElementType() == TokenType.WHITE_SPACE) return null;
        }

        CodeStyleManager.getInstance(myProject).performActionWithFormatterDisabled(new Runnable() {
            @Override
            public void run() {
                FormatterUtil.replaceWhiteSpace(whiteSpace, leafElement, TokenType.WHITE_SPACE, textRange);
            }
        });

        return whiteSpace;
    }

    
    protected ASTNode findElementAt(final int offset) {
        PsiFile containingFile = myASTNode.getPsi().getContainingFile();
        Project project = containingFile.getProject();
        assert !PsiDocumentManager.getInstance(project).isUncommited(myDocumentModel.getDocument());
        // TODO:default project can not be used for injections, because latter might wants (unavailable) indices
        PsiElement psiElement = project.isDefault() ? null : InjectedLanguageUtil.findInjectedElementNoCommit(containingFile, offset);
        if (psiElement == null) psiElement = containingFile.findElementAt(offset);
        if (psiElement == null) return null;
        return psiElement.getNode();
    }

    @Override
    
    public FormattingDocumentModel getDocumentModel() {
        return myDocumentModel;
    }

    @Override
    
    public Block getRootBlock() {
        return myRootBlock;
    }

    public void canModifyAllWhiteSpaces() {
        myCanModifyAllWhiteSpaces = true;
    }
}
