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
package com.gome.maven.usageView;

import com.gome.maven.openapi.components.NamedComponent;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.colors.EditorColorsUtil;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.util.ui.UIUtil;
import org.jdom.Element;

public class UsageTreeColorsScheme implements NamedComponent, JDOMExternalizable {
    private EditorColorsScheme myColorsScheme;

    /**
     * @noinspection UnusedParameters
     */
    public UsageTreeColorsScheme(EditorColorsManager editorColorsManager) {
    }

    public static UsageTreeColorsScheme getInstance() {
        return ServiceManager.getService(UsageTreeColorsScheme.class);
    }

    @Override
    
    public String getComponentName() {
        return "FindViewColorsScheme";
    }

    public EditorColorsScheme getScheme() {
        return myColorsScheme;
    }

    @Override
    public void readExternal(Element element) {
        if (myColorsScheme == null) {
            myColorsScheme = (EditorColorsScheme)EditorColorsUtil.getColorSchemeForBackground(UIUtil.getTreeTextBackground()).clone();
        }
        myColorsScheme.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        throw new WriteExternalException();
    }
}
