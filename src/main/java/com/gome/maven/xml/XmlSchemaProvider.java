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

package com.gome.maven.xml;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlSchemaProvider {
    public static final ExtensionPointName<XmlSchemaProvider> EP_NAME = new ExtensionPointName<XmlSchemaProvider>("com.gome.maven.xml.schemaProvider");

    
    public static XmlFile findSchema(  String namespace,  Module module,  PsiFile file) {
        if (file.getProject().isDefault()) return null;
        final boolean dumb = DumbService.getInstance(file.getProject()).isDumb();
        for (XmlSchemaProvider provider: Extensions.getExtensions(EP_NAME)) {
            if (dumb && !DumbService.isDumbAware(provider)) {
                continue;
            }

            if (file instanceof XmlFile && !provider.isAvailable((XmlFile)file)) {
                continue;
            }
            final XmlFile schema = provider.getSchema(namespace, module, file);
            if (schema != null) {
                return schema;
            }
        }
        return null;
    }

    
    public static XmlFile findSchema(  String namespace,  PsiFile baseFile) {
        final PsiDirectory directory = baseFile.getParent();
        final Module module = ModuleUtilCore.findModuleForPsiElement(directory == null ? baseFile : directory);
        return findSchema(namespace, module, baseFile);
    }

    /**
     * @see #getAvailableProviders(com.gome.maven.psi.xml.XmlFile)
     */
    @Deprecated
    
    public static XmlSchemaProvider getAvailableProvider( final XmlFile file) {
        for (XmlSchemaProvider provider: Extensions.getExtensions(EP_NAME)) {
            if (provider.isAvailable(file)) {
                return provider;
            }
        }
        return null;
    }

    public static List<XmlSchemaProvider> getAvailableProviders( final XmlFile file) {
        return ContainerUtil.findAll(Extensions.getExtensions(EP_NAME), new Condition<XmlSchemaProvider>() {
            @Override
            public boolean value(XmlSchemaProvider xmlSchemaProvider) {
                return xmlSchemaProvider.isAvailable(file);
            }
        });
    }

    
    public abstract XmlFile getSchema(  String url,  Module module,  final PsiFile baseFile);


    public boolean isAvailable( final XmlFile file) {
        return false;
    }

    /**
     * Provides specific namespaces for given xml file.
     * @param file an xml or jsp file.
     * @param tagName optional
     * @return available namespace uris, or <code>null</code> if the provider did not recognize the file.
     */
    
    public Set<String> getAvailableNamespaces( final XmlFile file,  final String tagName) {
        return Collections.emptySet();
    }

    
    public String getDefaultPrefix(  String namespace,  final XmlFile context) {
        return null;
    }

    
    public Set<String> getLocations(  String namespace,  final XmlFile context) {
        return null;
    }
}
