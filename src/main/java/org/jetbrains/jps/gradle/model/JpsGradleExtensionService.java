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
package org.jetbrains.jps.gradle.model;



import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gradle.model.impl.GradleProjectConfiguration;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author Vladislav.Soroka
 * @since 7/10/2014
 */
public abstract class JpsGradleExtensionService {
  public static JpsGradleExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsGradleExtensionService.class);
  }


  public abstract JpsGradleModuleExtension getExtension( JpsModule module);


  public abstract JpsGradleModuleExtension getOrCreateExtension( JpsModule module);

  public abstract void setProductionOnTestDependency( JpsDependencyElement dependency, boolean value);

  public abstract boolean isProductionOnTestDependency( JpsDependencyElement dependency);

  public abstract boolean hasGradleProjectConfiguration( BuildDataPaths paths);


  public abstract GradleProjectConfiguration getGradleProjectConfiguration(BuildDataPaths paths);
}
