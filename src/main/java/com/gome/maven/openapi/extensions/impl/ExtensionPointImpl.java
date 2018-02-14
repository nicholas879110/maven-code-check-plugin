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
package com.gome.maven.openapi.extensions.impl;


import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.extensions.*;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.StringInterner;
import org.jdom.Element;


import java.lang.reflect.Array;
import java.util.*;

/**
 * @author AKireyev
 */
@SuppressWarnings("SynchronizeOnThis")
public class ExtensionPointImpl<T> implements ExtensionPoint<T> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.extensions.impl.ExtensionPointImpl");

    private final LogProvider myLogger;

    private final AreaInstance myArea;
    private final String myName;
    private final String myClassName;
    private final Kind myKind;

    private final List<T> myExtensions = new ArrayList<T>();
    private volatile T[] myExtensionsCache;

    private final ExtensionsAreaImpl myOwner;
    private final PluginDescriptor myDescriptor;

    private final Set<ExtensionComponentAdapter> myExtensionAdapters = new LinkedHashSet<ExtensionComponentAdapter>();
    private final List<ExtensionPointListener<T>> myEPListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final List<ExtensionComponentAdapter> myLoadedAdapters = new ArrayList<ExtensionComponentAdapter>();

    private Class<T> myExtensionClass;

    private static final StringInterner INTERNER = new StringInterner();

    public ExtensionPointImpl( String name,
                               String className,
                               Kind kind,
                               ExtensionsAreaImpl owner,
                              AreaInstance area,
                               LogProvider logger,
                               PluginDescriptor descriptor) {
        synchronized (INTERNER) {
            myName = INTERNER.intern(name);
        }
        myClassName = className;
        myKind = kind;
        myOwner = owner;
        myArea = area;
        myLogger = logger;
        myDescriptor = descriptor;
    }

    
    @Override
    public String getName() {
        return myName;
    }

    @Override
    public AreaInstance getArea() {
        return myArea;
    }

    
    @Override
    public String getBeanClassName() {
        return myClassName;
    }

    
    @Override
    public String getClassName() {
        return myClassName;
    }

    
    @Override
    public Kind getKind() {
        return myKind;
    }

    @Override
    public void registerExtension( T extension) {
        registerExtension(extension, LoadingOrder.ANY);
    }

    
    public PluginDescriptor getDescriptor() {
        return myDescriptor;
    }

    @Override
    public synchronized void registerExtension( T extension,  LoadingOrder order) {
        assert myExtensions.size() == myLoadedAdapters.size();

        ExtensionComponentAdapter adapter = new ObjectComponentAdapter(extension, order);

        if (LoadingOrder.ANY == order) {
            int index = myLoadedAdapters.size();
            if (index > 0) {
                ExtensionComponentAdapter lastAdapter = myLoadedAdapters.get(index - 1);
                if (lastAdapter.getOrder() == LoadingOrder.LAST) {
                    index--;
                }
            }
            registerExtension(extension, adapter, index, true);
        }
        else {
            registerExtensionAdapter(adapter);
            processAdapters();
        }
    }

    private void registerExtension( T extension,  ExtensionComponentAdapter adapter, int index, boolean runNotifications) {
        if (myExtensions.contains(extension)) {
            myLogger.error("Extension was already added: " + extension);
            return;
        }

        Class<T> extensionClass = getExtensionClass();
        if (!extensionClass.isInstance(extension)) {
            myLogger.error("Extension " + extension.getClass() + " does not implement " + extensionClass);
            return;
        }

        myExtensions.add(index, extension);
        myLoadedAdapters.add(index, adapter);

        if (runNotifications) {
            clearCache();

            if (!adapter.isNotificationSent()) {
                if (extension instanceof Extension) {
                    try {
                        ((Extension)extension).extensionAdded(this);
                    }
                    catch (Throwable e) {
                        myLogger.error(e);
                    }
                }

                notifyListenersOnAdd(extension, adapter.getPluginDescriptor());
                adapter.setNotificationSent(true);
            }
        }
    }

    private void notifyListenersOnAdd( T extension, final PluginDescriptor pluginDescriptor) {
        for (ExtensionPointListener<T> listener : myEPListeners) {
            try {
                listener.extensionAdded(extension, pluginDescriptor);
            }
            catch (Throwable e) {
                myLogger.error(e);
            }
        }
    }

    @Override
    
    public T[] getExtensions() {
        T[] result = myExtensionsCache;
        if (result == null) {
            synchronized (this) {
                result = myExtensionsCache;
                if (result == null) {
                    processAdapters();

                    Class<T> extensionClass = getExtensionClass();
                    @SuppressWarnings("unchecked") T[] a = (T[])Array.newInstance(extensionClass, myExtensions.size());
                    result = myExtensions.toArray(a);

                    for (int i = result.length - 1; i >= 0; i--) {
                        T extension = result[i];
                        if (extension == null) {
                            LOG.error(" null extension: " + myExtensions + ";\n" +
                                    " getExtensionClass(): " + extensionClass + ";\n" );
                        }
                        if (i > 0 && extension == result[i - 1]) {
                            LOG.error("Duplicate extension found: " + extension + "; " +
                                    " Result:      " + Arrays.toString(result) + ";\n" +
                                    " extensions: " + myExtensions + ";\n" +
                                    " getExtensionClass(): " + extensionClass + ";\n" +
                                    " size:" + myExtensions.size() + ";" + result.length);
                        }
                    }

                    myExtensionsCache = result;
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasAnyExtensions() {
        final T[] cache = myExtensionsCache;
        if (cache != null) {
            return cache.length > 0;
        }
        synchronized (this) {
            return myExtensionAdapters.size() + myLoadedAdapters.size() > 0;
        }
    }

    private void processAdapters() {
        int totalSize = myExtensionAdapters.size() + myLoadedAdapters.size();
        if (totalSize != 0) {
            List<ExtensionComponentAdapter> adapters = ContainerUtil.newArrayListWithCapacity(totalSize);
            adapters.addAll(myExtensionAdapters);
            adapters.addAll(myLoadedAdapters);
            LoadingOrder.sort(adapters);
            myExtensionAdapters.clear();
            myExtensionAdapters.addAll(adapters);

            Set<ExtensionComponentAdapter> loaded = ContainerUtil.newHashOrEmptySet(myLoadedAdapters);
            myExtensions.clear();
            myLoadedAdapters.clear();

            for (ExtensionComponentAdapter adapter : adapters) {
                try {
                    @SuppressWarnings("unchecked") T extension = (T)adapter.getExtension();
                    registerExtension(extension, adapter, myExtensions.size(), !loaded.contains(adapter));
                    myExtensionAdapters.remove(adapter);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Exception e) {
                    LOG.error(e);
                    myExtensionAdapters.remove(adapter);
                }
            }
        }
    }

    @Override
   
    public T getExtension() {
        T[] extensions = getExtensions();
        return extensions.length == 0 ? null : extensions[0];
    }

    @Override
    public synchronized boolean hasExtension( T extension) {
        processAdapters();
        return myExtensions.contains(extension);
    }

    @Override
    public synchronized void unregisterExtension( final T extension) {
        final int index = getExtensionIndex(extension);
        final ExtensionComponentAdapter adapter = myLoadedAdapters.get(index);

        Object key = adapter.getComponentKey();
        myOwner.getPicoContainer().unregisterComponent(key);

        processAdapters();
        unregisterExtension(extension, null);
    }

    private int getExtensionIndex( T extension) {
        int i = myExtensions.indexOf(extension);
        if (i == -1) {
            throw new IllegalArgumentException("Extension to be removed not found: " + extension);
        }
        return i;
    }

    private void unregisterExtension( T extension, PluginDescriptor pluginDescriptor) {
        int index = getExtensionIndex(extension);

        myExtensions.remove(index);
        myLoadedAdapters.remove(index);
        clearCache();

        notifyListenersOnRemove(extension, pluginDescriptor);

        if (extension instanceof Extension) {
            try {
                ((Extension)extension).extensionRemoved(this);
            }
            catch (Throwable e) {
                myLogger.error(e);
            }
        }
    }

    private void notifyListenersOnRemove( T extensionObject, PluginDescriptor pluginDescriptor) {
        for (ExtensionPointListener<T> listener : myEPListeners) {
            try {
                listener.extensionRemoved(extensionObject, pluginDescriptor);
            }
            catch (Throwable e) {
                myLogger.error(e);
            }
        }
    }

    @Override
    public void addExtensionPointListener( final ExtensionPointListener<T> listener,  Disposable parentDisposable) {
        addExtensionPointListener(listener, true, parentDisposable);
    }

    public synchronized void addExtensionPointListener( final ExtensionPointListener<T> listener,
                                                       final boolean invokeForLoadedExtensions,
                                                        Disposable parentDisposable) {
        if (invokeForLoadedExtensions) {
            addExtensionPointListener(listener);
        } else {
            myEPListeners.add(listener);
        }
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeExtensionPointListener(listener, invokeForLoadedExtensions);
            }
        });
    }

    @Override
    public synchronized void addExtensionPointListener( ExtensionPointListener<T> listener) {
        processAdapters();
        if (myEPListeners.add(listener)) {
            for (ExtensionComponentAdapter componentAdapter : myLoadedAdapters.toArray(new ExtensionComponentAdapter[myLoadedAdapters.size()])) {
                try {
                    @SuppressWarnings("unchecked") T extension = (T)componentAdapter.getExtension();
                    listener.extensionAdded(extension, componentAdapter.getPluginDescriptor());
                }
                catch (Throwable e) {
                    myLogger.error(e);
                }
            }
        }
    }

    @Override
    public void removeExtensionPointListener( ExtensionPointListener<T> listener) {
        removeExtensionPointListener(listener, true);
    }

    private synchronized void removeExtensionPointListener( ExtensionPointListener<T> listener, boolean invokeForLoadedExtensions) {
        if (myEPListeners.remove(listener) && invokeForLoadedExtensions) {
            for (ExtensionComponentAdapter componentAdapter : myLoadedAdapters.toArray(new ExtensionComponentAdapter[myLoadedAdapters.size()])) {
                try {
                    @SuppressWarnings("unchecked") T extension = (T)componentAdapter.getExtension();
                    listener.extensionRemoved(extension, componentAdapter.getPluginDescriptor());
                }
                catch (Throwable e) {
                    myLogger.error(e);
                }
            }
        }
    }

    @Override
    public synchronized void reset() {
        myOwner.removeAllComponents(myExtensionAdapters);
        myExtensionAdapters.clear();
        for (T extension : getExtensions()) {
            unregisterExtension(extension);
        }
    }

    
    @Override
    public Class<T> getExtensionClass() {
        // racy single-check: we don't care whether the access to 'myExtensionClass' is thread-safe
        // but initial store in a local variable is crucial to prevent instruction reordering
        // see Item 71 in Effective Java or http://jeremymanson.blogspot.com/2008/12/benign-data-races-in-java.html
        Class<T> extensionClass = myExtensionClass;
        if (extensionClass == null) {
            try {
                ClassLoader pluginClassLoader = myDescriptor.getPluginClassLoader();
                @SuppressWarnings("unchecked") Class<T> extClass = pluginClassLoader == null
                        ? (Class<T>)Class.forName(myClassName) : (Class<T>)Class.forName(myClassName, true, pluginClassLoader);
                myExtensionClass = extensionClass = extClass;
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return extensionClass;
    }

    public String toString() {
        return getName();
    }

    synchronized void registerExtensionAdapter( ExtensionComponentAdapter adapter) {
        myExtensionAdapters.add(adapter);
        clearCache();
    }

    private void clearCache() {
        myExtensionsCache = null;
    }

    synchronized boolean unregisterExtensionAdapter( ExtensionComponentAdapter adapter) {
        try {
            if (myExtensionAdapters.remove(adapter)) {
                return true;
            }
            if (myLoadedAdapters.contains(adapter)) {
                Object key = adapter.getComponentKey();
                myOwner.getPicoContainer().unregisterComponent(key);

                @SuppressWarnings("unchecked") T extension = (T)adapter.getExtension();
                unregisterExtension(extension, adapter.getPluginDescriptor());
                return true;
            }
            return false;
        }
        finally {
            clearCache();
        }
    }

    final synchronized void notifyAreaReplaced(final ExtensionsArea area) {
        for (final ExtensionPointListener<T> listener : myEPListeners) {
            if (listener instanceof ExtensionPointAndAreaListener) {
                ((ExtensionPointAndAreaListener)listener).areaReplaced(area);
            }
        }
    }

    private static class ObjectComponentAdapter extends ExtensionComponentAdapter {
        private final Object myExtension;
        private final LoadingOrder myLoadingOrder;

        private ObjectComponentAdapter( Object extension,  LoadingOrder loadingOrder) {
            super(extension.getClass().getName(), null, null, null, false);
            myExtension = extension;
            myLoadingOrder = loadingOrder;
        }

        @Override
        public Object getExtension() {
            return myExtension;
        }

        @Override
        public LoadingOrder getOrder() {
            return myLoadingOrder;
        }

        @Override
       
        public String getOrderId() {
            return null;
        }

        @Override
        public Element getDescribingElement() {
            return new Element("RuntimeExtension: " + myExtension);
        }
    }
}
