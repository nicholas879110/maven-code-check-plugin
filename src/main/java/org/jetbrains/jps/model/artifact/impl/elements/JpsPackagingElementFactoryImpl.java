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
package org.jetbrains.jps.model.artifact.impl.elements;

import com.gome.maven.openapi.util.text.StringUtil;



import org.jetbrains.jps.model.artifact.JpsArtifactReference;
import org.jetbrains.jps.model.artifact.elements.*;
import org.jetbrains.jps.model.library.JpsLibraryReference;

/**
 * @author nik
 */
public class JpsPackagingElementFactoryImpl extends JpsPackagingElementFactory {

  @Override

  public JpsDirectoryCopyPackagingElement createDirectoryCopy( String directoryPath) {
    return new JpsDirectoryCopyPackagingElementImpl(directoryPath);
  }

  @Override
  public JpsPackagingElement createParentDirectories(String relativeOutputPath, JpsPackagingElement element) {
    relativeOutputPath = StringUtil.trimStart(relativeOutputPath, "/");
    if (relativeOutputPath.length() == 0) {
      return element;
    }
    int slash = relativeOutputPath.indexOf('/');
    if (slash == -1) slash = relativeOutputPath.length();
    String rootName = relativeOutputPath.substring(0, slash);
    String pathTail = relativeOutputPath.substring(slash);
    final JpsDirectoryPackagingElement root = createDirectory(rootName);
    final JpsCompositePackagingElement last = getOrCreateDirectoryOrArchive(root, pathTail, true);
    last.addChild(element);
    return root;
  }

  @Override
  public JpsCompositePackagingElement getOrCreateDirectory( JpsCompositePackagingElement root,  String path) {
    return getOrCreateDirectoryOrArchive(root, path, true);
  }

  @Override
  public JpsCompositePackagingElement getOrCreateArchive( JpsCompositePackagingElement root,  String path) {
    return getOrCreateDirectoryOrArchive(root, path, false);
  }


  private JpsCompositePackagingElement getOrCreateDirectoryOrArchive( JpsCompositePackagingElement root,
                                                                       String path, final boolean directory) {
    path = StringUtil.trimStart(StringUtil.trimEnd(path, "/"), "/");
    if (path.length() == 0) {
      return root;
    }
    int index = path.lastIndexOf('/');
    String lastName = path.substring(index + 1);
    String parentPath = index != -1 ? path.substring(0, index) : "";

    final JpsCompositePackagingElement parent = getOrCreateDirectoryOrArchive(root, parentPath, true);
    final JpsCompositePackagingElement last = directory ? createDirectory(lastName) : createArchive(lastName);
    return parent.addChild(last);
  }

  @Override

  public JpsFileCopyPackagingElement createFileCopy( String filePath,  String outputFileName) {
    return new JpsFileCopyPackagingElementImpl(filePath, outputFileName);
  }

  @Override

  public JpsExtractedDirectoryPackagingElement createExtractedDirectory( String jarPath,  String pathInJar) {
    return new JpsExtractedDirectoryPackagingElementImpl(jarPath, pathInJar);
  }

  @Override

  public JpsDirectoryPackagingElement createDirectory( String directoryName) {
    return new JpsDirectoryPackagingElementImpl(directoryName);
  }

  @Override

  public JpsArchivePackagingElement createArchive( String archiveName) {
    return new JpsArchivePackagingElementImpl(archiveName);
  }

  @Override

  public JpsArtifactRootElement createArtifactRoot() {
    return new JpsArtifactRootElementImpl();
  }

  @Override

  public JpsLibraryFilesPackagingElement createLibraryElement( JpsLibraryReference reference) {
    return new JpsLibraryFilesPackagingElementImpl(reference);
  }

  @Override

  public JpsArtifactOutputPackagingElement createArtifactOutput( JpsArtifactReference reference) {
    return new JpsArtifactOutputPackagingElementImpl(reference);
  }
}
