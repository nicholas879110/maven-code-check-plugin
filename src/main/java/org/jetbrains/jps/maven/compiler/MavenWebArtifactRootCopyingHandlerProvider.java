/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.jps.maven.compiler;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.util.text.StringUtil;


import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.artifacts.instructions.ArtifactRootCopyingHandlerProvider;
import org.jetbrains.jps.incremental.artifacts.instructions.FileCopyingHandler;
import org.jetbrains.jps.maven.model.JpsMavenExtensionService;
import org.jetbrains.jps.maven.model.impl.*;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.elements.JpsModuleOutputPackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author nik
 */
public class MavenWebArtifactRootCopyingHandlerProvider extends ArtifactRootCopyingHandlerProvider {
  private static final Logger LOG = Logger.getInstance(MavenWebArtifactRootCopyingHandlerProvider.class);

  @Override
  public FileCopyingHandler createCustomHandler( JpsArtifact artifact,
                                                 File root,
                                                 JpsPackagingElement contextElement,
                                                 JpsModel model,
                                                 BuildDataPaths buildDataPaths) {
    if (contextElement instanceof JpsModuleOutputPackagingElement) return null;

    MavenProjectConfiguration projectConfiguration = JpsMavenExtensionService.getInstance().getMavenProjectConfiguration(buildDataPaths);
    if (projectConfiguration == null) return null;

    MavenWebArtifactConfiguration artifactResourceConfiguration = projectConfiguration.webArtifactConfigs.get(artifact.getName());
    if (artifactResourceConfiguration == null) return null;

    ResourceRootConfiguration rootConfiguration = artifactResourceConfiguration.getRootConfiguration(root);
    if (rootConfiguration == null) return null;

    MavenModuleResourceConfiguration moduleResourceConfiguration = projectConfiguration.moduleConfigurations.get(artifactResourceConfiguration.moduleName);
    if (moduleResourceConfiguration == null) {
      LOG.debug("Maven resource configuration not found for module " + artifactResourceConfiguration.moduleName);
      return null;
    }

    MavenResourceFileProcessor fileProcessor = new MavenResourceFileProcessor(projectConfiguration, model.getProject(), moduleResourceConfiguration);
    return new MavenWebRootCopyingHandler(fileProcessor, rootConfiguration, moduleResourceConfiguration, root);
  }

  private static class MavenWebRootCopyingHandler extends FileCopyingHandler {
    private final MavenResourceFileProcessor myFileProcessor;
     private final ResourceRootConfiguration myRootConfiguration;
     private final MavenModuleResourceConfiguration myModuleResourceConfiguration;
     private final File myRoot;
    private FileFilter myFileFilter;
    private boolean myMainWebAppRoot;

    private MavenWebRootCopyingHandler( MavenResourceFileProcessor fileProcessor,
                                        ResourceRootConfiguration rootConfiguration,
                                        MavenModuleResourceConfiguration moduleResourceConfiguration,
                                        File root) {
      myFileProcessor = fileProcessor;
      myRootConfiguration = rootConfiguration;
      myModuleResourceConfiguration = moduleResourceConfiguration;
      myRoot = root;
      myFileFilter = new MavenResourceFileFilter(myRoot, myRootConfiguration);

      //for additional resource directory 'exclude' means 'exclude from copying' but for the default webapp resource it mean 'exclude from filtering'
      String relativePath = FileUtil.getRelativePath(FileUtil.toSystemIndependentName(moduleResourceConfiguration.directory),
                                                     FileUtil.toSystemIndependentName(rootConfiguration.directory), '/');
      myMainWebAppRoot = relativePath != null && "src/main/webapp".equals(StringUtil.trimEnd(relativePath, "/"));
    }

    @Override
    public void copyFile( File from,  File to,  CompileContext context) throws IOException {
      myFileProcessor.copyFile(from, to, myRootConfiguration, context, myMainWebAppRoot ? myFileFilter : FileUtilRt.ALL_FILES);
    }

    @Override
    public void writeConfiguration( PrintWriter out) {
      out.print("maven hash:");
      out.println(myModuleResourceConfiguration.computeModuleConfigurationHash() + 31*myRootConfiguration.computeConfigurationHash());
    }


    @Override
    public FileFilter createFileFilter() {
      return myMainWebAppRoot ? FileUtilRt.ALL_FILES : myFileFilter;
    }
  }
}
