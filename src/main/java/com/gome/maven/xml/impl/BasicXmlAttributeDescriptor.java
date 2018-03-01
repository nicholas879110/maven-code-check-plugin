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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Aug 27, 2002
 * Time: 9:55:06 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.xml.impl;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.xml.XmlAttributeDescriptor;

public abstract class BasicXmlAttributeDescriptor extends XmlEnumerationDescriptor implements XmlAttributeDescriptor {
    @Override
    public String validateValue(XmlElement context, String value) {
        return null;
    }

    @Override
    public String getName(PsiElement context){
        return getName();
    }

    
    public String[] getEnumeratedValues( XmlElement context) {
        return getEnumeratedValues();
    }

    @Override
    public boolean isEnumerated(XmlElement context) {
        return isEnumerated();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    protected PsiElement getEnumeratedValueDeclaration(XmlElement xmlElement, String value) {
        String[] values = getEnumeratedValues();
        if (values == null || values.length == 0) return getDeclaration();
        return ArrayUtilRt.find(values, value) != -1 ? getDeclaration() : null;
    }

    @Override
    protected PsiElement getDefaultValueDeclaration() {
        return getDeclaration();
    }
}
