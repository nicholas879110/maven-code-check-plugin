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
package com.gome.maven.html.impl;

import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.psi.xml.XmlAttribute;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.xml.XmlAttributeDescriptor;
import com.gome.maven.xml.XmlAttributeDescriptorsProvider;
import com.gome.maven.xml.XmlElementDescriptor;
import com.gome.maven.xml.XmlExtension;
import com.gome.maven.xml.impl.schema.AnyXmlElementDescriptor;
import com.gome.maven.xml.impl.schema.XmlElementDescriptorImpl;
import com.gome.maven.xml.util.HtmlUtil;
import com.gome.maven.xml.util.XmlUtil;

/**
 * @author Maxim.Mossienko
 */
public class RelaxedHtmlFromSchemaElementDescriptor extends XmlElementDescriptorImpl {

    RelaxedHtmlFromSchemaElementDescriptor(XmlTag tag) {
        super(tag);
    }

    @Override
    public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
        XmlElementDescriptor elementDescriptor = super.getElementDescriptor(childTag, contextTag);

        if (elementDescriptor == null) {
            return getRelaxedDescriptor(this, childTag);
        }

        return elementDescriptor;
    }

    @Override
    public XmlElementDescriptor[] getElementsDescriptors(final XmlTag context) {
        return ArrayUtil.mergeArrays(
                super.getElementsDescriptors(context),
                HtmlUtil.getCustomTagDescriptors(context)
        );
    }

    public static XmlElementDescriptor getRelaxedDescriptor(XmlElementDescriptor base, final XmlTag childTag) {
        final String namespace = childTag.getNamespace();
        final XmlExtension extension = XmlExtension.getExtensionByElement(childTag);
        if(!XmlUtil.XHTML_URI.equals(namespace) &&
                ( base.getContentType() != XmlElementDescriptor.CONTENT_TYPE_EMPTY ||
                        (extension != null && extension.isCustomTagAllowed(childTag)) // allow custom tag
                ) ) {
            return new AnyXmlElementDescriptor(base,childTag.getNSDescriptor(namespace,true));
        }
        return null;
    }

    @Override
    public XmlAttributeDescriptor[] getAttributesDescriptors(final XmlTag context) {
        return addAttrDescriptorsForFacelets(context, super.getAttributesDescriptors(context));
    }

    public static XmlAttributeDescriptor[] addAttrDescriptorsForFacelets(final XmlTag context,
                                                                         XmlAttributeDescriptor[] descriptors) {
        if (context == null) {
            return descriptors;
        }
        for (XmlAttributeDescriptorsProvider provider: Extensions.getExtensions(XmlAttributeDescriptorsProvider.EP_NAME)) {
            descriptors = ArrayUtil.mergeArrays(descriptors, provider.getAttributeDescriptors(context), XmlAttributeDescriptor.ARRAY_FACTORY);
        }
        return descriptors;
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
        return getAttributeDescriptor(attribute.getName(), attribute.getParent());
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, final XmlTag context) {
        final XmlAttributeDescriptor descriptor = super.getAttributeDescriptor(attributeName.toLowerCase(), context);
        if (descriptor != null) return descriptor;

        return getAttributeDescriptorFromFacelets(attributeName, context);
    }


    public static XmlAttributeDescriptor getAttributeDescriptorFromFacelets(final String attributeName, final XmlTag context) {
        if (context == null) {
            return null;
        }
        for (XmlAttributeDescriptorsProvider provider: Extensions.getExtensions(XmlAttributeDescriptorsProvider.EP_NAME)) {
            final XmlAttributeDescriptor descriptor = provider.getAttributeDescriptor(attributeName, context);
            if (descriptor != null) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public boolean allowElementsFromNamespace(final String namespace, final XmlTag context) {
        return true;
    }
}
