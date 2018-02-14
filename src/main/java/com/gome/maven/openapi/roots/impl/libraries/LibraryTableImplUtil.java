/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.impl.libraries;

import com.gome.maven.openapi.roots.impl.RootModelImpl;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.PersistentLibraryKind;
import com.gome.maven.openapi.util.InvalidDataException;
import org.jdom.Element;

import java.util.List;

/**
 *  @author dsl
 */
public class LibraryTableImplUtil {
     public static final String MODULE_LEVEL = "module";

    private LibraryTableImplUtil() {
    }

    public static Library loadLibrary(Element rootElement, RootModelImpl rootModel) throws InvalidDataException {
        final List children = rootElement.getChildren(LibraryImpl.ELEMENT);
        if (children.size() != 1) throw new InvalidDataException();
        Element element = (Element)children.get(0);
        return new LibraryImpl(null, element, rootModel);
    }

    public static Library createModuleLevelLibrary( String name,
                                                   final PersistentLibraryKind kind,
                                                   RootModelImpl rootModel) {
        return new LibraryImpl(name, kind, null, rootModel);
    }
}
