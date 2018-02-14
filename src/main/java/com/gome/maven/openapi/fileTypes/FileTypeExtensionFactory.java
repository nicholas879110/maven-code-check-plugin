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

/*
 * @author max
 */
package com.gome.maven.openapi.fileTypes;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.KeyedFactoryEPBean;
import com.gome.maven.openapi.util.KeyedExtensionFactory;

public class FileTypeExtensionFactory<T> extends KeyedExtensionFactory<T, FileType> {
    public FileTypeExtensionFactory( final Class<T> interfaceClass,   final ExtensionPointName<KeyedFactoryEPBean> epName) {
        super(interfaceClass, epName, ApplicationManager.getApplication().getPicoContainer());
    }

    @Override
    public String getKey( final FileType key) {
        return key.getName();
    }
}