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
package com.gome.maven.xml;

import com.gome.maven.codeInsight.daemon.impl.HighlightInfoType;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.xml.SchemaPrefix;
import com.gome.maven.psi.impl.source.xml.TagNameReference;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.xml.XmlAttribute;
import com.gome.maven.psi.xml.XmlDocument;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.xml.util.XmlUtil;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlExtension {
    public static final ExtensionPointName<XmlExtension> EP_NAME = new ExtensionPointName<XmlExtension>("com.gome.maven.xml.xmlExtension");

    public static final XmlExtension DEFAULT_EXTENSION = new DefaultXmlExtension();

    public static XmlExtension getExtension(PsiFile file) {
        for (XmlExtension extension : Extensions.getExtensions(EP_NAME)) {
            if (extension.isAvailable(file)) {
                return extension;
            }
        }
        return DEFAULT_EXTENSION;
    }

    @SuppressWarnings("ConstantConditions")
    public static XmlExtension getExtensionByElement(PsiElement element) {
        final PsiFile psiFile = element.getContainingFile();
        if (psiFile != null) {
            return getExtension(psiFile);
        }
        return null;
    }

    public abstract boolean isAvailable(PsiFile file);

    public static class TagInfo {

        public final String name;
        public final String namespace;

        public TagInfo(String name, String namespace) {
            this.name = name;
            this.namespace = namespace;
        }

        
        public PsiElement getDeclaration() {
            return null;
        }
    }

    
    public abstract List<TagInfo> getAvailableTagNames( final XmlFile file,  final XmlTag context);

    
    public TagNameReference createTagNameReference(final ASTNode nameElement, final boolean startTagFlag) {
        return new TagNameReference(nameElement, startTagFlag);
    }

    
    public String[][] getNamespacesFromDocument(final XmlDocument parent, boolean declarationsExist) {
        return declarationsExist ? null : XmlUtil.getDefaultNamespaces(parent);
    }

    public boolean canBeDuplicated(XmlAttribute attribute) {
        return false;
    }

    public boolean isRequiredAttributeImplicitlyPresent(XmlTag tag, String attrName) {
        return false;
    }

    public HighlightInfoType getHighlightInfoType(XmlFile file) {
        return HighlightInfoType.ERROR;
    }

    
    public abstract SchemaPrefix getPrefixDeclaration(final XmlTag context, String namespacePrefix);

    public SearchScope getNsPrefixScope(XmlAttribute declaration) {
        return new LocalSearchScope(declaration.getParent());
    }

    public boolean shouldBeHighlightedAsTag(XmlTag tag) {
        return true;
    }

    
    public XmlElementDescriptor getElementDescriptor(XmlTag tag, XmlTag contextTag, final XmlElementDescriptor parentDescriptor) {
        return parentDescriptor.getElementDescriptor(tag, contextTag);
    }

    
    public XmlNSDescriptor getNSDescriptor(final XmlTag element, final String namespace, final boolean strict) {
        return element.getNSDescriptor(namespace, strict);
    }

    
    public XmlTag getParentTagForNamespace(XmlTag tag, XmlNSDescriptor namespace) {
        return tag.getParentTag();
    }

    
    public XmlFile getContainingFile(PsiElement element) {
        if (element == null) {
            return null;
        }
        final PsiFile psiFile = element.getContainingFile();
        return psiFile instanceof XmlFile ? (XmlFile)psiFile : null;
    }

    public XmlNSDescriptor getDescriptorFromDoctype(final XmlFile containingFile, XmlNSDescriptor descr) {
        return descr;
    }

    public boolean hasDynamicComponents(final PsiElement element) {
        return false;
    }

    public boolean isIndirectSyntax(final XmlAttributeDescriptor descriptor) {
        return false;
    }

    public boolean isCustomTagAllowed(final XmlTag tag) {
        return false;
    }

    public boolean needWhitespaceBeforeAttribute() {
        return true;
    }

    public boolean useXmlTagInsertHandler() {
        return true;
    }
}
