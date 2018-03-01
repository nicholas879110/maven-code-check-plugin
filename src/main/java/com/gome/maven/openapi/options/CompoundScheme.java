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
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.diagnostic.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CompoundScheme<T extends SchemeElement> implements ExternalizableScheme {

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.options.CompoundScheme");

    protected String myName;
    private final List<T> myElements = new ArrayList<T>();
    private final ExternalInfo myExternalInfo = new ExternalInfo();

    public CompoundScheme(final String name) {
        myName = name;
    }

    public void addElement(T t) {
        if (!contains(t)) {
            myElements.add(t);
        }
    }

    public void insertElement(T element, final int i) {
        if (!contains(element)) {
            myElements.add(i, element);
        }
    }

    public List<T> getElements() {
        return Collections.unmodifiableList(new ArrayList<T>(myElements));
    }

    @Override
    
    public String getName() {
        return myName;
    }

    @Override
    public void setName( final String name) {
        myName = name;
        for (T template : myElements) {
            template.setGroupName(name);
        }
    }

    public void removeElement(final T template) {
        for (Iterator<T> templateIterator = myElements.iterator(); templateIterator.hasNext();) {
            T t = templateIterator.next();
            if (t.getKey() != null && t.getKey().equals(template.getKey())) {
                templateIterator.remove();
            }
        }
    }

    public boolean isEmpty() {
        return myElements.isEmpty();
    }

    @Override
    
    public ExternalInfo getExternalInfo() {
        return myExternalInfo;
    }

    public CompoundScheme copy() {
        CompoundScheme result = createNewInstance(getName());
        for (T element : myElements) {
            //noinspection unchecked
            result.addElement(element.copy());
        }
        result.getExternalInfo().copy(getExternalInfo());
        return result;
    }

    private CompoundScheme createNewInstance(final String name) {
        try {
            Constructor<? extends CompoundScheme> constructor = getClass().getConstructor(String.class);
            return constructor.newInstance(name);
        }
        catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean contains(final T element) {
        for (T t : myElements) {
            if (t.getKey() != null && t.getKey().equals(element.getKey())) {
                return true;
            }
        }
        return false;
    }
}
