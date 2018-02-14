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
package org.jetbrains.jps.model.module;



import org.jetbrains.jps.model.JpsElement;

import java.io.File;
import java.util.Set;

/**
 * @author nik
 */
public interface JpsModuleSourceRoot extends JpsElement {

  JpsModuleSourceRootType<?> getRootType();

  /**
   * @return the root properties element or {@code null} if the root type doesn't equal to {@code type}
   */

  <P extends JpsElement> P getProperties( JpsModuleSourceRootType<P> type);

  /**
   * @return the root properties element or {@code null} if the root type isn't contained in {@code types}
   */

  <P extends JpsElement> P getProperties( Set<? extends JpsModuleSourceRootType<P>> types);


  <P extends JpsElement> JpsTypedModuleSourceRoot<P> asTyped( JpsModuleSourceRootType<P> type);


  JpsTypedModuleSourceRoot<?> asTyped();


  JpsElement getProperties();


  String getUrl();


  File getFile();
}
