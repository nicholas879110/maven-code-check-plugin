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


import com.gome.maven.openapi.util.JDOMUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;


import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Set;

public class XmlSerializer {
    private static final SerializationFilter TRUE_FILTER = new SerializationFilter() {
        @Override
        public boolean accepts(Accessor accessor, Object bean) {
            return true;
        }
    };

    private XmlSerializer() {
    }

    /**
     * Consider to use {@link SkipDefaultValuesSerializationFilters}
     */
    public static Element serialize( Object object) throws XmlSerializationException {
        return serialize(object, TRUE_FILTER);
    }

    
    public static Element serialize( Object object,  SerializationFilter filter) throws XmlSerializationException {
        return XmlSerializerImpl.serialize(object, filter == null ? TRUE_FILTER : filter);
    }

    
    public static Element serializeIfNotDefault( Object object,  SerializationFilter filter) {
        return XmlSerializerImpl.serializeIfNotDefault(object, filter == null ? TRUE_FILTER : filter);
    }

    
    public static <T> T deserialize(Document document, Class<T> aClass) throws XmlSerializationException {
        return deserialize(document.getRootElement(), aClass);
    }

    
    @SuppressWarnings({"unchecked"})
    public static <T> T deserialize(Element element, Class<T> aClass) throws XmlSerializationException {
        try {
            return (T)XmlSerializerImpl.getBinding(aClass).deserialize(null, element);
        }
        catch (XmlSerializationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new XmlSerializationException("Cannot deserialize class " + aClass.getName(), e);
        }
    }

    public static <T> T[] deserialize(Element[] elements, Class<T> aClass) throws XmlSerializationException {
        //noinspection unchecked
        T[] result = (T[])Array.newInstance(aClass, elements.length);

        for (int i = 0; i < result.length; i++) {
            result[i] = deserialize(elements[i], aClass);
        }

        return result;
    }

    
    public static <T> T deserialize(URL url, Class<T> aClass) throws XmlSerializationException {
        try {
            Document document = JDOMUtil.loadDocument(url);
            document = JDOMXIncluder.resolve(document, url.toExternalForm());
            return deserialize(document.getRootElement(), aClass);
        }
        catch (IOException e) {
            throw new XmlSerializationException(e);
        }
        catch (JDOMException e) {
            throw new XmlSerializationException(e);
        }
    }

    public static void deserializeInto( Object bean,  Element element) {
        deserializeInto(bean, element, null);
    }

    public static void deserializeInto( Object bean,  Element element,  Set<String> accessorNameTracker) {
        try {
            ((BeanBinding)XmlSerializerImpl.getBinding(bean.getClass())).deserializeInto(bean, element, accessorNameTracker);
        }
        catch (XmlSerializationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new XmlSerializationException(e);
        }
    }

    public static void serializeInto(final Object bean, final Element element) {
        serializeInto(bean, element, null);
    }

    public static void serializeInto( Object bean,  Element element,  SerializationFilter filter) {
        if (filter == null) {
            filter = TRUE_FILTER;
        }
        try {
            Binding binding = XmlSerializerImpl.getBinding(bean.getClass());
            assert binding instanceof BeanBinding;
            ((BeanBinding)binding).serializeInto(bean, element, filter);
        }
        catch (XmlSerializationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new XmlSerializationException(e);
        }
    }
}
