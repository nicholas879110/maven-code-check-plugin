/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.editor.colors.ex;

import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.colors.impl.DefaultColorsScheme;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "DefaultColorSchemesManager",
        defaultStateAsResource = true,
        storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")
)
public class DefaultColorSchemesManager implements PersistentStateComponent<Element> {
    private final List<DefaultColorsScheme> mySchemes;
     private static final String SCHEME_ELEMENT = "scheme";

    public DefaultColorSchemesManager() {
        mySchemes = new ArrayList<DefaultColorsScheme>();
    }

    public static DefaultColorSchemesManager getInstance() {
        return ServiceManager.getService(DefaultColorSchemesManager.class);
    }

    
    @Override
    public Element getState() {
        return null;
    }

    @Override
    public void loadState(Element state) {
        for (Element schemeElement : (List<Element>)state.getChildren(SCHEME_ELEMENT)) {
            DefaultColorsScheme newScheme = new DefaultColorsScheme();
            newScheme.readExternal(schemeElement);
            mySchemes.add(newScheme);
        }
    }

    public DefaultColorsScheme[] getAllSchemes() {
        return mySchemes.toArray(new DefaultColorsScheme[mySchemes.size()]);
    }

    
    public EditorColorsScheme getScheme(String name) {
        for (DefaultColorsScheme scheme : mySchemes) {
            if (name.equals(scheme.getName())) return scheme;
        }
        return null;
    }
}
