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
package org.jetbrains.jps.builders.java;

import com.gome.maven.openapi.util.io.FileUtil;

import org.jetbrains.jps.builders.BuildRootDescriptor;
import org.jetbrains.jps.incremental.ResourcesTarget;

import java.io.File;
import java.util.Set;

/**
 * @author nik
 */
public class ResourceRootDescriptor extends BuildRootDescriptor {
   private final File myRoot;
   private final ResourcesTarget myTarget;
   private final String myPackagePrefix;
   private final Set<File> myExcludes;

  public ResourceRootDescriptor( File root,  ResourcesTarget target,  String packagePrefix,  Set<File> excludes) {
    myPackagePrefix = packagePrefix;
    myRoot = root;
    myTarget = target;
    myExcludes = excludes;
  }

  @Override
  public File getRootFile() {
    return myRoot;
  }


  @Override
  public Set<File> getExcludedRoots() {
    return myExcludes;
  }


  @Override
  public ResourcesTarget getTarget() {
    return myTarget;
  }


  public String getPackagePrefix() {
    return myPackagePrefix;
  }

  @Override
  public boolean isGenerated() {
    return false;
  }

  @Override
  public String toString() {
    return "ResourceRootDescriptor{target='" + myTarget + '\'' + ", root=" + myRoot + '}';
  }

  @Override
  public boolean canUseFileCache() {
    return true;
  }

  @Override
  public String getRootId() {
    return FileUtil.toSystemIndependentName(myRoot.getPath());
  }
}
