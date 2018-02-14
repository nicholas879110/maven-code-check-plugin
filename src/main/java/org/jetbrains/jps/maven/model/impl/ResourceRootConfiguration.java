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
package org.jetbrains.jps.maven.model.impl;

import com.gome.maven.util.xmlb.annotations.Attribute;
import com.gome.maven.util.xmlb.annotations.Tag;



/**
 * @author Eugene Zhuravlev
 *         Date: 10/20/12
 */
@Tag("resource")
public class ResourceRootConfiguration extends FilePattern {
  @Tag("directory")

  public String directory;

  @Tag("targetPath")

  public String targetPath;

  @Attribute("filtered")
  public boolean isFiltered;

  public int computeConfigurationHash() {
    int result = directory.hashCode();
    result = 31 * result + (targetPath != null ? targetPath.hashCode() : 0);
    result = 31 * result + (isFiltered ? 1 : 0);
    //result = 31 * result + includes.hashCode();
    //result = 31 * result + excludes.hashCode();
    return result;
  }
}
