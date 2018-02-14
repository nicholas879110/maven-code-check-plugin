package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;



public abstract class WebServerRootsProvider {
  public static final ExtensionPointName<WebServerRootsProvider> EP_NAME = ExtensionPointName.create("org.jetbrains.webServerRootsProvider");


  public abstract PathInfo resolve( String path,  Project project);


  public abstract PathInfo getRoot( VirtualFile file,  Project project);

  public boolean isClearCacheOnFileContentChanged( VirtualFile file) {
    return false;
  }
}