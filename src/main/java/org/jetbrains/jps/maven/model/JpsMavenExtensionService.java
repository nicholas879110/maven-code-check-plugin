package org.jetbrains.jps.maven.model;



import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.maven.model.impl.MavenProjectConfiguration;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author nik
 */
public abstract class JpsMavenExtensionService {
  public static JpsMavenExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsMavenExtensionService.class);
  }


  public abstract JpsMavenModuleExtension getExtension( JpsModule module);


  public abstract JpsMavenModuleExtension getOrCreateExtension( JpsModule module);

  public abstract void setProductionOnTestDependency( JpsDependencyElement dependency, boolean value);

  public abstract boolean isProductionOnTestDependency( JpsDependencyElement dependency);

  public abstract boolean hasMavenProjectConfiguration( BuildDataPaths paths);


  public abstract MavenProjectConfiguration getMavenProjectConfiguration(BuildDataPaths paths);
}
