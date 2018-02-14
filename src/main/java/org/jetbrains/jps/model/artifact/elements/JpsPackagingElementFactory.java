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
package org.jetbrains.jps.model.artifact.elements;



import org.jetbrains.jps.model.artifact.JpsArtifactReference;
import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author nik
 */
public abstract class JpsPackagingElementFactory {
  public abstract JpsCompositePackagingElement getOrCreateDirectory( JpsCompositePackagingElement root,  String path);

  public abstract JpsCompositePackagingElement getOrCreateArchive( JpsCompositePackagingElement root,  String path);

  public static JpsPackagingElementFactory getInstance() {
    return JpsServiceManager.getInstance().getService(JpsPackagingElementFactory.class);
  }


  public abstract JpsDirectoryCopyPackagingElement createDirectoryCopy( String directoryPath);

  public abstract JpsPackagingElement createParentDirectories(String path, JpsPackagingElement element);


  public abstract JpsFileCopyPackagingElement createFileCopy( String filePath,  String outputFileName);


  public abstract JpsExtractedDirectoryPackagingElement createExtractedDirectory( String jarPath,  String pathInJar);


  public abstract JpsDirectoryPackagingElement createDirectory( String directoryName);


  public abstract JpsArchivePackagingElement createArchive( String archiveName);


  public abstract JpsArtifactRootElement createArtifactRoot();


  public abstract JpsLibraryFilesPackagingElement createLibraryElement( JpsLibraryReference reference);


  public abstract JpsArtifactOutputPackagingElement createArtifactOutput( JpsArtifactReference reference);

}
