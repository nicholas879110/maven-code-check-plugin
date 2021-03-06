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
package com.gome.maven.openapi.extensions;


/**
 * @author AKireyev
 */
public class EPAvailabilityListenerExtension implements PluginAware {
    public static final String EXTENSION_POINT_NAME = "com.gome.maven.openapi.extensions.epAvailabilityListener";

    private String myExtensionPointName;
    private String myListenerClass;
    private PluginDescriptor myPluginDescriptor;

    public EPAvailabilityListenerExtension() {
    }

    public EPAvailabilityListenerExtension( String extensionPointName,  String listenerClass) {
        myExtensionPointName = extensionPointName;
        myListenerClass = listenerClass;
    }

    
    public String getExtensionPointName() {
        return myExtensionPointName;
    }

    public void setExtensionPointName( String extensionPointName) {
        myExtensionPointName = extensionPointName;
    }

    
    public String getListenerClass() {
        return myListenerClass;
    }

    public void setListenerClass( String listenerClass) {
        myListenerClass = listenerClass;
    }

    @Override
    public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
        myPluginDescriptor = pluginDescriptor;
    }

    public PluginDescriptor getPluginDescriptor() {
        return myPluginDescriptor;
    }

    public Class loadListenerClass() throws ClassNotFoundException {
        if (myPluginDescriptor != null && myPluginDescriptor.getPluginClassLoader() != null) {
            return Class.forName(getListenerClass(), true, myPluginDescriptor.getPluginClassLoader());
        }
        else {
            return Class.forName(getListenerClass());
        }
    }
}
