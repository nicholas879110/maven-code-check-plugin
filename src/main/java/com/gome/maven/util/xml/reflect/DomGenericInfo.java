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
package com.gome.maven.util.xml.reflect;

import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.GenericDomValue;

import java.util.List;

/**
 * @author peter
 */
public interface DomGenericInfo {

    
    String getElementName(DomElement element);

    
    List<? extends AbstractDomChildrenDescription> getChildrenDescriptions();

    
    List<? extends DomFixedChildDescription> getFixedChildrenDescriptions();

    
    List<? extends DomCollectionChildDescription> getCollectionChildrenDescriptions();

    
    List<? extends DomAttributeChildDescription> getAttributeChildrenDescriptions();

     DomFixedChildDescription getFixedChildDescription( String tagName);

     DomFixedChildDescription getFixedChildDescription( String tagName,  String namespaceKey);

     DomCollectionChildDescription getCollectionChildDescription( String tagName);

     DomCollectionChildDescription getCollectionChildDescription( String tagName,  String namespaceKey);

    
    DomAttributeChildDescription getAttributeChildDescription( String attributeName);

    
    DomAttributeChildDescription getAttributeChildDescription( String attributeName,  String namespaceKey);

    /**
     * @return true, if there's no children in the element, only tag value accessors
     */
    boolean isTagValueElement();

    /**
     *
     * @param element
     * @return {@link com.gome.maven.psi.xml.XmlAttributeValue} or {@link com.gome.maven.psi.xml.XmlTag}
     */
    @Deprecated
    
    XmlElement getNameElement(DomElement element);

    
    GenericDomValue getNameDomElement(DomElement element);

    
    List<? extends CustomDomChildrenDescription> getCustomNameChildrenDescription();
}
