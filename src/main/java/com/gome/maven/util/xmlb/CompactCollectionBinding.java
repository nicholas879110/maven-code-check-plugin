/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.util.xmlb;

import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import java.util.List;

/**
 * @see com.gome.maven.util.xmlb.annotations.CollectionBean
 */
class CompactCollectionBinding extends Binding {
    private final String name;

    protected CompactCollectionBinding( MutableAccessor accessor) {
        super(accessor);

        name = myAccessor.getName();
    }

    
    @Override
    public Object serialize( Object o,  Object context,  SerializationFilter filter) {
        Element result = new Element(name);
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>)o;
        if (list.isEmpty()) {
            return result;
        }

        for (String item : list) {
            result.addContent(new Element("item").setAttribute("value", item));
        }
        return result;
    }

    
    @Override
    public Object deserialize(Object context,  Element element) {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>)context;
        list.clear();
        if (element.getName().equals(name)) {
            for (Element item : (List<Element>)element.getChildren("item")) {
                ContainerUtil.addIfNotNull(list, item.getAttributeValue("value"));
            }
        }
        else {
            // JDOMExternalizableStringList format
            Element value = element.getChild("value");
            if (value != null) {
                value = value.getChild("list");
            }
            if (value != null) {
                for (Element item : (List<Element>)value.getChildren("item")) {
                    //noinspection SpellCheckingInspection
                    ContainerUtil.addIfNotNull(list, item.getAttributeValue("itemvalue"));
                }
            }
        }
        return list;
    }

    @Override
    public boolean isBoundTo( Element element) {
        String elementName = element.getName();
        if (isNameEqual(elementName)) {
            return true;
        }
        else if (elementName.equals(Constants.OPTION)) {
            // JDOMExternalizableStringList format
            return isNameEqual(element.getAttributeValue(Constants.NAME));
        }
        return false;
    }

    private boolean isNameEqual( String value) {
        if (value == null) {
            return false;
        }
        else if (value.equals(name)) {
            return true;
        }
        else if (value.length() == (name.length() + 2) && value.startsWith("my")) {
            return Character.isUpperCase(value.charAt(2)) && value.regionMatches(true, 2, name, 0, name.length());
        }
        else {
            return false;
        }
    }
}