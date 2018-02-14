package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.PairFunction;



public abstract class PrefixlessWebServerRootsProvider extends WebServerRootsProvider {

  @Override
  public final PathInfo resolve( String path,  Project project) {
    return resolve(path, project, WebServerPathToFileManager.getInstance(project).getResolver(path));
  }


  public abstract PathInfo resolve( String path,  Project project,  PairFunction<String, VirtualFile, VirtualFile> resolver);
}