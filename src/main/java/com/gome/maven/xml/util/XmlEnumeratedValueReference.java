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

import com.gome.maven.codeInsight.daemon.EmptyResolveMessageProvider;
import com.gome.maven.codeInsight.daemon.XmlErrorMessages;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReferenceBase;
import com.gome.maven.psi.ResolvingHint;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.xml.impl.XmlEnumerationDescriptor;

/**
 * @author Dmitry Avdeev
 *         Date: 16.08.13
 */
public class XmlEnumeratedValueReference extends PsiReferenceBase<XmlElement> implements EmptyResolveMessageProvider, ResolvingHint {
    private final XmlEnumerationDescriptor myDescriptor;

    public XmlEnumeratedValueReference(XmlElement value, XmlEnumerationDescriptor descriptor) {
        super(value);
        myDescriptor = descriptor;
    }

    public XmlEnumeratedValueReference(XmlElement value, XmlEnumerationDescriptor descriptor, TextRange range) {
        super(value, range);
        myDescriptor = descriptor;
    }

    @Override
    public boolean canResolveTo(Class<? extends PsiElement> elementClass) {
        return ReflectionUtil.isAssignable(XmlElement.class, elementClass);
    }

    
    @Override
    public PsiElement resolve() {
        return myDescriptor.getValueDeclaration(getElement(), getValue());
    }

    
    @Override
    public Object[] getVariants() {
        if (myDescriptor.isFixed()) {
            String defaultValue = myDescriptor.getDefaultValue();
            return defaultValue == null ? ArrayUtil.EMPTY_OBJECT_ARRAY : new Object[] {defaultValue};
        }
        else {
            String[] values = myDescriptor.getValuesForCompletion();
            return values == null ? ArrayUtil.EMPTY_OBJECT_ARRAY : values;
        }
    }

    
    @Override
    public String getUnresolvedMessagePattern() {
        String name = getElement() instanceof XmlTag ? "tag" : "attribute";
        return myDescriptor.isFixed()
                ? XmlErrorMessages.message("should.have.fixed.value", StringUtil.capitalize(name), myDescriptor.getDefaultValue())
                : XmlErrorMessages.message("wrong.value", name);
    }
}
