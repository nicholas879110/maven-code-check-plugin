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
package com.gome.maven.psi.xml;

import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.xml.XmlAttributeDescriptor;

/**
 * @author Mike
 */
public interface XmlAttribute extends XmlElement, PsiNamedElement {
    XmlAttribute[] EMPTY_ARRAY = new XmlAttribute[0];

    @Override
      String getName();

      String getLocalName();

    XmlElement getNameElement();

      String getNamespace();

      String getNamespacePrefix();

    @Override
    XmlTag getParent();

    /**
     * @return text inside XML attribute with quotes stripped off
     */
    
    String getValue();

    /**
     * @return text inside XML attribute with quotes stripped off and XML char entities replaced with corresponding characters
     */
    
    String getDisplayValue();

    /**
     * @param offset in string returned by {@link #getText()} (with quotes stripped)
     * @return offset in the string returned from {@link #getDisplayValue()} or -1 if the offset is out of valid range
     */
    int physicalToDisplay(int offset);
    /**
     * @param offset in the string returned from {@link #getDisplayValue()}
     * @return offset in string returned by {@link #getText()} (with quotes stripped) or -1 if the offset is out of valid range
     */
    int displayToPhysical(int offset);

    /**
     * @return TextRange of the XML attribute value.
     * If quotes are present, it returns <code>new TextRange(1, getTextLength()-1)</code>, otherwise it is <code>new TextRange(0, getTextLength())</code>
     */
    
    TextRange getValueTextRange();

    /**
     * @return true if the attribute is a namespace declaration (its name equals to <code>xmlns</code> or starts with <code>xmlns:</code>)
     */
    boolean isNamespaceDeclaration();

     XmlAttributeDescriptor getDescriptor();

    // In this case function is also used to get references from attribute value
    
    XmlAttributeValue getValueElement();

    void setValue(String value) throws IncorrectOperationException;
}
