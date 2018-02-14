package org.jetbrains.builtInWebServer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gome.maven.ProjectTopics;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootAdapter;
import com.gome.maven.openapi.roots.ModuleRootEvent;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.util.PairFunction;



import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implement {@link WebServerRootsProvider} to add your provider
 */
public class WebServerPathToFileManager {
  private static final PairFunction<String, VirtualFile, VirtualFile> RELATIVE_PATH_RESOLVER = new PairFunction<String, VirtualFile, VirtualFile>() {

    @Override
    public VirtualFile fun(String path, VirtualFile parent) {
      return parent.findFileByRelativePath(path);
    }
  };

  private static final PairFunction<String, VirtualFile, VirtualFile> EMPTY_PATH_RESOLVER = new PairFunction<String, VirtualFile, VirtualFile>() {

    @Override
    public VirtualFile fun(String path, VirtualFile parent) {
      return BuiltInWebServer.findIndexFile(parent);
    }
  };

  private final Project project;

  final Cache<String, VirtualFile> pathToFileCache = CacheBuilder.newBuilder().maximumSize(512).expireAfterAccess(10, TimeUnit.MINUTES).build();
  // time to expire should be greater than pathToFileCache
  private final Cache<VirtualFile, PathInfo> fileToRoot = CacheBuilder.newBuilder().maximumSize(512).expireAfterAccess(11, TimeUnit.MINUTES).build();

  public static WebServerPathToFileManager getInstance( Project project) {
    return ServiceManager.getService(project, WebServerPathToFileManager.class);
  }

  public WebServerPathToFileManager( Application application,  Project project) {
    this.project = project;
    application.getMessageBus().connect(project).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
      @Override
      public void after( List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
          if (event instanceof VFileContentChangeEvent) {
            VirtualFile file = ((VFileContentChangeEvent)event).getFile();
            for (WebServerRootsProvider rootsProvider : WebServerRootsProvider.EP_NAME.getExtensions()) {
              if (rootsProvider.isClearCacheOnFileContentChanged(file)) {
                clearCache();
                break;
              }
            }
          }
          else {
            clearCache();
            break;
          }
        }
      }
    });
    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        clearCache();
      }
    });
  }

  private void clearCache() {
    pathToFileCache.invalidateAll();
    fileToRoot.invalidateAll();
  }


  public VirtualFile get( String path) {
    return get(path, true);
  }


  public VirtualFile get( String path, boolean cacheResult) {
    VirtualFile result = pathToFileCache.getIfPresent(path);
    if (result == null || !result.isValid()) {
      result = findByRelativePath(project, path);
      if (cacheResult && result != null && result.isValid()) {
        pathToFileCache.put(path, result);
      }
    }
    return result;
  }


  public String getPath( VirtualFile file) {
    PathInfo pathInfo = getRoot(file);
    return pathInfo == null ? null : pathInfo.getPath();
  }


  public PathInfo getRoot( VirtualFile child) {
    PathInfo result = fileToRoot.getIfPresent(child);
    if (result == null) {
      for (WebServerRootsProvider rootsProvider : WebServerRootsProvider.EP_NAME.getExtensions()) {
        result = rootsProvider.getRoot(child, project);
        if (result != null) {
          fileToRoot.put(child, result);
          break;
        }
      }
    }
    return result;
  }


  VirtualFile findByRelativePath( Project project,  String path) {
    for (WebServerRootsProvider rootsProvider : WebServerRootsProvider.EP_NAME.getExtensions()) {
      PathInfo result = rootsProvider.resolve(path, project);
      if (result != null) {
        fileToRoot.put(result.getChild(), result);
        return result.getChild();
      }
    }
    return null;
  }


  public PairFunction<String, VirtualFile, VirtualFile> getResolver( String path) {
    return path.isEmpty() ? EMPTY_PATH_RESOLVER : RELATIVE_PATH_RESOLVER;
  }
}