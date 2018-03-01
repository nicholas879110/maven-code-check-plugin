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
package com.gome.maven.psi.impl.beanProperties;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiType;
import com.gome.maven.psi.impl.FakePsiElement;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.meta.PsiPresentableMetaData;
import com.gome.maven.psi.util.PropertyUtil;
import com.gome.maven.util.ArrayUtil;

import javax.swing.*;

/**
 * @author peter
 */
public class BeanPropertyElement extends FakePsiElement implements PsiMetaOwner, PsiPresentableMetaData {
    private final PsiMethod myMethod;
    private final String myName;

    public BeanPropertyElement( PsiMethod method,  String name) {
        myMethod = method;
        myName = name;
    }

    
    public PsiType getPropertyType() {
        return PropertyUtil.getPropertyType(myMethod);
    }

    
    public PsiMethod getMethod() {
        return myMethod;
    }

    
    @Override
    public PsiElement getNavigationElement() {
        return myMethod;
    }

    @Override
    public PsiManager getManager() {
        return myMethod.getManager();
    }

    @Override
    public PsiElement getDeclaration() {
        return this;
    }

    @Override
    
    public String getName(PsiElement context) {
        return getName();
    }

    @Override
    
    public String getName() {
        return myName;
    }

    @Override
    public void init(PsiElement element) {

    }

    @Override
    public Object[] getDependences() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    
    public Icon getIcon(boolean flags) {
        return AllIcons.Nodes.Property;
    }

    @Override
    public PsiElement getParent() {
        return myMethod;
    }

    @Override
    
    public PsiMetaData getMetaData() {
        return this;
    }

    @Override
    public String getTypeName() {
        return IdeBundle.message("bean.property");
    }

    @Override
    
    public Icon getIcon() {
        return getIcon(0);
    }

    @Override
    public TextRange getTextRange() {
        return TextRange.from(0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BeanPropertyElement element = (BeanPropertyElement)o;

        if (!myMethod.equals(element.myMethod)) return false;
        if (!myName.equals(element.myName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = myMethod.hashCode();
        result = 31 * result + myName.hashCode();
        return result;
    }
}
