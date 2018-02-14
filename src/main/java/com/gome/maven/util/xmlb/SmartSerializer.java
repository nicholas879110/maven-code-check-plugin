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

import com.gome.maven.util.ThreeState;
import gnu.trove.TObjectFloatHashMap;
import org.jdom.Element;

import java.util.LinkedHashSet;

public final class SmartSerializer {
    private LinkedHashSet<String> mySerializedAccessorNameTracker;
    private TObjectFloatHashMap<String> myOrderedBindings;
    private final SerializationFilter mySerializationFilter;

    public SmartSerializer(boolean trackSerializedNames, boolean useSkipEmptySerializationFilter) {
        mySerializedAccessorNameTracker = trackSerializedNames ? new LinkedHashSet<String>() : null;

        mySerializationFilter = useSkipEmptySerializationFilter ?
                new SkipEmptySerializationFilter() {
                    @Override
                    protected ThreeState accepts( String name,  Object beanValue) {
                        return mySerializedAccessorNameTracker != null && mySerializedAccessorNameTracker.contains(name) ? ThreeState.YES : ThreeState.UNSURE;
                    }
                } :
                new SkipDefaultValuesSerializationFilters() {
                    @Override
                    public boolean accepts( Accessor accessor,  Object bean) {
                        if (mySerializedAccessorNameTracker != null && mySerializedAccessorNameTracker.contains(accessor.getName())) {
                            return true;
                        }
                        return super.accepts(accessor, bean);
                    }
                };
    }

    public SmartSerializer() {
        this(true, false);
    }

    
    public static SmartSerializer skipEmptySerializer() {
        return new SmartSerializer(true, true);
    }

    public void readExternal( Object bean,  Element element) {
        if (mySerializedAccessorNameTracker != null) {
            mySerializedAccessorNameTracker.clear();
            myOrderedBindings = null;
        }

        BeanBinding beanBinding = getBinding(bean);
        beanBinding.deserializeInto(bean, element, mySerializedAccessorNameTracker);

        if (mySerializedAccessorNameTracker != null) {
            myOrderedBindings = beanBinding.computeBindingWeights(mySerializedAccessorNameTracker);
        }
    }

    public void writeExternal( Object bean,  Element element) {
        writeExternal(bean, element, true);
    }

    public void writeExternal( Object bean,  Element element, boolean preserveCompatibility) {
        BeanBinding binding = getBinding(bean);
        if (preserveCompatibility && myOrderedBindings != null) {
            binding.sortBindings(myOrderedBindings);
        }

        if (preserveCompatibility || mySerializedAccessorNameTracker == null) {
            binding.serializeInto(bean, element, mySerializationFilter);
        }
        else {
            LinkedHashSet<String> oldTracker = mySerializedAccessorNameTracker;
            try {
                mySerializedAccessorNameTracker = null;
                binding.serializeInto(bean, element, mySerializationFilter);
            }
            finally {
                mySerializedAccessorNameTracker = oldTracker;
            }
        }
    }

    
    private static BeanBinding getBinding( Object bean) {
        return (BeanBinding)XmlSerializerImpl.getBinding(bean.getClass());
    }
}