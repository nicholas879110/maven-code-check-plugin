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
package org.jetbrains.jps.model.impl;



import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.impl.JpsLibraryImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryReferenceImpl;
import org.jetbrains.jps.model.library.impl.JpsSdkReferenceImpl;
import org.jetbrains.jps.model.library.impl.sdk.JpsSdkImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;
import org.jetbrains.jps.model.module.impl.JpsModuleImpl;
import org.jetbrains.jps.model.module.impl.JpsModuleReferenceImpl;
import org.jetbrains.jps.model.module.impl.JpsModuleSourceRootImpl;

/**
 * @author nik
 */
public class JpsElementFactoryImpl extends JpsElementFactory {
  @Override
  public JpsModel createModel() {
    return new JpsModelImpl(new JpsEventDispatcherBase() {
      @Override
      public void fireElementRenamed( JpsNamedElement element,  String oldName,  String newName) {
      }

      @Override
      public void fireElementChanged( JpsElement element) {
      }
    });
  }

  @Override
  public <P extends JpsElement> JpsModule createModule( String name,  JpsModuleType<P> type,  P properties) {
    return new JpsModuleImpl<P>(type, name, properties);
  }


  @Override
  public <P extends JpsElement> JpsTypedLibrary<P> createLibrary( String name,
                                                                    JpsLibraryType<P> type,
                                                                    P properties) {
    return new JpsLibraryImpl<P>(name, type, properties);
  }

  @Override
  public <P extends JpsElement> JpsTypedLibrary<JpsSdk<P>> createSdk( String name,  String homePath,
                                                                      String versionString,  JpsSdkType<P> type,
                                                                      P properties) {
    return createLibrary(name, type, new JpsSdkImpl<P>(homePath, versionString, type, properties));
  }


  @Override
  public <P extends JpsElement> JpsModuleSourceRoot createModuleSourceRoot( String url,
                                                                            JpsModuleSourceRootType<P> type,
                                                                            P properties) {
    return new JpsModuleSourceRootImpl<P>(url, type, properties);
  }


  @Override
  public JpsModuleReference createModuleReference( String moduleName) {
    return new JpsModuleReferenceImpl(moduleName);
  }


  @Override
  public JpsLibraryReference createLibraryReference( String libraryName,
                                                     JpsElementReference<? extends JpsCompositeElement> parentReference) {
    return new JpsLibraryReferenceImpl(libraryName, parentReference);
  }


  @Override
  public <P extends JpsElement> JpsSdkReference<P> createSdkReference( String sdkName,  JpsSdkType<P> sdkType) {
    return new JpsSdkReferenceImpl<P>(sdkName, sdkType, createGlobalReference());
  }


  @Override
  public JpsElementReference<JpsProject> createProjectReference() {
    return new JpsProjectElementReference();
  }


  @Override
  public JpsElementReference<JpsGlobal> createGlobalReference() {
    return new JpsGlobalElementReference();
  }


  @Override
  public JpsDummyElement createDummyElement() {
    return new JpsDummyElementImpl();
  }


  @Override
  public <D> JpsSimpleElement<D> createSimpleElement( D data) {
    return new JpsSimpleElementImpl<D>(data);
  }
}
