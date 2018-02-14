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
package org.jetbrains.jps.model;



import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author nik
 */
public abstract class JpsElementFactory {
  public static JpsElementFactory getInstance() {
    return JpsServiceManager.getInstance().getService(JpsElementFactory.class);
  }

  public abstract JpsModel createModel();

  public abstract <P extends JpsElement> JpsModule createModule( String name,  JpsModuleType<P> type,  P properties);

  public abstract <P extends JpsElement> JpsTypedLibrary<P> createLibrary( String name,  JpsLibraryType<P> type,  P properties);

  public abstract <P extends JpsElement> JpsTypedLibrary<JpsSdk<P>> createSdk( String name,  String homePath,  String versionString,
                                                                               JpsSdkType<P> type,  P properties);


  public abstract <P extends JpsElement> JpsModuleSourceRoot createModuleSourceRoot( String url,  JpsModuleSourceRootType<P> type,  P properties);


  public abstract JpsModuleReference createModuleReference( String moduleName);


  public abstract JpsLibraryReference createLibraryReference( String libraryName,
                                                              JpsElementReference<? extends JpsCompositeElement> parentReference);


  public abstract <P extends JpsElement> JpsSdkReference<P> createSdkReference( String sdkName,
                                                                                                 JpsSdkType<P> sdkType);


  public abstract JpsElementReference<JpsProject> createProjectReference();


  public abstract JpsElementReference<JpsGlobal> createGlobalReference();


  public abstract JpsDummyElement createDummyElement();


  public abstract <D> JpsSimpleElement<D> createSimpleElement( D data);
}
