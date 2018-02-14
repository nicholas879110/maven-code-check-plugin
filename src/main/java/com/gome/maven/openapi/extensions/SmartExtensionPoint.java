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

import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author peter
 */
public abstract class SmartExtensionPoint<Extension,V> implements ExtensionPointAndAreaListener<Extension> {
    private final Collection<V> myExplicitExtensions;
    private ExtensionPoint<Extension> myExtensionPoint;
    private List<V> myCache;

    protected SmartExtensionPoint( final Collection<V> explicitExtensions) {
        myExplicitExtensions = explicitExtensions;
    }

    
    protected abstract ExtensionPoint<Extension> getExtensionPoint();

    public final void addExplicitExtension( V extension) {
        synchronized (myExplicitExtensions) {
            myExplicitExtensions.add(extension);
            myCache = null;
        }
    }

    public final void removeExplicitExtension( V extension) {
        synchronized (myExplicitExtensions) {
            myExplicitExtensions.remove(extension);
            myCache = null;
        }
    }

    
    protected abstract V getExtension( final Extension extension);

    
    public final List<V> getExtensions() {
        synchronized (myExplicitExtensions) {
            if (myCache == null) {
                myExtensionPoint = getExtensionPoint();
                myExtensionPoint.addExtensionPointListener(this);
                myCache = new ArrayList<V>(myExplicitExtensions);
                myCache.addAll(ContainerUtil.mapNotNull(myExtensionPoint.getExtensions(), new NullableFunction<Extension, V>() {
                    @Override
                    
                    public V fun(final Extension extension) {
                        return getExtension(extension);
                    }
                }));
            }
            return myCache;
        }
    }

    @Override
    public final void extensionAdded( final Extension extension,  final PluginDescriptor pluginDescriptor) {
        dropCache();
    }

    public final void dropCache() {
        synchronized (myExplicitExtensions) {
            if (myCache != null) {
                myCache = null;
                myExtensionPoint.removeExtensionPointListener(this);
                myExtensionPoint = null;
            }
        }
    }

    @Override
    public final void extensionRemoved( final Extension extension,  final PluginDescriptor pluginDescriptor) {
        dropCache();
    }

    @Override
    public void areaReplaced(final ExtensionsArea area) {
        dropCache();
    }
}
