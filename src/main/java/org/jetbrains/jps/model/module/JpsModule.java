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



import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryCollection;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;

import java.util.List;

/**
 * @author nik
 */
public interface JpsModule extends JpsNamedElement, JpsReferenceableElement<JpsModule>, JpsCompositeElement {

  JpsUrlList getContentRootsList();


  JpsUrlList getExcludeRootsList();


  List<JpsModuleSourceRoot> getSourceRoots();


  <P extends JpsElement>
  Iterable<JpsTypedModuleSourceRoot<P>> getSourceRoots( JpsModuleSourceRootType<P> type);


  <P extends JpsElement>
  JpsModuleSourceRoot addSourceRoot( String url,  JpsModuleSourceRootType<P> rootType);


  <P extends JpsElement>
  JpsModuleSourceRoot addSourceRoot( String url,  JpsModuleSourceRootType<P> rootType,  P properties);

  void addSourceRoot( JpsModuleSourceRoot root);

  void removeSourceRoot( String url,  JpsModuleSourceRootType rootType);

  JpsDependenciesList getDependenciesList();


  JpsModuleReference createReference();


  <P extends JpsElement, Type extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addModuleLibrary( String name,  Type type);

  void addModuleLibrary( JpsLibrary library);


  JpsLibraryCollection getLibraryCollection();


  JpsSdkReferencesTable getSdkReferencesTable();


  <P extends JpsElement>
  JpsSdkReference<P> getSdkReference( JpsSdkType<P> type);


  <P extends JpsElement>
  JpsSdk<P> getSdk( JpsSdkType<P> type);

  void delete();

  JpsProject getProject();


  JpsModuleType<?> getModuleType();


  JpsElement getProperties();


  <P extends JpsElement> JpsTypedModule<P> asTyped( JpsModuleType<P> type);
}
