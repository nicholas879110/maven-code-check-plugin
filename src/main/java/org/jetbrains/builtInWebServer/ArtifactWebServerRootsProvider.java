package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.packaging.artifacts.Artifact;
import com.gome.maven.packaging.artifacts.ArtifactManager;
import com.gome.maven.util.PairFunction;



final class ArtifactWebServerRootsProvider extends PrefixlessWebServerRootsProvider {

  @Override
  public PathInfo resolve( String path,  Project project,  PairFunction<String, VirtualFile, VirtualFile> resolver) {
    for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
      VirtualFile root = artifact.getOutputFile();
      if (root != null) {
        VirtualFile file = root.findFileByRelativePath(path);
        if (file != null) {
          return new PathInfo(file, root);
        }
      }
    }
    return null;
  }


  @Override
  public PathInfo getRoot( VirtualFile file,  Project project) {
    for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
      VirtualFile root = artifact.getOutputFile();
      if (root != null && VfsUtilCore.isAncestor(root, file, true)) {
        return new PathInfo(file, root);
      }
    }
    return null;
  }
}