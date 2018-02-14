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

import org.jdom.Element;
import org.picocontainer.PicoContainer;


/**
 * @author AKireyev
 */
public interface ExtensionsArea  {
    void registerExtensionPoint(  String extensionPointName,  String extensionPointBeanClass);
    void registerExtensionPoint(  String extensionPointName,  String extensionPointBeanClass,  ExtensionPoint.Kind kind);
    void registerExtensionPoint( String extensionPointName,  String extensionPointBeanClass,  PluginDescriptor descriptor);
    void unregisterExtensionPoint(  String extensionPointName);

    boolean hasExtensionPoint(  String extensionPointName);
    
    <T> ExtensionPoint<T> getExtensionPoint(  String extensionPointName);

    
    <T> ExtensionPoint<T> getExtensionPoint( ExtensionPointName<T> extensionPointName);

    
    ExtensionPoint[] getExtensionPoints();
    void suspendInteractions();
    void resumeInteractions();

    void killPendingInteractions();

    void addAvailabilityListener( String extensionPointName,  ExtensionPointAvailabilityListener listener);

    
    AreaPicoContainer getPicoContainer();
    void registerExtensionPoint( String pluginName,  Element extensionPointElement);
    void registerExtensionPoint( PluginDescriptor pluginDescriptor,  Element extensionPointElement);
    void registerExtension( String pluginName,  Element extensionElement);

    void registerExtension( PluginDescriptor pluginDescriptor,  Element extensionElement);

    
    PicoContainer getPluginContainer(String pluginName);

    String getAreaClass();
}
