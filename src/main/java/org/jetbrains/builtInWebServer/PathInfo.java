package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;



public class PathInfo {
  private final VirtualFile child;
  private final VirtualFile root;
  private final boolean isLibrary;

  String moduleName;

  private String computedPath;

  public PathInfo( VirtualFile child,  VirtualFile root,  String moduleName, boolean isLibrary) {
    this.child = child;
    this.root = root;
    this.moduleName = moduleName;
    this.isLibrary = isLibrary;
  }

  public PathInfo( VirtualFile child,  VirtualFile root) {
    this(child, root, null, false);
  }


  public VirtualFile getChild() {
    return child;
  }


  public VirtualFile getRoot() {
    return root;
  }


  public String getModuleName() {
    return moduleName;
  }


  public String getPath() {
    if (computedPath == null) {
      StringBuilder builder = new StringBuilder();
      if (moduleName != null) {
        builder.append(moduleName).append('/');
      }

      if (isLibrary) {
        builder.append(root.getName()).append('/');
      }

      computedPath = builder.append(VfsUtilCore.getRelativePath(child, root, '/')).toString();
    }
    return computedPath;
  }
}