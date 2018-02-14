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

import com.gome.maven.openapi.util.Comparing;

import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.impl.runConfiguration.JpsRunConfigurationImpl;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryCollection;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.impl.JpsLibraryCollectionImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryRole;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleType;
import org.jetbrains.jps.model.module.JpsSdkReferencesTable;
import org.jetbrains.jps.model.module.JpsTypedModule;
import org.jetbrains.jps.model.module.impl.JpsModuleImpl;
import org.jetbrains.jps.model.module.impl.JpsModuleRole;
import org.jetbrains.jps.model.module.impl.JpsSdkReferencesTableImpl;
import org.jetbrains.jps.model.runConfiguration.JpsRunConfiguration;
import org.jetbrains.jps.model.runConfiguration.JpsRunConfigurationType;
import org.jetbrains.jps.model.runConfiguration.JpsTypedRunConfiguration;

import java.util.List;

/**
 * @author nik
 */
public class JpsProjectImpl extends JpsRootElementBase<JpsProjectImpl> implements JpsProject {
  private static final JpsElementCollectionRole<JpsElementReference<?>> EXTERNAL_REFERENCES_COLLECTION_ROLE =
    JpsElementCollectionRole.create(JpsElementChildRoleBase.<JpsElementReference<?>>create("external reference"));
  private static final JpsElementCollectionRole<JpsRunConfiguration> RUN_CONFIGURATIONS_ROLE = JpsElementCollectionRole.create(JpsElementChildRoleBase.<JpsRunConfiguration>create("run configuration"));
  private final JpsLibraryCollection myLibraryCollection;
  private String myName = "";

  public JpsProjectImpl( JpsModel model, JpsEventDispatcher eventDispatcher) {
    super(model, eventDispatcher);
    myContainer.setChild(JpsModuleRole.MODULE_COLLECTION_ROLE);
    myContainer.setChild(EXTERNAL_REFERENCES_COLLECTION_ROLE);
    myContainer.setChild(JpsSdkReferencesTableImpl.ROLE);
    myContainer.setChild(RUN_CONFIGURATIONS_ROLE);
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.setChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }

  public JpsProjectImpl(JpsProjectImpl original, JpsModel model, JpsEventDispatcher eventDispatcher) {
    super(original, model, eventDispatcher);
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.getChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }


  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setName( String name) {
    if (!Comparing.equal(myName, name)) {
      myName = name;
      fireElementChanged();
    }
  }

  public void addExternalReference( JpsElementReference<?> reference) {
    myContainer.getChild(EXTERNAL_REFERENCES_COLLECTION_ROLE).addChild(reference);
  }


  @Override
  public
  <P extends JpsElement, ModuleType extends JpsModuleType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsModule addModule( final String name,  ModuleType moduleType) {
    final JpsElementCollection<JpsModule> collection = myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE);
    return collection.addChild(new JpsModuleImpl<P>(moduleType, name, moduleType.createDefaultProperties()));
  }


  @Override
  public <P extends JpsElement, LibraryType extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addLibrary( String name,  LibraryType libraryType) {
    return myLibraryCollection.addLibrary(name, libraryType);
  }


  @Override
  public List<JpsModule> getModules() {
    return myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).getElements();
  }

  @Override

  public <P extends JpsElement> Iterable<JpsTypedModule<P>> getModules(JpsModuleType<P> type) {
    return myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).getElementsOfType(type);
  }
  
  @Override
  public void addModule( JpsModule module) {
    myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).addChild(module);
  }


  @Override
  public JpsLibraryCollection getLibraryCollection() {
    return myLibraryCollection;
  }

  @Override

  public JpsSdkReferencesTable getSdkReferencesTable() {
    return myContainer.getChild(JpsSdkReferencesTableImpl.ROLE);
  }


  @Override
  public <P extends JpsElement> Iterable<JpsTypedRunConfiguration<P>> getRunConfigurations(JpsRunConfigurationType<P> type) {
    return getRunConfigurationsCollection().getElementsOfType(type);
  }


  @Override
  public List<JpsRunConfiguration> getRunConfigurations() {
    return getRunConfigurationsCollection().getElements();
  }


  @Override
  public <P extends JpsElement> JpsTypedRunConfiguration<P> addRunConfiguration( String name,
                                                                                 JpsRunConfigurationType<P> type,
                                                                                 P properties) {
    return getRunConfigurationsCollection().addChild(new JpsRunConfigurationImpl<P>(name, type, properties));
  }

  private JpsElementCollection<JpsRunConfiguration> getRunConfigurationsCollection() {
    return myContainer.getChild(RUN_CONFIGURATIONS_ROLE);
  }


  @Override
  public JpsElementReference<JpsProject> createReference() {
    return new JpsProjectElementReference();
  }
}
