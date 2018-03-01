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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.tree.IElementType;

public class ClsBinaryExpressionImpl extends ClsElementImpl implements PsiBinaryExpression {
    private final ClsElementImpl myParent;
    private final PsiExpression myLOperand;
    private final PsiJavaToken myOperation;
    private final PsiExpression myROperand;

    public ClsBinaryExpressionImpl(ClsElementImpl parent,
                                   ClsLiteralExpressionImpl lOperand,
                                   ClsJavaTokenImpl operation,
                                   ClsLiteralExpressionImpl rOperand) {
        myParent = parent;
        myLOperand = lOperand;
        myOperation = operation;
        myROperand = rOperand;
        lOperand.setParent(this);
        operation.setParent(this);
        rOperand.setParent(this);
    }

    public ClsBinaryExpressionImpl(ClsElementImpl parent,
                                   ClsPrefixExpressionImpl lOperand,
                                   ClsJavaTokenImpl operation,
                                   ClsLiteralExpressionImpl rOperand) {
        myParent = parent;
        myLOperand = lOperand;
        myOperation = operation;
        myROperand = rOperand;
        lOperand.setParent(this);
        operation.setParent(this);
        rOperand.setParent(this);
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        buffer.append(getText());
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, JavaElementType.BINARY_EXPRESSION);
    }

    @Override
    public String getText() {
        return StringUtil.join(myLOperand.getText(), " ", myOperation.getText(), " ", myROperand.getText());
    }

    
    @Override
    public PsiElement[] getChildren() {
        return new PsiElement[]{myLOperand, myOperation, myROperand};
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitBinaryExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    
    @Override
    public PsiExpression getLOperand() {
        return myLOperand;
    }

    
    @Override
    public PsiExpression getROperand() {
        return myROperand;
    }

    
    @Override
    public PsiJavaToken getOperationSign() {
        return myOperation;
    }

    
    @Override
    public IElementType getOperationTokenType() {
        return myOperation.getTokenType();
    }

    @Override
    public PsiJavaToken getTokenBeforeOperand( PsiExpression operand) {
        return getOperationSign();
    }

    @Override
    public PsiType getType() {
        return myLOperand.getType();
    }

    
    @Override
    public PsiExpression[] getOperands() {
        return new PsiExpression[]{getLOperand(), getROperand()};
    }

    @Override
    public String toString() {
        return "PsiBinaryExpression:" + getText();
    }
}
