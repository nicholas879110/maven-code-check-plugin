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

import com.gome.maven.application.options.PathMacrosCollector;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.components.PathMacroSubstitutor;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.SmartHashSet;
import com.gome.maven.util.containers.StringInterner;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.*;

import static com.gome.maven.openapi.components.impl.stores.StateMap.getNewByteIfDiffers;

public class StorageData extends StorageDataBase {
    private static final Logger LOG = Logger.getInstance(StorageData.class);
    public static final String COMPONENT = "component";
    public static final String NAME = "name";

    private final StateMap myStates;

    protected final String myRootElementName;

    public StorageData() {
        this(COMPONENT);
    }

    public StorageData( String rootElementName) {
        myStates = new StateMap();
        myRootElementName = rootElementName;
    }

    StorageData( StorageData storageData) {
        myRootElementName = storageData.myRootElementName;
        myStates = new StateMap(storageData.myStates);
    }

    @Override
    
    public Set<String> getComponentNames() {
        return myStates.keys();
    }

    public void load( Element rootElement,  PathMacroSubstitutor pathMacroSubstitutor, boolean intern) {
        if (pathMacroSubstitutor != null) {
            pathMacroSubstitutor.expandPaths(rootElement);
        }

        StringInterner interner = intern ? new StringInterner() : null;
        for (Iterator<Element> iterator = rootElement.getChildren(COMPONENT).iterator(); iterator.hasNext(); ) {
            Element element = iterator.next();
            String name = getComponentNameIfValid(element);
            if (name == null || !(element.getAttributes().size() > 1 || !element.getChildren().isEmpty())) {
                continue;
            }

            iterator.remove();
            if (interner != null) {
                JDOMUtil.internElement(element, interner);
            }

            myStates.put(name, element);

            if (pathMacroSubstitutor instanceof TrackingPathMacroSubstitutor) {
                ((TrackingPathMacroSubstitutor)pathMacroSubstitutor).addUnknownMacros(name, PathMacrosCollector.getMacroNames(element));
            }

            // remove only after "getMacroNames" - some PathMacroFilter requires element name attribute
            element.removeAttribute(NAME);
        }
    }

    
    static String getComponentNameIfValid( Element element) {
        String name = element.getAttributeValue(NAME);
        if (StringUtil.isEmpty(name)) {
            LOG.warn("No name attribute for component in " + JDOMUtil.writeElement(element));
            return null;
        }
        return name;
    }

    
    protected Element save( Map<String, Element> newLiveStates) {
        if (myStates.isEmpty()) {
            return null;
        }

        Element rootElement = new Element(myRootElementName);
        String[] componentNames = ArrayUtil.toStringArray(myStates.keys());
        Arrays.sort(componentNames);
        for (String componentName : componentNames) {
            assert componentName != null;
            Element element = myStates.getElement(componentName, newLiveStates);
            // name attribute should be first
            List<Attribute> elementAttributes = element.getAttributes();
            if (elementAttributes.isEmpty()) {
                element.setAttribute(NAME, componentName);
            }
            else {
                Attribute nameAttribute = element.getAttribute(NAME);
                if (nameAttribute == null) {
                    nameAttribute = new Attribute(NAME, componentName);
                    elementAttributes.add(0, nameAttribute);
                }
                else {
                    nameAttribute.setValue(componentName);
                    if (elementAttributes.get(0) != nameAttribute) {
                        elementAttributes.remove(nameAttribute);
                        elementAttributes.add(0, nameAttribute);
                    }
                }
            }

            rootElement.addContent(element);
        }
        return rootElement;
    }

    
    public Element getState( String name) {
        return myStates.getState(name);
    }

    
    public Element getStateAndArchive( String name) {
        return myStates.getStateAndArchive(name);
    }

    
    static StorageData setStateAndCloneIfNeed( String componentName,  Element newState,  StorageData storageData,  Map<String, Element> newLiveStates) {
        Object oldState = storageData.myStates.get(componentName);
        if (newState == null || JDOMUtil.isEmpty(newState)) {
            if (oldState == null) {
                return null;
            }

            StorageData newStorageData = storageData.clone();
            newStorageData.myStates.remove(componentName);
            return newStorageData;
        }

        prepareElement(newState);

        newLiveStates.put(componentName, newState);

        byte[] newBytes = null;
        if (oldState instanceof Element) {
            if (JDOMUtil.areElementsEqual((Element)oldState, newState)) {
                return null;
            }
        }
        else if (oldState != null) {
            newBytes = getNewByteIfDiffers(componentName, newState, (byte[])oldState);
            if (newBytes == null) {
                return null;
            }
        }

        StorageData newStorageData = storageData.clone();
        newStorageData.myStates.put(componentName, newBytes == null ? newState : newBytes);
        return newStorageData;
    }

    
    final Object setState( String componentName,  Element newState,  Map<String, Element> newLiveStates) {
        if (newState == null || JDOMUtil.isEmpty(newState)) {
            return myStates.remove(componentName);
        }

        prepareElement(newState);

        newLiveStates.put(componentName, newState);

        Object oldState = myStates.get(componentName);

        byte[] newBytes = null;
        if (oldState instanceof Element) {
            if (JDOMUtil.areElementsEqual((Element)oldState, newState)) {
                return null;
            }
        }
        else if (oldState != null) {
            newBytes = getNewByteIfDiffers(componentName, newState, (byte[])oldState);
            if (newBytes == null) {
                return null;
            }
        }

        myStates.put(componentName, newBytes == null ? newState : newBytes);
        return newState;
    }

    private static void prepareElement( Element state) {
        if (state.getParent() != null) {
            LOG.warn("State element must not have parent " + JDOMUtil.writeElement(state));
            state.detach();
        }
        state.setName(COMPONENT);
    }

    @Override
    public StorageData clone() {
        return new StorageData(this);
    }

    // newStorageData - myStates contains only live (unarchived) states
    public Set<String> getChangedComponentNames( StorageData newStorageData,  PathMacroSubstitutor substitutor) {
        Set<String> bothStates = new SmartHashSet<String>(myStates.keys());
        bothStates.retainAll(newStorageData.myStates.keys());

        Set<String> diffs = new SmartHashSet<String>();
        diffs.addAll(newStorageData.myStates.keys());
        diffs.addAll(myStates.keys());
        diffs.removeAll(bothStates);

        for (String componentName : bothStates) {
            myStates.compare(componentName, newStorageData.myStates, diffs);
        }
        return diffs;
    }

    @Override
    public boolean hasState( String componentName) {
        return myStates.hasState(componentName);
    }
}
