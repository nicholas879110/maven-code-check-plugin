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
package com.gome.maven.openapi.editor.actionSystem;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.AbstractExtensionPointBean;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.util.pico.DefaultPicoContainer;
import com.gome.maven.util.xmlb.annotations.Attribute;

/**
 * @author yole
 */
public class EditorTypedHandlerBean extends AbstractExtensionPointBean {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.editor.actionSystem.EditorActionHandlerBean");

    public static final ExtensionPointName<EditorTypedHandlerBean> EP_NAME = ExtensionPointName.create("com.gome.maven.editorTypedHandler");

    // these must be public for scrambling compatibility
    @Attribute("implementationClass")
    public String implementationClass;

    private TypedActionHandler myHandler;

    public TypedActionHandler getHandler(TypedActionHandler originalHandler) {
        if (myHandler == null) {
            try {
                DefaultPicoContainer container = new DefaultPicoContainer(ApplicationManager.getApplication().getPicoContainer());
                container.registerComponentInstance(originalHandler);
                myHandler = instantiate(implementationClass, container);
            }
            catch(Exception e) {
                LOG.error(e);
                return null;
            }
        }
        return myHandler;
    }
}