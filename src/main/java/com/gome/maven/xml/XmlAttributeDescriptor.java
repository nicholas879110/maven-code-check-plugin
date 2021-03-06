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

import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.util.ArrayFactory;

/**
 * @author Mike
 */
public interface XmlAttributeDescriptor extends PsiMetaData {
    XmlAttributeDescriptor[] EMPTY = new XmlAttributeDescriptor[0];
    ArrayFactory<XmlAttributeDescriptor> ARRAY_FACTORY = new ArrayFactory<XmlAttributeDescriptor>() {
        
        @Override
        public XmlAttributeDescriptor[] create(int count) {
            return new XmlAttributeDescriptor[count];
        }
    };

    boolean isRequired();
    boolean isFixed();
    boolean hasIdType();
    boolean hasIdRefType();

    
    String getDefaultValue();

    //todo: refactor to hierarchy of value descriptor?
    boolean isEnumerated();
    
    String[] getEnumeratedValues();

    
    String validateValue(XmlElement context, String value);
}
