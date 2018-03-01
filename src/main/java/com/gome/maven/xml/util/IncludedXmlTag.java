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
package com.gome.maven.xml.util;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.impl.source.xml.XmlTagValueImpl;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.xml.XmlAttribute;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.xml.XmlTagChild;
import com.gome.maven.psi.xml.XmlTagValue;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.xml.XmlElementDescriptor;
import com.gome.maven.xml.XmlNSDescriptor;

import java.util.Map;

/**
 * @author peter
 */
public class IncludedXmlTag extends IncludedXmlElement<XmlTag> implements XmlTag {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.xml.util.IncludedXmlTag");
    public IncludedXmlTag( XmlTag original,  PsiElement parent) {
        super(original, parent);
    }

    @Override
    
    public XmlTag getParentTag() {
        return getParent() instanceof XmlTag ? (XmlTag)getParent() : null;
    }

    @Override
    
    
    public String getName() {
        return getOriginal().getName();
    }

    @Override
    public PsiElement setName(  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    
    
    public String getNamespace() {
        XmlTag original = getOriginal();
        LOG.assertTrue(original.isValid());
        return original.getNamespace();
    }

    @Override
    
    
    public String getLocalName() {
        return getOriginal().getLocalName();
    }

    @Override
    
    public XmlElementDescriptor getDescriptor() {
        return getOriginal().getDescriptor();
    }

    @Override
    
    public XmlAttribute[] getAttributes() {
        XmlAttribute[] original = getOriginal().getAttributes();
        XmlAttribute[] attributes = new XmlAttribute[original.length];
        for (int i = 0; i < original.length; i++) {
            XmlAttribute attribute = original[i];
            attributes[i] = new IncludedXmlAttribute(attribute, this);
        }
        return attributes;
    }

    @Override
    
    public XmlAttribute getAttribute( String name,  String namespace) {
        XmlAttribute attribute = getOriginal().getAttribute(name, namespace);
        return attribute == null ? null : new IncludedXmlAttribute(attribute, this);
    }

    @Override
    
    public XmlAttribute getAttribute( String qname) {
        XmlAttribute attribute = getOriginal().getAttribute(qname);
        return attribute == null ? null : new IncludedXmlAttribute(attribute, this);
    }

    @Override
    
    public String getAttributeValue( String name,  String namespace) {
        return getOriginal().getAttributeValue(name, namespace);
    }

    @Override
    
    public String getAttributeValue( String qname) {
        return getOriginal().getAttributeValue(qname);
    }

    @Override
    public XmlAttribute setAttribute( String name,  String namespace,  String value) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    public XmlAttribute setAttribute( String qname,  String value) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    public XmlTag createChildTag( String localName,
                                  String namespace,
                                   String bodyText,
                                 boolean enforceNamespacesDeep) {
        return getOriginal().createChildTag(localName, namespace, bodyText, enforceNamespacesDeep);
    }

    @Override
    public XmlTag addSubTag(XmlTag subTag, boolean first) {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    
    public XmlTag[] getSubTags() {
        return wrapTags(getOriginal().getSubTags());
    }

    private XmlTag[] wrapTags(XmlTag[] original) {
        XmlTag[] result = new XmlTag[original.length];
        for (int i = 0; i < original.length; i++) {
            result[i] = new IncludedXmlTag(original[i], this);
        }
        return result;
    }

    @Override
    
    public XmlTag[] findSubTags( String qname) {
        return wrapTags(getOriginal().findSubTags(qname));
    }

    @Override
    
    public XmlTag[] findSubTags( String localName,  String namespace) {
        return wrapTags(getOriginal().findSubTags(localName, namespace));
    }

    @Override
    
    public XmlTag findFirstSubTag( String qname) {
        XmlTag tag = getOriginal().findFirstSubTag(qname);
        return tag == null ? null : new IncludedXmlTag(tag, this);
    }

    @Override
    
    
    public String getNamespacePrefix() {
        return getOriginal().getNamespacePrefix();
    }

    @Override
    
    
    public String getNamespaceByPrefix( String prefix) {
        return getOriginal().getNamespaceByPrefix(prefix);
    }

    @Override
    
    public String getPrefixByNamespace( String namespace) {
        return getOriginal().getPrefixByNamespace(namespace);
    }

    @Override
    public String[] knownNamespaces() {
        return getOriginal().knownNamespaces();
    }

    @Override
    public boolean hasNamespaceDeclarations() {
        return getOriginal().hasNamespaceDeclarations();
    }

    @Override
    
    public Map<String, String> getLocalNamespaceDeclarations() {
        return getOriginal().getLocalNamespaceDeclarations();
    }

    @Override
    
    public XmlTagValue getValue() {
        return XmlTagValueImpl.createXmlTagValue(this);
    }

    @Override
    
    public XmlNSDescriptor getNSDescriptor( String namespace, boolean strict) {
        return getOriginal().getNSDescriptor(namespace, strict);
    }

    @Override
    public boolean isEmpty() {
        return getOriginal().isEmpty();
    }

    @Override
    public void collapseIfEmpty() {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    
    
    public String getSubTagText( String qname) {
        return getOriginal().getSubTagText(qname);
    }

    @Override
    public PsiMetaData getMetaData() {
        return null;
    }

    @Override
    public XmlTagChild getNextSiblingInTag() {
        return null;
    }

    @Override
    public XmlTagChild getPrevSiblingInTag() {
        return null;
    }
}