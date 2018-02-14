/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.ide;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.codeStyle.NameUtil;

import javax.swing.*;

/**
 * @author peter
 */
public abstract class TypePresentationService {

    public static TypePresentationService getService() {
        return ServiceManager.getService(TypePresentationService.class);
    }

    
    public abstract Icon getIcon(Object o);

    
    public abstract Icon getTypeIcon(Class type);

    
    public abstract String getTypePresentableName(Class type);

    
    public abstract String getTypeName(Object o);


    public static String getDefaultTypeName(final Class aClass) {
        String simpleName = aClass.getSimpleName();
        final int i = simpleName.indexOf('$');
        if (i >= 0) {
            simpleName = simpleName.substring(i + 1);
        }
        return StringUtil.capitalizeWords(StringUtil.join(NameUtil.nameToWords(simpleName), " "), true);
    }
}
