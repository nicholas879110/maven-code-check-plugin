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
package com.gome.maven.patterns;

import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.util.ProcessingContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author spleaner
 */
public class XmlTagPattern<Self extends XmlTagPattern<Self>> extends XmlNamedElementPattern<XmlTag, Self> {
    protected XmlTagPattern() {
        super(new InitialPatternCondition<XmlTag>(XmlTag.class) {
            @Override
            public boolean accepts( final Object o, final ProcessingContext context) {
                return o instanceof XmlTag;
            }
        });
    }

    protected XmlTagPattern( final InitialPatternCondition<XmlTag> condition) {
        super(condition);
    }

    @Override
    protected String getLocalName(XmlTag tag) {
        return tag.getLocalName();
    }

    @Override
    protected String getNamespace(XmlTag tag) {
        return tag.getNamespace();
    }

    public Self withAttributeValue(  final String attributeName,  final String attributeValue) {
        return with(new PatternCondition<XmlTag>("withAttributeValue") {
            @Override
            public boolean accepts( final XmlTag xmlTag, final ProcessingContext context) {
                return Comparing.equal(xmlTag.getAttributeValue(attributeName), attributeValue);
            }
        });
    }

    public Self withAnyAttribute(  final String... attributeNames) {
        return with(new PatternCondition<XmlTag>("withAnyAttribute") {
            @Override
            public boolean accepts( final XmlTag xmlTag, final ProcessingContext context) {
                for (String attributeName : attributeNames) {
                    if (xmlTag.getAttribute(attributeName) != null) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public Self withDescriptor( final ElementPattern<? extends PsiMetaData> metaDataPattern) {
        return with(new PatternCondition<XmlTag>("withDescriptor") {
            @Override
            public boolean accepts( final XmlTag xmlTag, final ProcessingContext context) {
                return metaDataPattern.accepts(xmlTag.getDescriptor());
            }
        });
    }

    public Self isFirstSubtag( final ElementPattern pattern) {
        return with(new PatternCondition<XmlTag>("isFirstSubtag") {
            @Override
            public boolean accepts( final XmlTag xmlTag, final ProcessingContext context) {
                final XmlTag parent = xmlTag.getParentTag();
                return parent != null &&
                        pattern.accepts(parent, context) && parent.getSubTags()[0] == xmlTag;
            }
        });
    }

    public Self withFirstSubTag( final ElementPattern<? extends XmlTag> pattern) {
        return withSubTags(StandardPatterns.<XmlTag>collection().first(pattern));
    }

    public Self withSubTags( final ElementPattern<? extends Collection<XmlTag>> pattern) {
        return with(new PatternCondition<XmlTag>("withSubTags") {
            @Override
            public boolean accepts( final XmlTag xmlTag, final ProcessingContext context) {
                return pattern.accepts(Arrays.asList(xmlTag.getSubTags()), context);
            }
        });
    }

    public Self withoutAttributeValue(  final String attributeName,  final String attributeValue) {
        return and(StandardPatterns.not(withAttributeValue(attributeName, attributeValue)));
    }

    public static class Capture extends XmlTagPattern<Capture> {
    }
}
