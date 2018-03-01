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
package com.gome.maven.xml.util;

import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.xml.XmlAttribute;
import com.gome.maven.psi.xml.XmlAttributeValue;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.xml.XmlAttributeDescriptor;

/**
 * @author peter
 */
public class IncludedXmlAttribute extends IncludedXmlElement<XmlAttribute> implements XmlAttribute {

    public IncludedXmlAttribute( XmlAttribute original,  XmlTag parent) {
        super(original, parent);
    }

    @Override
    
    
    public String getName() {
        return getOriginal().getName();
    }

    @Override
    public PsiElement setName(  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included elements");
    }

    @Override
    
    
    public String getLocalName() {
        return getOriginal().getLocalName();
    }

    @Override
    public XmlElement getNameElement() {
        return getOriginal().getNameElement();
    }

    @Override
    
    
    public String getNamespace() {
        return getOriginal().getNamespace();
    }

    @Override
    
    
    public String getNamespacePrefix() {
        return getOriginal().getNamespacePrefix();
    }

    @Override
    public XmlTag getParent() {
        return (XmlTag)super.getParent();
    }

    @Override
    public String getValue() {
        return getOriginal().getValue();
    }

    @Override
    public String getDisplayValue() {
        return getOriginal().getDisplayValue();
    }

    @Override
    public int physicalToDisplay(int offset) {
        return getOriginal().physicalToDisplay(offset);
    }

    @Override
    public int displayToPhysical(int offset) {
        return getOriginal().displayToPhysical(offset);
    }

    
    @Override
    public TextRange getValueTextRange() {
        return getOriginal().getValueTextRange();
    }

    @Override
    public boolean isNamespaceDeclaration() {
        return getOriginal().isNamespaceDeclaration();
    }

    @Override
    
    public XmlAttributeDescriptor getDescriptor() {
        return getOriginal().getDescriptor();
    }

    @Override
    
    public XmlAttributeValue getValueElement() {
        return getOriginal().getValueElement();
    }

    @Override
    public void setValue(String value) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included elements");
    }
}
