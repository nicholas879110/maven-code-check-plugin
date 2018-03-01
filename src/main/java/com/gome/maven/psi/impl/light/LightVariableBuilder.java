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
package com.gome.maven.psi.impl.light;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.ElementPresentationUtil;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;

/**
 * @author peter
 */
public class LightVariableBuilder<T extends LightVariableBuilder> extends LightElement implements PsiVariable, NavigationItem, OriginInfoAwareElement {
    private final String myName;
    private final PsiType myType;
    private volatile LightModifierList myModifierList;
    private volatile Icon myBaseIcon = PlatformIcons.VARIABLE_ICON;
    private String myOriginInfo;

    public LightVariableBuilder( String name,  String type,  PsiElement navigationElement) {
        this(name, JavaPsiFacade.getElementFactory(navigationElement.getProject()).createTypeFromText(type, navigationElement), navigationElement);
    }

    public LightVariableBuilder( String name,  PsiType type,  PsiElement navigationElement) {
        this(navigationElement.getManager(), name, type, JavaLanguage.INSTANCE);
        setNavigationElement(navigationElement);
    }

    public LightVariableBuilder(PsiManager manager,  String name,  PsiType type,  Language language) {
        super(manager, language);
        myName = name;
        myType = type;
        myModifierList = new LightModifierList(manager);
    }

    @Override
    public String toString() {
        return "LightVariableBuilder:" + getName();
    }

    
    @Override
    public PsiType getType() {
        return myType;
    }

    @Override
    
    public PsiModifierList getModifierList() {
        return myModifierList;
    }

    public T setModifiers(String... modifiers) {
        myModifierList = new LightModifierList(getManager(), getLanguage(), modifiers);
        return (T)this;
    }

    public T setModifierList(LightModifierList modifierList) {
        myModifierList = modifierList;
        return (T)this;
    }

    @Override
    public boolean hasModifierProperty(  String name) {
        return myModifierList.hasModifierProperty(name);
    }

    
    @Override
    public String getName() {
        return myName;
    }

    @Override
    public PsiTypeElement getTypeElement() {
        return null;
    }

    @Override
    public PsiExpression getInitializer() {
        return null;
    }

    @Override
    public boolean hasInitializer() {
        return false;
    }

    @Override
    public void normalizeDeclaration() throws IncorrectOperationException {
    }

    @Override
    public Object computeConstantValue() {
        return null;
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        return null;
    }

    @Override
    public PsiElement setName(  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException("setName is not implemented yet in com.gome.maven.psi.impl.light.LightVariableBuilder");
    }

    @Override
    protected boolean isVisibilitySupported() {
        return true;
    }

    @Override
    public Icon getElementIcon(final int flags) {
        final RowIcon baseIcon = ElementPresentationUtil.createLayeredIcon(myBaseIcon, this, false);
        return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon);
    }

    public T setBaseIcon(Icon baseIcon) {
        myBaseIcon = baseIcon;
        return (T)this;
    }

    
    @Override
    public String getOriginInfo() {
        return myOriginInfo;
    }

    public void setOriginInfo( String originInfo) {
        myOriginInfo = originInfo;
    }
}
