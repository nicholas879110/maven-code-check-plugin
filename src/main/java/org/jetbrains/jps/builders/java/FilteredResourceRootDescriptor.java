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
package org.jetbrains.jps.builders.java;


import org.jetbrains.jps.incremental.ResourcesTarget;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.compiler.JpsCompilerExcludes;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

/**
* @author Eugene Zhuravlev
*         Date: 1/3/12
*/
public final class FilteredResourceRootDescriptor extends ResourceRootDescriptor {
  public FilteredResourceRootDescriptor( File root,  ResourcesTarget target,  String packagePrefix,
                                         Set<File> excludes) {
    super(root, target, packagePrefix, excludes);
  }


  @Override
  public FileFilter createFileFilter() {
    final JpsProject project = getTarget().getModule().getProject();
    final JpsJavaCompilerConfiguration configuration = JpsJavaExtensionService.getInstance().getOrCreateCompilerConfiguration(project);
    final JpsCompilerExcludes excludes = configuration.getCompilerExcludes();
    return new FileFilter() {
      @Override
      public boolean accept(File file) {
        return !excludes.isExcluded(file) && configuration.isResourceFile(file, getRootFile());
      }
    };
  }
}
