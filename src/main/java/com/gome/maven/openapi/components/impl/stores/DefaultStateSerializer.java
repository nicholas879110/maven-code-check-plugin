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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.xmlb.XmlSerializer;
import org.jdom.Element;

@SuppressWarnings({"deprecation"})
public class DefaultStateSerializer {
    private static final Logger LOG = Logger.getInstance(DefaultStateSerializer.class);

    private DefaultStateSerializer() {
    }

    @SuppressWarnings({"unchecked"})
    
    public static <T> T deserializeState( Element stateElement, Class <T> stateClass,  T mergeInto) {
        if (stateElement == null) {
            return mergeInto;
        }
        else if (stateClass == Element.class) {
            return (T)stateElement;
        }
        else if (JDOMExternalizable.class.isAssignableFrom(stateClass)) {
            if (mergeInto != null) {
                String elementText = JDOMUtil.writeElement(stateElement, "\n");
                LOG.error("State is " + stateClass.getName() + ", merge into is " + mergeInto.toString() + ", state element text is " + elementText);
            }

            T t = ReflectionUtil.newInstance(stateClass);
            try {
                ((JDOMExternalizable)t).readExternal(stateElement);
                return t;
            }
            catch (InvalidDataException e) {
                throw new RuntimeException(e);
            }
        }
        else if (mergeInto == null) {
            return XmlSerializer.deserialize(stateElement, stateClass);
        }
        else {
            XmlSerializer.deserializeInto(mergeInto, stateElement);
            return mergeInto;
        }
    }
}
