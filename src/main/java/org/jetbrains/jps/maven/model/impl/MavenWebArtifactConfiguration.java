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
package org.jetbrains.jps.maven.model.impl;

import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.util.xmlb.annotations.AbstractCollection;
import com.gome.maven.util.xmlb.annotations.Tag;
import com.gome.maven.util.xmlb.annotations.Transient;
import gnu.trove.THashMap;



import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class MavenWebArtifactConfiguration {
  @Tag("module-name")
  public String moduleName;

  @Tag("web-resources")
  @AbstractCollection(surroundWithTag = false, elementTag = "resource")
  public List<ResourceRootConfiguration> webResources = new ArrayList<ResourceRootConfiguration>();

  @Transient
  private volatile Map<File, ResourceRootConfiguration> myResourceRootsMap;


  public ResourceRootConfiguration getRootConfiguration( File root) {
    if (myResourceRootsMap == null) {
      Map<File, ResourceRootConfiguration> map = new THashMap<File, ResourceRootConfiguration>(FileUtil.FILE_HASHING_STRATEGY);
      for (ResourceRootConfiguration resource : webResources) {
        map.put(new File(resource.directory), resource);
      }
      myResourceRootsMap = map;
    }
    return myResourceRootsMap.get(root);
  }
}
