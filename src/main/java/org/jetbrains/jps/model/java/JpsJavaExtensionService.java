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
package org.jetbrains.jps.model.java;



import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationProperties;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationState;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;
import org.jetbrains.jps.service.JpsServiceManager;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public abstract class JpsJavaExtensionService {
  public static JpsJavaExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsJavaExtensionService.class);
  }

  public static JpsJavaDependenciesEnumerator dependencies(JpsModule module) {
    return getInstance().enumerateDependencies(module);
  }

  public static JpsJavaDependenciesEnumerator dependencies(JpsProject project) {
    return getInstance().enumerateDependencies(project);
  }


  public abstract JpsProductionModuleOutputPackagingElement createProductionModuleOutput( JpsModuleReference moduleReference);


  public abstract JpsTestModuleOutputPackagingElement createTestModuleOutput( JpsModuleReference moduleReference);

  public abstract JpsJavaDependenciesEnumerator enumerateDependencies(Collection<JpsModule> modules);

  protected abstract JpsJavaDependenciesEnumerator enumerateDependencies(JpsProject project);

  protected abstract JpsJavaDependenciesEnumerator enumerateDependencies(JpsModule module);


  public abstract JpsJavaProjectExtension getOrCreateProjectExtension( JpsProject project);


  public abstract JpsJavaProjectExtension getProjectExtension( JpsProject project);



  public abstract JpsJavaModuleExtension getOrCreateModuleExtension( JpsModule module);


  public abstract JpsJavaModuleExtension getModuleExtension( JpsModule module);


  public abstract JpsJavaDependencyExtension getOrCreateDependencyExtension( JpsDependencyElement dependency);


  public abstract JpsJavaDependencyExtension getDependencyExtension( JpsDependencyElement dependency);


  public abstract ExplodedDirectoryModuleExtension getExplodedDirectoryExtension( JpsModule module);


  public abstract ExplodedDirectoryModuleExtension getOrCreateExplodedDirectoryExtension( JpsModule module);


  public abstract List<JpsDependencyElement> getDependencies(JpsModule module, JpsJavaClasspathKind classpathKind, boolean exportedOnly);


  public abstract LanguageLevel getLanguageLevel(JpsModule module);


  public abstract String getOutputUrl(JpsModule module, boolean forTests);


  public abstract File getOutputDirectory(JpsModule module, boolean forTests);

  public abstract JpsTypedLibrary<JpsSdk<JpsDummyElement>> addJavaSdk( JpsGlobal global,  String name,
                                                                       String homePath);


  public abstract JpsJavaCompilerConfiguration getCompilerConfiguration( JpsProject project);


  public abstract JpsJavaCompilerConfiguration getOrCreateCompilerConfiguration( JpsProject project);


  public abstract JpsSdkReference<JpsDummyElement> createWrappedJavaSdkReference( JpsJavaSdkTypeWrapper sdkType,
                                                                                  JpsSdkReference<?> wrapperReference);


  public abstract JpsApplicationRunConfigurationProperties createRunConfigurationProperties(JpsApplicationRunConfigurationState state);


  public abstract JavaSourceRootProperties createSourceRootProperties( String packagePrefix, boolean isGenerated);


  public abstract JavaSourceRootProperties createSourceRootProperties( String packagePrefix);


  public abstract JavaResourceRootProperties createResourceRootProperties( String relativeOutputPath, boolean forGeneratedResource);
}
