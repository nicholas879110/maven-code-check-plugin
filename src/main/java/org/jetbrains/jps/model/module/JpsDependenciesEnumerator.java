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

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.util.Consumer;

import org.jetbrains.jps.model.library.JpsLibrary;

import java.util.Set;

/**
 * Interface for convenient processing dependencies of a module or a project
 * <p/>
 * Use {@link org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator JpsJavaDependenciesEnumerator} for java-specific dependencies processing
 * <p/>
 * Note that all configuration methods modify {@link org.jetbrains.jps.model.module.JpsDependenciesEnumerator} instance instead of creating a new one.
 *
 * @author nik
 */
public interface JpsDependenciesEnumerator {

  JpsDependenciesEnumerator withoutLibraries();

  JpsDependenciesEnumerator withoutDepModules();

  JpsDependenciesEnumerator withoutSdk();

  JpsDependenciesEnumerator withoutModuleSourceEntries();

  /**
   * Recursively process modules on which the module depends
   *
   * @return this instance
   */

  JpsDependenciesEnumerator recursively();

  /**
   * Process only dependencies which satisfies the specified condition
   *
   * @param condition filtering condition
   * @return this instance
   */

  JpsDependenciesEnumerator satisfying( Condition<JpsDependencyElement> condition);

  /**
   * @return all modules processed by enumerator
   */

  Set<JpsModule> getModules();

  /**
   * @return all libraries processed by enumerator
   */

  Set<JpsLibrary> getLibraries();

  /**
   * Runs {@code consumer.consume()} for each module processed by this enumerator
   */
  void processModules( Consumer<JpsModule> consumer);

  /**
   * Runs {@code consumer.consume()} for each library processed by this enumerator
   */
  void processLibraries( Consumer<JpsLibrary> consumer);

  /**
   * Runs {@code moduleConsumer.consume()} for each module and {@code libraryConsumer.consume()} for each library processed by this enumerator
   */
  void processModuleAndLibraries( Consumer<JpsModule> moduleConsumer,  Consumer<JpsLibrary> libraryConsumer);
}
