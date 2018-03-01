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

import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.TreeElement;

public class ClsLiteralExpressionImpl extends ClsElementImpl implements PsiLiteralExpression {
    private ClsElementImpl myParent;
    private final String myText;
    private final PsiType myType;
    private final Object myValue;

    public ClsLiteralExpressionImpl(ClsElementImpl parent, String text, PsiType type, Object value) {
        myParent = parent;
        myText = text;
        myType = type;
        myValue = value;
    }

    void setParent(ClsElementImpl parent) {
        myParent = parent;
    }

    @Override
    public PsiType getType() {
        return myType;
    }

    @Override
    public Object getValue() {
        return myValue;
    }

    @Override
    public String getText() {
        return myText;
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        buffer.append(getText());
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, JavaElementType.LITERAL_EXPRESSION);
    }

    @Override
    
    public PsiElement[] getChildren() {
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    public String toString() {
        return "PsiLiteralExpression:" + getText();
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitLiteralExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
