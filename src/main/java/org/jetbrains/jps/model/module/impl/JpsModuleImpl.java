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



import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.ex.JpsNamedCompositeElementBase;
import org.jetbrains.jps.model.impl.JpsUrlListRole;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryCollection;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.impl.JpsLibraryCollectionImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryRole;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;

import java.util.List;

/**
 * @author nik
 */
public class JpsModuleImpl<P extends JpsElement> extends JpsNamedCompositeElementBase<JpsModuleImpl<P>> implements JpsTypedModule<P> {
  private static final JpsUrlListRole CONTENT_ROOTS_ROLE = new JpsUrlListRole("content roots");
  private static final JpsUrlListRole EXCLUDED_ROOTS_ROLE = new JpsUrlListRole("excluded roots");
  private static final JpsElementChildRole<JpsDependenciesListImpl> DEPENDENCIES_LIST_CHILD_ROLE = JpsElementChildRoleBase.create("dependencies");
  private final JpsModuleType<P> myModuleType;
  private final JpsLibraryCollection myLibraryCollection;

  public JpsModuleImpl(JpsModuleType<P> type,  String name,  P properties) {
    super(name);
    myModuleType = type;
    myContainer.setChild(myModuleType.getPropertiesRole(), properties);
    myContainer.setChild(CONTENT_ROOTS_ROLE);
    myContainer.setChild(EXCLUDED_ROOTS_ROLE);
    myContainer.setChild(DEPENDENCIES_LIST_CHILD_ROLE, new JpsDependenciesListImpl());
    getDependenciesList().addModuleSourceDependency();
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.setChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
    myContainer.setChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE);
    myContainer.setChild(JpsSdkReferencesTableImpl.ROLE);
  }

  private JpsModuleImpl(JpsModuleImpl<P> original) {
    super(original);
    myModuleType = original.myModuleType;
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.getChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }


  @Override
  public JpsModuleImpl<P> createCopy() {
    return new JpsModuleImpl<P>(this);
  }

  @Override
  public JpsElementType<P> getType() {
    return myModuleType;
  }

  @Override

  public P getProperties() {
    return myContainer.getChild(myModuleType.getPropertiesRole());
  }

  @Override
  public <P extends JpsElement> JpsTypedModule<P> asTyped( JpsModuleType<P> type) {
    //noinspection unchecked
    return myModuleType.equals(type) ? (JpsTypedModule<P>)this : null;
  }


  @Override
  public JpsUrlList getContentRootsList() {
    return myContainer.getChild(CONTENT_ROOTS_ROLE);
  }


  public JpsUrlList getExcludeRootsList() {
    return myContainer.getChild(EXCLUDED_ROOTS_ROLE);
  }


  @Override
  public List<JpsModuleSourceRoot> getSourceRoots() {
    return myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE).getElements();
  }


  @Override
  public <P extends JpsElement> Iterable<JpsTypedModuleSourceRoot<P>> getSourceRoots( JpsModuleSourceRootType<P> type) {
    return myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE).getElementsOfType(type);
  }


  @Override
  public <P extends JpsElement> JpsModuleSourceRoot addSourceRoot( String url,  JpsModuleSourceRootType<P> rootType) {
    return addSourceRoot(url, rootType, rootType.createDefaultProperties());
  }


  @Override
  public <P extends JpsElement> JpsModuleSourceRoot addSourceRoot( String url,  JpsModuleSourceRootType<P> rootType,
                                                                   P properties) {
    final JpsModuleSourceRootImpl root = new JpsModuleSourceRootImpl<P>(url, rootType, properties);
    addSourceRoot(root);
    return root;
  }

  @Override
  public void addSourceRoot( JpsModuleSourceRoot root) {
    myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE).addChild(root);
  }

  @Override
  public void removeSourceRoot( String url,  JpsModuleSourceRootType rootType) {
    final JpsElementCollection<JpsModuleSourceRoot> roots = myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE);
    for (JpsModuleSourceRoot root : roots.getElements()) {
      if (root.getRootType().equals(rootType) && root.getUrl().equals(url)) {
        roots.removeChild(root);
        break;
      }
    }
  }


  @Override
  public JpsDependenciesList getDependenciesList() {
    return myContainer.getChild(DEPENDENCIES_LIST_CHILD_ROLE);
  }

  @Override

  public JpsSdkReferencesTable getSdkReferencesTable() {
    return myContainer.getChild(JpsSdkReferencesTableImpl.ROLE);
  }

  @Override
  public <P extends JpsElement> JpsSdkReference<P> getSdkReference( JpsSdkType<P> type) {
    JpsSdkReference<P> sdkReference = getSdkReferencesTable().getSdkReference(type);
    if (sdkReference != null) {
      return sdkReference;
    }
    JpsProject project = getProject();
    if (project != null) {
      return project.getSdkReferencesTable().getSdkReference(type);
    }
    return null;
  }

  @Override
  public <P extends JpsElement> JpsSdk<P> getSdk( JpsSdkType<P> type) {
    final JpsSdkReference<P> reference = getSdkReference(type);
    if (reference == null) return null;
    JpsTypedLibrary<JpsSdk<P>> library = reference.resolve();
    return library != null ? library.getProperties() : null;
  }

  @Override
  public void delete() {
    //noinspection unchecked
    ((JpsElementCollection<JpsModule>)myParent).removeChild(this);
  }


  @Override
  public JpsModuleReference createReference() {
    return new JpsModuleReferenceImpl(getName());
  }


  @Override
  public <P extends JpsElement, Type extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addModuleLibrary( String name,  Type type) {
    return myLibraryCollection.addLibrary(name, type);
  }

  @Override
  public void addModuleLibrary(final  JpsLibrary library) {
    myLibraryCollection.addLibrary(library);
  }


  @Override
  public JpsLibraryCollection getLibraryCollection() {
    return myLibraryCollection;
  }


  public JpsProject getProject() {
    JpsModel model = getModel();
    return model != null ? model.getProject() : null;
  }


  @Override
  public JpsModuleType<P> getModuleType() {
    return myModuleType;
  }
}
