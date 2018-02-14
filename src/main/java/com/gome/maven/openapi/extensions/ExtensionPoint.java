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


import com.gome.maven.openapi.Disposable;

/**
 * @author AKireyev
 */
public interface ExtensionPoint<T> {
    
    String getName();
    AreaInstance getArea();

    /**
     * @deprecated use {@link #getClassName()} instead
     */
    
    String getBeanClassName();

    void registerExtension( T extension);
    void registerExtension( T extension,  LoadingOrder order);

    
    T[] getExtensions();
    boolean hasAnyExtensions();


    T getExtension();
    boolean hasExtension( T extension);

    void unregisterExtension( T extension);

    void addExtensionPointListener( ExtensionPointListener<T> listener,  Disposable parentDisposable);
    void addExtensionPointListener( ExtensionPointListener<T> listener);
    void removeExtensionPointListener( ExtensionPointListener<T> extensionPointListener);

    void reset();

    
    Class<T> getExtensionClass();

    
    Kind getKind();

    
    String getClassName();

    enum Kind {INTERFACE, BEAN_CLASS}
}
