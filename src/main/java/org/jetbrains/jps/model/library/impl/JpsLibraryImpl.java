/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.jps.model.library.impl;

import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.util.containers.ContainerUtil;


import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.ex.JpsNamedCompositeElementBase;
import org.jetbrains.jps.model.impl.JpsElementCollectionImpl;
import org.jetbrains.jps.model.library.*;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class JpsLibraryImpl<P extends JpsElement> extends JpsNamedCompositeElementBase<JpsLibraryImpl<P>> implements JpsTypedLibrary<P> {
  private final JpsLibraryType<P> myLibraryType;

  public JpsLibraryImpl( String name,  JpsLibraryType<P> type,  P properties) {
    super(name);
    myLibraryType = type;
    myContainer.setChild(myLibraryType.getPropertiesRole(), properties);
  }

  private JpsLibraryImpl( JpsLibraryImpl<P> original) {
    super(original);
    myLibraryType = original.myLibraryType;
  }

  @Override

  public JpsLibraryType<P> getType() {
    return myLibraryType;
  }


  @Override
  public <P extends JpsElement> JpsTypedLibrary<P> asTyped( JpsLibraryType<P> type) {
    //noinspection unchecked
    return myLibraryType.equals(type) ? (JpsTypedLibrary<P>)this : null;
  }


  @Override
  public P getProperties() {
    return myContainer.getChild(myLibraryType.getPropertiesRole());
  }


  @Override
  public List<JpsLibraryRoot> getRoots( JpsOrderRootType rootType) {
    final JpsElementCollection<JpsLibraryRoot> rootsCollection = myContainer.getChild(getRole(rootType));
    return rootsCollection != null ? rootsCollection.getElements() : Collections.<JpsLibraryRoot>emptyList();
  }

  @Override
  public void addRoot( String url,  JpsOrderRootType rootType) {
    addRoot(url, rootType, JpsLibraryRoot.InclusionOptions.ROOT_ITSELF);
  }

  @Override
  public void addRoot( File file,  JpsOrderRootType rootType) {
    addRoot(JpsPathUtil.getLibraryRootUrl(file), rootType);
  }

  @Override
  public void addRoot( final String url,  final JpsOrderRootType rootType,
                       JpsLibraryRoot.InclusionOptions options) {
    myContainer.getOrSetChild(getRole(rootType)).addChild(new JpsLibraryRootImpl(url, rootType, options));
  }

  @Override
  public void removeUrl( final String url,  final JpsOrderRootType rootType) {
    final JpsElementCollection<JpsLibraryRoot> rootsCollection = myContainer.getChild(getRole(rootType));
    if (rootsCollection != null) {
      for (JpsLibraryRoot root : rootsCollection.getElements()) {
        if (root.getUrl().equals(url) && root.getRootType().equals(rootType)) {
          rootsCollection.removeChild(root);
          break;
        }
      }
    }
  }

  private static JpsElementCollectionRole<JpsLibraryRoot> getRole(JpsOrderRootType type) {
    return JpsElementCollectionRole.create(new JpsLibraryRootRole(type));
  }

  @Override
  public void delete() {
    getParent().removeChild(this);
  }

  public JpsElementCollectionImpl<JpsLibrary> getParent() {
    //noinspection unchecked
    return (JpsElementCollectionImpl<JpsLibrary>)myParent;
  }


  @Override
  public JpsLibraryImpl<P> createCopy() {
    return new JpsLibraryImpl<P>(this);
  }


  @Override
  public JpsLibraryReference createReference() {
    return new JpsLibraryReferenceImpl(getName(), createParentReference());
  }

  private JpsElementReference<JpsCompositeElement> createParentReference() {
    //noinspection unchecked
    return ((JpsReferenceableElement<JpsCompositeElement>)getParent().getParent()).createReference();
  }

  @Override
  public List<File> getFiles(final JpsOrderRootType rootType) {
    List<String> urls = getRootUrls(rootType);
    List<File> files = new ArrayList<File>(urls.size());
    for (String url : urls) {
      if (!url.startsWith("jrt://")) {
        files.add(JpsPathUtil.urlToFile(url));
      }
    }
    return files;
  }

  @Override
  public List<String> getRootUrls(JpsOrderRootType rootType) {
    List<String> urls = new ArrayList<String>();
    for (JpsLibraryRoot root : getRoots(rootType)) {
      switch (root.getInclusionOptions()) {
        case ROOT_ITSELF:
          urls.add(root.getUrl());
          break;
        case ARCHIVES_UNDER_ROOT:
          collectArchives(JpsPathUtil.urlToFile(root.getUrl()), false, urls);
          break;
        case ARCHIVES_UNDER_ROOT_RECURSIVELY:
          collectArchives(JpsPathUtil.urlToFile(root.getUrl()), true, urls);
          break;
      }
    }
    return urls;
  }

  private static final Set<String> AR_EXTENSIONS  = ContainerUtil.newTroveSet(FileUtil.PATH_HASHING_STRATEGY, "jar", "zip", "swc", "ane");

  private static void collectArchives(File file, boolean recursively, List<String> result) {
    final File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) {
        final String extension = FileUtilRt.getExtension(child.getName());
        if (child.isDirectory()) {
          if (recursively) {
            collectArchives(child, recursively, result);
          }
        }
        // todo [nik] get list of extensions mapped to Archive file type from IDE settings
        else if (AR_EXTENSIONS.contains(extension)) {
          result.add(JpsPathUtil.getLibraryRootUrl(child));
        }
      }
    }
  }
}
