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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.util.pico.ConstructorInjectionComponentAdapter;
import org.picocontainer.PicoContainer;

/**
 * @author peter
 */
public abstract class AbstractExtensionPointBean implements PluginAware {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.extensions.AbstractExtensionPointBean");
    protected PluginDescriptor myPluginDescriptor;

    @Override
    public final void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
        myPluginDescriptor = pluginDescriptor;
    }

    public PluginDescriptor getPluginDescriptor() {
        return myPluginDescriptor;
    }

    
    public final <T> Class<T> findClass(final String className) throws ClassNotFoundException {
        return (Class<T>)Class.forName(className, true, getLoaderForClass());
    }

    
    public final <T> Class<T> findClassNoExceptions(final String className) {
        try {
            return findClass(className);
        }
        catch (ClassNotFoundException e) {
            LOG.error("Problem loading class " + className + " from plugin " + myPluginDescriptor.getPluginId().getIdString(), e);
            return null;
        }
    }

    
    public ClassLoader getLoaderForClass() {
        return myPluginDescriptor == null ? getClass().getClassLoader() : myPluginDescriptor.getPluginClassLoader();
    }

    
    public final <T> T instantiate(final String className,  final PicoContainer container) throws ClassNotFoundException {
        return instantiate(this.<T>findClass(className), container);
    }

    
    public static <T> T instantiate( final Class<T> aClass,  final PicoContainer container) {
        return instantiate(aClass, container, false);
    }

    
    public static <T> T instantiate( final Class<T> aClass,
                                     final PicoContainer container,
                                    final boolean allowNonPublicClasses) {
        return (T)new ConstructorInjectionComponentAdapter(aClass.getName(), aClass, null, allowNonPublicClasses).getComponentInstance(container);
    }

}
