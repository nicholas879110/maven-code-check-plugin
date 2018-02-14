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
package org.jetbrains.jps.model.module.impl;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.util.CollectConsumer;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Processor;
import gnu.trove.THashSet;


import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.module.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author nik
 */
public abstract class JpsDependenciesEnumeratorBase<Self extends JpsDependenciesEnumerator> implements JpsDependenciesEnumerator {
  private boolean myWithoutSdk;
  private boolean myWithoutLibraries;
  protected boolean myWithoutDepModules;
  private boolean myWithoutModuleSourceEntries;
  protected boolean myRecursively;
  protected final Collection<JpsModule> myRootModules;
  private Condition<JpsDependencyElement> myCondition;

  protected JpsDependenciesEnumeratorBase(Collection<JpsModule> rootModules) {
    myRootModules = rootModules;
  }


  @Override
  public Self withoutLibraries() {
    myWithoutLibraries = true;
    return self();
  }


  @Override
  public Self withoutDepModules() {
    myWithoutDepModules = true;
    return self();
  }


  @Override
  public Self withoutSdk() {
    myWithoutSdk = true;
    return self();
  }


  @Override
  public Self withoutModuleSourceEntries() {
    myWithoutModuleSourceEntries = true;
    return self();
  }


  @Override
  public Self satisfying( Condition<JpsDependencyElement> condition) {
    myCondition = condition;
    return self();
  }


  @Override
  public Self recursively() {
    myRecursively = true;
    return self();
  }

  protected abstract Self self();


  @Override
  public Set<JpsModule> getModules() {
    Set<JpsModule> result = new LinkedHashSet<JpsModule>();
    processModules(new CollectConsumer<JpsModule>(result));
    return result;
  }

  @Override
  public void processModules( final Consumer<JpsModule> consumer) {
    //noinspection unchecked
    processModuleAndLibraries(consumer, Consumer.EMPTY_CONSUMER);
  }

  protected boolean shouldProcessDependenciesRecursively() {
    return true;
  }

  public boolean processDependencies(Processor<JpsDependencyElement> processor) {
    THashSet<JpsModule> processed = new THashSet<JpsModule>();
    for (JpsModule module : myRootModules) {
      if (!doProcessDependencies(module, processor, processed)) {
        return false;
      }
    }
    return true;
  }

  private boolean doProcessDependencies(JpsModule module, Processor<JpsDependencyElement> processor, Set<JpsModule> processed) {
    if (!processed.add(module)) return true;

    for (JpsDependencyElement element : module.getDependenciesList().getDependencies()) {
      if (myCondition != null && !myCondition.value(element)) continue;

      if (myWithoutSdk && element instanceof JpsSdkDependency
       || myWithoutLibraries && element instanceof JpsLibraryDependency
       || myWithoutModuleSourceEntries && element instanceof JpsModuleSourceDependency) continue;

      if (myWithoutDepModules) {
        if (!myRecursively && element instanceof JpsModuleDependency) continue;
        if (element instanceof JpsModuleSourceDependency && !isEnumerationRootModule(module)) continue;
      }

      if (!shouldProcess(module, element)) {
        continue;
      }

      if (element instanceof JpsModuleDependency) {
        if (myRecursively && shouldProcessDependenciesRecursively()) {
          JpsModule depModule = ((JpsModuleDependency)element).getModule();
          if (depModule != null) {
            doProcessDependencies(depModule, processor, processed);
            continue;
          }
        }
        if (myWithoutDepModules) continue;
      }

      if (!processor.process(element)) {
        return false;
      }
    }

    return true;
  }

  protected boolean shouldProcess(JpsModule module, JpsDependencyElement element) {
    return true;
  }

  public boolean isEnumerationRootModule(JpsModule module) {
    return myRootModules.contains(module);
  }


  @Override
  public Set<JpsLibrary> getLibraries() {
    Set<JpsLibrary> libraries = new LinkedHashSet<JpsLibrary>();
    processLibraries(new CollectConsumer<JpsLibrary>(libraries));
    return libraries;
  }

  @Override
  public void processLibraries( final Consumer<JpsLibrary> consumer) {
    //noinspection unchecked
    processModuleAndLibraries(Consumer.EMPTY_CONSUMER, consumer);
  }

  @Override
  public void processModuleAndLibraries( final Consumer<JpsModule> moduleConsumer,  final Consumer<JpsLibrary> libraryConsumer) {
    processDependencies(new Processor<JpsDependencyElement>() {
      @Override
      public boolean process(JpsDependencyElement dependencyElement) {
        if (moduleConsumer != null) {
          if (myRecursively && dependencyElement instanceof JpsModuleSourceDependency) {
            moduleConsumer.consume(dependencyElement.getContainingModule());
          }
          else if ((!myRecursively || !shouldProcessDependenciesRecursively()) && dependencyElement instanceof JpsModuleDependency) {
            JpsModule module = ((JpsModuleDependency)dependencyElement).getModule();
            if (module != null) {
              moduleConsumer.consume(module);
            }
          }
        }
        if (libraryConsumer != null && dependencyElement instanceof JpsLibraryDependency) {
          JpsLibrary library = ((JpsLibraryDependency)dependencyElement).getLibrary();
          if (library != null) {
            libraryConsumer.consume(library);
          }
        }
        return true;
      }
    });
  }
}
