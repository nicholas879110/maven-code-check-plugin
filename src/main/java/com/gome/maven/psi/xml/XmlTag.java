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
package com.gome.maven.psi.xml;

import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.xml.XmlElementDescriptor;
import com.gome.maven.xml.XmlNSDescriptor;

import java.util.Map;

/**
 * @author Mike
 */
public interface XmlTag extends XmlElement, PsiNamedElement, PsiMetaOwner, XmlTagChild {
    XmlTag[] EMPTY = new XmlTag[0];

    @Override
      String getName();
      String getNamespace();
      String getLocalName();

     XmlElementDescriptor getDescriptor();

     XmlAttribute[] getAttributes();

     XmlAttribute getAttribute( String name,  String namespace);

    /**
     * Returns a tag attribute by qualified name.
     *
     * @param qname qualified attribute name, like "ns:name" or "name".
     * @return null if the attribute not exist.
     * @see #getAttribute(String, String)
     */
     XmlAttribute getAttribute( String qname);

     String getAttributeValue( String name,  String namespace);

    /**
     * Returns a tag attribute value by qualified name.
     *
     * @param qname qualified attribute name, like "ns:name" or "name".
     * @return null if the attribute not exist.
     * @see #getAttributeValue(String, String)
     */
     String getAttributeValue( String qname);

    XmlAttribute setAttribute( String name,  String namespace,  String value) throws IncorrectOperationException;
    XmlAttribute setAttribute( String qname,  String value) throws IncorrectOperationException;

    /**
     * Creates a new child tag
     * @param localName new tag's name
     * @param namespace new tag's namespace
     * @param bodyText pass null to create collapsed tag, empty string means creating expanded one
     * @param enforceNamespacesDeep if you pass some xml tags to <code>bodyText</code> parameter, this flag sets namespace prefixes for them
     * @return created tag. Use {@link #addSubTag(XmlTag, boolean)}} to add it to parent
     */
    XmlTag createChildTag( String localName,  String namespace,   String bodyText, boolean enforceNamespacesDeep);
    XmlTag addSubTag(XmlTag subTag, boolean first);

     XmlTag[] getSubTags();
     XmlTag[] findSubTags( String qname);

    /**
     * @param localName non-qualified tag name.
     * @param namespace if null, name treated as qualified name to find.
     */
     XmlTag[] findSubTags( String localName,  String namespace);

     XmlTag findFirstSubTag( String qname);

      String getNamespacePrefix();
      String getNamespaceByPrefix( String prefix);
     String getPrefixByNamespace( String namespace);
    String[] knownNamespaces();

    boolean hasNamespaceDeclarations();

    /**
     * @return map keys: prefixes values: namespaces
     */
     Map<String, String> getLocalNamespaceDeclarations();

     XmlTagValue getValue();

     XmlNSDescriptor getNSDescriptor( String namespace, boolean strict);

    boolean isEmpty();

    void collapseIfEmpty();

     
    String getSubTagText( String qname);
}
