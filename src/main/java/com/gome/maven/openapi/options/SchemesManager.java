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

import com.gome.maven.util.ThrowableConvertor;
import org.jdom.Element;

import java.io.File;
import java.util.Collection;
import java.util.List;

public abstract class SchemesManager<T extends Scheme, E extends ExternalizableScheme> {
    
    public abstract Collection<E> loadSchemes();

    public abstract void addNewScheme( T scheme, final boolean replaceExisting);

    public abstract void clearAllSchemes();

    
    public abstract List<T> getAllSchemes();

    
    public abstract T findSchemeByName( String schemeName);

    public abstract void save();

    public abstract void setCurrentSchemeName( String schemeName);

    
    public abstract T getCurrentScheme();

    public abstract void removeScheme( T scheme);

    
    public abstract Collection<String> getAllSchemeNames();

    public abstract File getRootDirectory();

    public void loadBundledScheme( String resourceName,  Object requestor,  ThrowableConvertor<Element, T, Throwable> convertor) {
    }
}
