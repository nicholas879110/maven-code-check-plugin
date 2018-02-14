/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.jps.model.serialization;

import org.jdom.Element;


import org.jetbrains.jps.model.JpsCompositeElement;
import org.jetbrains.jps.model.JpsElementReference;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.artifact.JpsArtifactExtensionSerializer;
import org.jetbrains.jps.model.serialization.artifact.JpsArtifactPropertiesSerializer;
import org.jetbrains.jps.model.serialization.artifact.JpsPackagingElementSerializer;
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer;
import org.jetbrains.jps.model.serialization.library.JpsLibraryPropertiesSerializer;
import org.jetbrains.jps.model.serialization.library.JpsLibraryRootTypeSerializer;
import org.jetbrains.jps.model.serialization.library.JpsSdkPropertiesSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModuleClasspathSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer;
import org.jetbrains.jps.model.serialization.runConfigurations.JpsRunConfigurationPropertiesSerializer;
import org.jetbrains.jps.service.JpsServiceManager;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class JpsModelSerializerExtension {
  public static Iterable<JpsModelSerializerExtension> getExtensions() {
    return JpsServiceManager.getInstance().getExtensions(JpsModelSerializerExtension.class);
  }

  public void loadRootModel( JpsModule module,  Element rootModel) {
  }

  public void saveRootModel( JpsModule module,  Element rootModel) {
  }

  public void loadModuleOptions( JpsModule module,  Element rootElement) {
  }

  public void saveModuleOptions( JpsModule module,  Element rootElement) {
  }

  public List<JpsLibraryRootTypeSerializer> getLibraryRootTypeSerializers() {
    return Collections.emptyList();
  }


  public List<JpsLibraryRootTypeSerializer> getSdkRootTypeSerializers() {
    return Collections.emptyList();
  }

  public void loadModuleDependencyProperties(JpsDependencyElement dependency, Element orderEntry) {
  }

  public void saveModuleDependencyProperties(JpsDependencyElement dependency, Element orderEntry) {
  }


  public JpsElementReference<? extends JpsCompositeElement> createLibraryTableReference(String tableLevel) {
    return null;
  }


  public String getLibraryTableLevelId(JpsElementReference<? extends JpsCompositeElement> reference) {
    return null;
  }


  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsGlobalExtensionSerializer> getGlobalExtensionSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsModulePropertiesSerializer<?>> getModulePropertiesSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsModuleSourceRootPropertiesSerializer<?>> getModuleSourceRootPropertiesSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsLibraryPropertiesSerializer<?>> getLibraryPropertiesSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsSdkPropertiesSerializer<?>> getSdkPropertiesSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsFacetConfigurationSerializer<?>> getFacetConfigurationSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsPackagingElementSerializer<?>> getPackagingElementSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsArtifactPropertiesSerializer<?>> getArtifactTypePropertiesSerializers() {
    return Collections.emptyList();
  }


  public List<? extends JpsArtifactExtensionSerializer<?>> getArtifactExtensionSerializers() {
    return Collections.emptyList();
  }


  public JpsModuleClasspathSerializer getClasspathSerializer() {
    return null;
  }


  public List<? extends JpsRunConfigurationPropertiesSerializer<?>> getRunConfigurationPropertiesSerializers() {
    return Collections.emptyList();
  }
}
