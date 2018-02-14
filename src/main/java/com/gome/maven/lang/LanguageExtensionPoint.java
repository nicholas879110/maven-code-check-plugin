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
package com.gome.maven.lang;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.CustomLoadingExtensionPointBean;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.util.KeyedLazyInstance;
import com.gome.maven.util.xmlb.annotations.Attribute;

/**
 * @author yole
 */
public class LanguageExtensionPoint<T> extends CustomLoadingExtensionPointBean implements KeyedLazyInstance<T> {

    // these must be public for scrambling compatibility
    @Attribute("language")
    public String language;

    @Attribute("implementationClass")
    public String implementationClass;

    private final NotNullLazyValue<T> myHandler = new NotNullLazyValue<T>() {
        @Override
        
        protected T compute() {
            try {
                return (T)instantiateExtension(implementationClass, ApplicationManager.getApplication().getPicoContainer());
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    
    @Override
    public T getInstance() {
        return myHandler.getValue();
    }

    @Override
    public String getKey() {
        return language;
    }
}
