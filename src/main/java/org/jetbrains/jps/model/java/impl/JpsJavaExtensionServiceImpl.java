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
package org.jetbrains.jps.model.java.impl;

import com.gome.maven.openapi.util.io.FileUtil;


import org.jetbrains.jps.model.java.impl.runConfiguration.JpsApplicationRunConfigurationPropertiesImpl;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationProperties;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationState;
import org.jetbrains.jps.util.JpsPathUtil;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;
import org.jetbrains.jps.model.java.impl.compiler.JpsJavaCompilerConfigurationImpl;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JpsJavaExtensionServiceImpl extends JpsJavaExtensionService {

  @Override
  public JpsJavaProjectExtension getOrCreateProjectExtension( JpsProject project) {
    return project.getContainer().getOrSetChild(JavaProjectExtensionRole.INSTANCE);
  }


  @Override
  public JpsJavaProjectExtension getProjectExtension( JpsProject project) {
    return project.getContainer().getChild(JavaProjectExtensionRole.INSTANCE);
  }


  @Override
  public JpsJavaModuleExtension getOrCreateModuleExtension( JpsModule module) {
    return module.getContainer().getOrSetChild(JavaModuleExtensionRole.INSTANCE);
  }


  @Override
  public JpsJavaDependencyExtension getOrCreateDependencyExtension( JpsDependencyElement dependency) {
    return dependency.getContainer().getOrSetChild(JpsJavaDependencyExtensionRole.INSTANCE);
  }

  @Override
  public JpsJavaDependencyExtension getDependencyExtension( JpsDependencyElement dependency) {
    return dependency.getContainer().getChild(JpsJavaDependencyExtensionRole.INSTANCE);
  }

  @Override

  public JpsJavaModuleExtension getModuleExtension( JpsModule module) {
    return module.getContainer().getChild(JavaModuleExtensionRole.INSTANCE);
  }

  @Override

  public ExplodedDirectoryModuleExtension getOrCreateExplodedDirectoryExtension( JpsModule module) {
    return module.getContainer().getOrSetChild(ExplodedDirectoryModuleExtensionImpl.ExplodedDirectoryModuleExtensionRole.INSTANCE);
  }

  @Override

  public ExplodedDirectoryModuleExtension getExplodedDirectoryExtension( JpsModule module) {
    return module.getContainer().getChild(ExplodedDirectoryModuleExtensionImpl.ExplodedDirectoryModuleExtensionRole.INSTANCE);
  }


  @Override
  public List<JpsDependencyElement> getDependencies(JpsModule module, JpsJavaClasspathKind classpathKind, boolean exportedOnly) {
    final List<JpsDependencyElement> result = new ArrayList<JpsDependencyElement>();
    for (JpsDependencyElement dependencyElement : module.getDependenciesList().getDependencies()) {
      final JpsJavaDependencyExtension extension = getDependencyExtension(dependencyElement);
      if (extension == null || extension.getScope().isIncludedIn(classpathKind) && (!exportedOnly || extension.isExported())) {
        result.add(dependencyElement);
      }
    }
    return result;
  }

  @Override
  public LanguageLevel getLanguageLevel(JpsModule module) {
    final JpsJavaModuleExtension moduleExtension = getModuleExtension(module);
    if (moduleExtension == null) return null;
    final LanguageLevel languageLevel = moduleExtension.getLanguageLevel();
    if (languageLevel != null) return languageLevel;
    final JpsJavaProjectExtension projectExtension = getProjectExtension(module.getProject());
    return projectExtension != null ? projectExtension.getLanguageLevel() : null;
  }

  @Override
  public String getOutputUrl(JpsModule module, boolean forTests) {
    final JpsJavaModuleExtension extension = getModuleExtension(module);
    if (extension == null) return null;
    if (extension.isInheritOutput()) {
      JpsJavaProjectExtension projectExtension = getProjectExtension(module.getProject());
      if (projectExtension == null) return null;
      final String url = projectExtension.getOutputUrl();
      if (url == null) return null;
      return url + "/" + (forTests ? "test" : "production") + "/" + module.getName();
    }
    return forTests ? extension.getTestOutputUrl() : extension.getOutputUrl();
  }


  @Override
  public File getOutputDirectory(JpsModule module, boolean forTests) {
    String url = getOutputUrl(module, forTests);
    return url != null ? JpsPathUtil.urlToFile(url) : null;
  }

  @Override
  public JpsTypedLibrary<JpsSdk<JpsDummyElement>> addJavaSdk( JpsGlobal global,  String name,  String homePath) {
    String version = JdkVersionDetector.getInstance().detectJdkVersion(homePath);
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> sdk = global.addSdk(name, homePath, version, JpsJavaSdkType.INSTANCE);
    File homeDir = new File(FileUtil.toSystemDependentName(homePath));
    List<File> roots = JavaSdkUtil.getJdkClassesRoots(homeDir, false);
    for (File root : roots) {
      sdk.addRoot(root, JpsOrderRootType.COMPILED);
    }
    return sdk;
  }


  @Override
  public JpsJavaCompilerConfiguration getCompilerConfiguration( JpsProject project) {
    return project.getContainer().getChild(JpsJavaCompilerConfigurationImpl.ROLE);
  }


  @Override
  public JpsJavaCompilerConfiguration getOrCreateCompilerConfiguration( JpsProject project) {
    JpsJavaCompilerConfiguration configuration = getCompilerConfiguration(project);
    if (configuration == null) {
      configuration = project.getContainer().setChild(JpsJavaCompilerConfigurationImpl.ROLE, new JpsJavaCompilerConfigurationImpl());
    }
    return configuration;
  }


  @Override
  public JpsSdkReference<JpsDummyElement> createWrappedJavaSdkReference( JpsJavaSdkTypeWrapper sdkType,
                                                                         JpsSdkReference<?> wrapperReference) {
    return new JpsWrappedJavaSdkReferenceImpl(sdkType, wrapperReference);
  }


  @Override
  public JpsApplicationRunConfigurationProperties createRunConfigurationProperties(JpsApplicationRunConfigurationState state) {
    return new JpsApplicationRunConfigurationPropertiesImpl(state);
  }


  @Override
  public JavaSourceRootProperties createSourceRootProperties( String packagePrefix, boolean isGenerated) {
    return new JavaSourceRootProperties(packagePrefix, isGenerated);
  }


  @Override
  public JavaSourceRootProperties createSourceRootProperties( String packagePrefix) {
    return createSourceRootProperties(packagePrefix, false);
  }


  @Override
  public JavaResourceRootProperties createResourceRootProperties( String relativeOutputPath, boolean forGeneratedResource) {
    return new JavaResourceRootProperties(relativeOutputPath, forGeneratedResource);
  }

  @Override

  public JpsProductionModuleOutputPackagingElement createProductionModuleOutput( JpsModuleReference moduleReference) {
    return new JpsProductionModuleOutputPackagingElementImpl(moduleReference);
  }

  @Override

  public JpsTestModuleOutputPackagingElement createTestModuleOutput( JpsModuleReference moduleReference) {
    return new JpsTestModuleOutputPackagingElementImpl(moduleReference);
  }

  @Override
  public JpsJavaDependenciesEnumerator enumerateDependencies(Collection<JpsModule> modules) {
    return new JpsJavaDependenciesEnumeratorImpl(modules);
  }

  @Override
  protected JpsJavaDependenciesEnumerator enumerateDependencies(JpsProject project) {
    return new JpsJavaDependenciesEnumeratorImpl(project.getModules());
  }

  @Override
  protected JpsJavaDependenciesEnumerator enumerateDependencies(JpsModule module) {
    return new JpsJavaDependenciesEnumeratorImpl(Collections.singletonList(module));
  }
}
